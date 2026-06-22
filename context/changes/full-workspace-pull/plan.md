# Pełny Pull Workspace do Lokalnej Kopii (S-01) — Implementation Plan

## Overview

S-01 dostarcza pierwszy realny pull danych ClickUp do lokalnej kopii: asynchronicznie
pobiera całe workspace (spaces → folders → lists → tasks) i zapisuje do PostgreSQL
operacją replace-all. Serwis zaprojektowany z parametrem `since: Instant?` wykłada
kontrakt pod S-02 (sync przyrostowy) — null = pełny pull/replace-all, non-null = upsert
przyrostowy bez delete.

## Current State Analysis

- **F-01 (done)**: `ClickupClient` (RestClient + backoff-ready), `ConnectivityService`,
  `SettingsStore` (token na dysku), `ClickupProperties` (`api.base-url`, `settings-file`).
- **F-02 (done)**: Schemat V1 (`space/folder/list/task`), repozytoria z `upsert ON CONFLICT`,
  `PostgresTestcontainersConfig` jako test harness.
- **Brak**: klienta do pobierania danych workspace (tylko `GET /user` istnieje), warstwy
  serwisowej sync, tabeli stanu sync, obsługi subtasków (brak `parent_id` w schemacie).
- **FK bez CASCADE w V1**: replace-all wymaga albo manualnej kolejności delete leaf→root,
  albo CASCADE. S-01 dodaje CASCADE przez V2 migrację — upraszcza logikę serwisu.

## Desired End State

Po zakończeniu planu:
- `POST /api/sync/full-pull` → 202 natychmiast; pull idzie asynchronicznie w tle.
- `GET /api/sync/status` → JSON ze stanem job (`IDLE/RUNNING/COMPLETED/FAILED`,
  `startedAt`, `completedAt`) + `lastSyncedAt` per zestaw sync z DB.
- Lokalny Postgres zawiera wszystkie spaces/folders/lists/tasks (w tym subtaski z
  `parent_id`) z pierwszego team'u ClickUp, wierne stanowi API na moment pull'u.
- Ponowny pull = replace-all: elementy usunięte z ClickUp znikają z lokalnej kopii.
- Zakończenie obu zestawów sync zapisuje `last_synced_at` do tabeli `sync_set` (kontrakt
  pod S-02 i S-07).

Weryfikacja: `mvn test` (toolchain + Docker) zielony; ręczne `POST /api/sync/full-pull`
przeciw realnemu ClickUp → `GET /api/sync/status` zwraca `COMPLETED`; tabele w lokalnym
Postgresie wypełnione.

### Key Discoveries

- Rate limit ClickUp: 100 req/min; backoff exponential (1 s → 2 s → 4 s, max 3 próby) na
  HTTP 429 w każdym wywołaniu klienta.
- Paginacja tasków: 100/stronę, flaga `last_page` boolean; `include_closed=true` pobiera
  wszystkie statusy; `subtasks=true` inline'uje subtaski w `task.subtasks[]`.
- `task.milestone` boolean w API → `is_milestone` w DB; `task.parent` (nullable) → `parent_id`
  (V2); `milestone_id` w DB = lokalny koncept, zawsze `null` po pull'u (S-03/S-04 go ustawiają).
- Folderless lists: `folder.hidden = true` w API → `folder_id = null` w DB.
- `@Async` wymaga wywołania z zewnątrz beana (AOP proxy); wywołanie `self.method()` ignoruje
  interceptor.
- V2 dodaje `ON DELETE CASCADE` do `task.list_id` i `folder.space_id` itp. — syncDictionaries
  może usunąć stale space/folder/list, a CASCADE czyści zależne encje.
- Jackson 3 (`tools.jackson.*`), test-slice'y Boot 4 w modularnych pakietach, Testcontainers
  2.x (prefix `testcontainers-`) — per `context/foundation/lessons.md`.

## What We're NOT Doing

- Sync przyrostowy (`date_updated_gt`) — S-02 (metoda serwisu z `since != null` jest
  zaprojektowana, ale nie jest wyzwalana żadnym endpointem w S-01).
- Pull więcej niż pierwszego team'u z `GET /team`.
- Panel statusu sync z historią błędów i konfiguracją częstotliwości — S-07.
- Głębokie zagnieżdżenie subtasków (> 1 poziom) — S-01 przechowuje tylko bezpośrednie
  subtaski (parent = task w tej samej liście); głębsze poziomy out of scope MVP.
- Zarządzanie strukturą słowników (tworzenie/edycja spaces/folders/lists) — Non-Goal PRD.
- OAuth — Non-Goal PRD.

## Implementation Approach

Cztery fazy, każda samodzielnie testowalna i kończąca się punktem weryfikacji:

1. **Schema V2 + domain update** — Flyway V2 (`parent_id`, CASCADE, `sync_set`), aktualizacja
   rekordu `Task`, nowa encja `SyncSet`.
2. **ClickUp workspace API client** — `ClickupWorkspaceClient` z 6 metodami (team/space/
   folder/list/task + paginacja + retry backoff) i DTOs; testy z `MockRestServiceServer`.
3. **WorkspaceSyncService** — orchestracja `@Async`, dwie `@Transactional` operacje per
   zestaw sync, replace-all z FK-safe delete, parametr `since`.
4. **REST surface** — `SyncController` (POST trigger + GET status) + integracyjny smoke-test.

## Critical Implementation Details

- **CASCADE w V2 (Phase 1)**: Replace-all działa w dwóch osobnych transakcjach
  (`syncDictionaries` → `syncTasks`). Gdy lista znika z ClickUp, `syncDictionaries` usuwa
  ją — ale bez CASCADE FK violation (`task.list_id NOT NULL` jeszcze wskazuje na tę listę).
  Rozwiązanie: V2 dodaje `ON DELETE CASCADE` na `task.list_id`, `list.folder_id`,
  `folder.space_id` i `list.space_id`. Delete space/folder/list kaskadowo usuwa dzieci.
  `task.milestone_id` i `task.parent_id` dostają `ON DELETE SET NULL` (self-ref i subtask-ref).

- **Kolejność zestawów sync (Phase 3)**: `syncDictionaries` PRZED `syncTasks`. Taski zależą
  od list przez FK; lista musi być w DB zanim upsertujemy do niej taski. Przy replace-all
  `syncDictionaries` usuwa stale listy (→ CASCADE czyści ich taski), a `syncTasks` następnie
  obsługuje task-level staleness w obrębie ocalałych list.

- **Subtaski — `list_id` dziedziczone (Phase 2/3)**: API zwraca subtaski w `task.subtasks[]`
  przy `subtasks=true`. Subtask ma `parent` (id rodzica), ale nie ma bezpośredniego `list_id`.
  Implementacja: dziedzicz `list_id` z rodzica przy mapowaniu. Upsert w dwóch przebiegach:
  najpierw top-level taski (parent=null), potem subtaski (żeby FK `parent_id → task.id` nie
  failowało na INSERT).

- **`@Async` i AOP proxy (Phase 3)**: `triggerPull()` musi być wołane z zewnątrz beana
  (`SyncController → WorkspaceSyncService`), nie self-call wewnątrz serwisu. Self-call omija
  Spring proxy i `@Async` jest ignorowane.

- **Toolchain build**: `JAVA_HOME="$JAVA_HOME21" "$MAVEN_HOME9/bin/mvn" -B test` w `server/`.
  Docker wymagany dla Testcontainers.

---

## Phase 1: Schema V2 + Domain Update

### Overview

Flyway V2 dodaje `parent_id` do `task`, `ON DELETE CASCADE`/`SET NULL` do kluczowych FK
oraz nową tabelę `sync_set` z dwoma preseeded rows. Rekord `Task` i nowa encja `SyncSet`
aktualizowane odpowiednio. Faza czysto infra — zero logiki serwisowej.

### Changes Required

#### 1. Migracja Flyway V2

**File**: `server/src/main/resources/db/migration/V2__subtasks_and_sync_set.sql`

**Intent**: Rozszerzyć schemat o subtaski i stan zestawów sync, jednocześnie wzmacniając
integralność FK przez CASCADE/SET NULL — usuwa potrzebę ręcznej kolejności delete w serwisie.

**Contract**:
- `ALTER TABLE task ADD COLUMN parent_id text NULL` + `REFERENCES task(id) ON DELETE SET NULL`
  + `CREATE INDEX idx_task_parent_id ON task(parent_id)`
- Przebudowa FK `task.list_id`: DROP starą (bez CASCADE), ADD `REFERENCES list(id) ON DELETE CASCADE`
- Przebudowa FK `task.milestone_id`: DROP, ADD `REFERENCES task(id) ON DELETE SET NULL`
- Przebudowa FK `list.folder_id`: DROP, ADD `REFERENCES folder(id) ON DELETE CASCADE`
- Przebudowa FK `list.space_id`: DROP, ADD `REFERENCES space(id) ON DELETE CASCADE`
- Przebudowa FK `folder.space_id`: DROP, ADD `REFERENCES space(id) ON DELETE CASCADE`
- `CREATE TABLE sync_set (name text PRIMARY KEY, last_synced_at timestamptz NULL)`
- `INSERT INTO sync_set (name) VALUES ('dictionaries'), ('tasks')`

#### 2. Task record — dodanie parentId

**File**: `server/src/main/java/com/example/clickupsimplifier/persistence/Task.java`

**Intent**: Odzwierciedlić nową kolumnę `parent_id` w rekordzie domenowym.

**Contract**: Dodaj pole `@Nullable String parentId` do rekordu. Spring Data JDBC mapuje
`parentId` → `parent_id` przez konwencję camelCase→snake_case; brak dodatkowych adnotacji.

#### 3. TaskRepository — rozszerzenie upsertu i delete-not-in

**File**: `server/src/main/java/com/example/clickupsimplifier/persistence/TaskRepository.java`

**Intent**: Uwzględnić `parent_id` w upsertcie oraz dostarczyć metodę czyszczenia stale
tasków per lista (potrzebną w replace-all).

**Contract**:
- Zaktualizować `upsert(...)`: dodać `parent_id = :parentId` do klauzuli `DO UPDATE SET`.
- Dodać `@Modifying @Query("DELETE FROM task WHERE list_id = :listId AND id NOT IN (:ids)") void deleteStaleByListId(@Param("listId") String listId, @Param("ids") Collection<String> ids)`.

#### 4. SyncSet entity + repository

**File**: `server/src/main/java/com/example/clickupsimplifier/persistence/SyncSet.java`
**File**: `server/src/main/java/com/example/clickupsimplifier/persistence/SyncSetRepository.java`

**Intent**: Persystować timestamp ostatniego udanego sync per zestaw; S-07 rozszerzy tę
tabelę o `last_error`, `frequency` itp.

**Contract**:
- Rekord `SyncSet(@Id String name, @Nullable Instant lastSyncedAt)` z `@Table("sync_set")`.
- `SyncSetRepository extends CrudRepository<SyncSet, String>` + metoda
  `@Modifying @Query("UPDATE sync_set SET last_synced_at = :ts WHERE name = :name") void updateLastSyncedAt(@Param("name") String name, @Param("ts") Instant ts)`.

#### 5. Repozytoria słownikowe — delete-not-in

**File**: `server/src/main/java/com/example/clickupsimplifier/persistence/SpaceRepository.java`
**File**: `server/src/main/java/com/example/clickupsimplifier/persistence/FolderRepository.java`
**File**: `server/src/main/java/com/example/clickupsimplifier/persistence/WorkspaceListRepository.java`

**Intent**: Dostarczyć metodę czyszczenia stale encji słownikowych na potrzeby replace-all
(CASCADE obsługuje zależnych potomków).

**Contract**: Każde repo: `@Modifying @Query("DELETE FROM <table> WHERE id NOT IN (:ids)") void deleteByIdNotIn(@Param("ids") Collection<String> ids)`.

### Success Criteria

#### Automated Verification

- `mvn test` (toolchain + Docker) przechodzi; V2 aplikuje się czysto na Testcontainers
  Postgres bez naruszenia istniejących danych V1
- Test schematu (rozszerzenie `SchemaMigrationTest`): kolumna `parent_id` na `task`,
  tabela `sync_set` z rows `dictionaries` i `tasks`, CASCADE FK zweryfikowany (DELETE na
  `list` → taski usunięte kaskadowo)
- Test integracyjny `SyncSet`: zapis, `updateLastSyncedAt`, odczyt `lastSyncedAt`
- Istniejące testy F-02 (`RepositoryIntegrationTest`) nadal przechodzą

#### Manual Verification

- Po starcie aplikacji przeciw lokalnemu Postgresowi: schemat V2 zaaplikowany, `sync_set`
  zawiera 2 rows, `\d task` w psql pokazuje kolumnę `parent_id`

**Implementation Note**: Po automatycznej weryfikacji zatrzymaj się przed Phase 2.

---

## Phase 2: ClickUp Workspace API Client

### Overview

Nowy klient HTTP do pobierania danych workspace: spaces, folders, lists, tasks (z paginacją,
filtrem `since` i retry backoff na 429). Wzorzec zgodny z istniejącym `ClickupClient` z F-01.

### Changes Required

#### 1. DTOs workspace ClickUp

**File**: `server/src/main/java/com/example/clickupsimplifier/clickup/workspace/` (nowy pakiet)

**Intent**: Typy mapujące JSON z ClickUp API na rekordy Java. Każde DTO odwzorowuje minimum
pól potrzebnych do wypełnienia encji domenowej.

**Contract** (rekordy Java, adnotacje z `com.fasterxml.jackson.annotation.*`):
- `ClickupTeam(String id, String name)` ← `teams[].id/name`
- `ClickupSpace(String id, String name)` ← `spaces[].id/name`
- `ClickupFolder(String id, String name, String spaceId)` ← `folders[].id/name` + `folders[].space.id` (nested; pole `space` to pomocniczy rekord `ClickupRef(String id)` reużywany w kilku DTO)
- `ClickupList(String id, String name, String spaceId, @Nullable String folderId)` ← `lists[].id/name/space.id`; `folderId = folder.hidden ? null : folder.id`
- `ClickupTask(String id, String name, @Nullable String status, @Nullable String description, boolean milestone, @Nullable String parent)` ← `tasks[].id/name/description/milestone/parent`; status z nested: `tasks[].status.status`
- Wrapper-response rekordy (do deserializacji wrappera): `TeamsResponse(List<ClickupTeam> teams)`, `SpacesResponse`, `FoldersResponse`, `ListsResponse`, `TasksPage(List<ClickupTask> tasks, boolean lastPage)` (pole `last_page` z API → `lastPage` przez `@JsonProperty`)

#### 2. ClickupWorkspaceClient

**File**: `server/src/main/java/com/example/clickupsimplifier/clickup/workspace/ClickupWorkspaceClient.java`

**Intent**: Dostarczyć wszystkie wywołania HTTP potrzebne do pełnego pull workspace, z
wbudowanym retry na 429 i paginacją tasków.

**Contract**: `@Component` wstrzykujący istniejący bean `RestClient` (z `ClickupClientConfig`).
Nagłówek `Authorization: <token>` (bez `Bearer`) na każdym wywołaniu — per wzorzec F-01.

Metody:
- `List<ClickupTeam> getTeams(String token)` ← `GET /team`
- `List<ClickupSpace> getSpaces(String token, String teamId)` ← `GET /team/{teamId}/space?archived=false`
- `List<ClickupFolder> getFolders(String token, String spaceId)` ← `GET /space/{spaceId}/folder?archived=false`
- `List<ClickupList> getFolderlessLists(String token, String spaceId)` ← `GET /space/{spaceId}/list?archived=false`
- `List<ClickupList> getListsByFolder(String token, String folderId)` ← `GET /folder/{folderId}/list?archived=false`
- `List<ClickupTask> getTasks(String token, String listId, @Nullable Instant since)` ← `GET /list/{listId}/task?include_closed=true&subtasks=true[&date_updated_gt=<epochMs>]`; pętla stronami `page=0,1,...` dopóki `last_page == false`; zwraca flatlistę wszystkich stron

Retry: prywatna metoda `<T> T withRetry(Supplier<T> call)` — łapie `RestClientResponseException`
z status 429, czeka 1 s → 2 s → 4 s (max 3 próby), po wyczerpaniu propaguje wyjątek.

#### 3. Testy ClickupWorkspaceClient

**File**: `server/src/test/java/com/example/clickupsimplifier/clickup/workspace/ClickupWorkspaceClientTest.java`

**Intent**: Zweryfikować poprawność requestów (path, auth header, query params) i edge cases
— wzorzec `MockRestServiceServer` z F-01.

**Contract**: Scenariusze unit testów:
- Każda metoda: poprawny path, nagłówek `Authorization = token` (bez Bearer), query params
- `getTasks` z paginacją: mock 2 strony (`last_page: false` → `last_page: true`); wynik = concat obu stron
- `getTasks` z `since != null`: query param `date_updated_gt` = epochMillis od `Instant`
- 429 retry: mock 429 → 429 → 200; wynik OK; backoff wywołany (3 próby)
- 429 po 3 próbach wyczerpanych: wyjątek propagowany do wywołującego

### Success Criteria

#### Automated Verification

- `mvn test` (toolchain) przechodzi
- Wszystkie scenariusze `ClickupWorkspaceClientTest` zielone: path/header/params, paginacja,
  `since` param jako `date_updated_gt`, retry-on-429 (backoff + fail-after-3)

#### Manual Verification

- Wywołanie `getTeams` z realnym tokenem zwraca workspace name (opcjonalne, przez tymczasowy
  test/main)

**Implementation Note**: Zatrzymaj się przed Phase 3.

---

## Phase 3: WorkspaceSyncService

### Overview

Serwis orkiestrujący pełny pull: `@Async` trigger, dwie `@Transactional` operacje per zestaw
sync (dictionaries → tasks), replace-all z FK-safe delete przez CASCADE, parametr `since`
pod S-02. In-memory `SyncJobStatus` śledzi stan bieżącego job'a.

### Changes Required

#### 0. Migracja Flyway V3 + oznaczenie list do sync

**File**: `server/src/main/resources/db/migration/V3__list_sync_enabled.sql`

**Intent**: Dodać pole opt-in `sync_enabled` do tabeli `list`. Domyślnie `false` — użytkownik
świadomie oznacza, które listy uczestniczą w synchronizacji tasków. Synchronizacja słowników
(spaces/folders/lists) pozostaje niezmieniona i zawsze pobiera pełny workspace.

**Contract**:
- `ALTER TABLE list ADD COLUMN sync_enabled boolean NOT NULL DEFAULT false`

**File**: `server/src/main/java/com/example/clickupsimplifier/persistence/WorkspaceList.java`

**Intent**: Odzwierciedlić `sync_enabled` w rekordzie domenowym.

**Contract**: Dodaj pole `boolean syncEnabled` do rekordu. Mapowanie camelCase→snake_case przez
konwencję Spring Data JDBC — bez adnotacji. Przy upsertcie nowo pobranych list z ClickUp
`sync_enabled = false` (domyślna wartość DB, nie ma w DTO ClickUp).

**File**: `server/src/main/java/com/example/clickupsimplifier/persistence/WorkspaceListRepository.java`

**Intent**: Dostarczyć metody filtrowania i zarządzania znacznikiem sync per lista.

**Contract**:
- `@Query("SELECT * FROM list WHERE sync_enabled = true") List<WorkspaceList> findAllSyncEnabled()`
- `@Modifying @Query("UPDATE list SET sync_enabled = :enabled WHERE id = :id") void updateSyncEnabled(@Param("id") String id, @Param("enabled") boolean enabled)`

**Testy (rozszerzenie istniejących)**:
- `SchemaMigrationTest`: kolumna `sync_enabled` na `list`, wartość domyślna `false` potwierdzona
  przez INSERT + SELECT bez podania pola
- `RepositoryIntegrationTest` (lub nowy `WorkspaceListRepositoryTest`): `findAllSyncEnabled`
  zwraca tylko listy z `sync_enabled = true`; `updateSyncEnabled` zmienia wartość; upsert
  istniejącej listy nie resetuje wartości `sync_enabled` ustawionej ręcznie (upsert nie dotyka
  pola `sync_enabled`)

#### 1. @EnableAsync konfiguracja

**File**: `server/src/main/java/com/example/clickupsimplifier/config/AsyncConfig.java`

**Intent**: Włączyć Spring `@Async` w aplikacji.

**Contract**: `@Configuration @EnableAsync class AsyncConfig` — samo `@EnableAsync` z domyślnym
executorem wystarczy dla S-01.

#### 2. SyncJobStatus

**File**: `server/src/main/java/com/example/clickupsimplifier/sync/SyncJobStatus.java`

**Intent**: In-memory, thread-safe reprezentacja stanu bieżącego pull job'a.

**Contract**: `enum SyncState { IDLE, RUNNING, COMPLETED, FAILED }`. Rekord
`SyncJobStatus(SyncState state, @Nullable String message, @Nullable Instant startedAt, @Nullable Instant completedAt)`
+ statyczne factory methods: `idle()`, `running(Instant)`, `completed(Instant)`, `failed(String, Instant)`.

#### 3. WorkspaceSyncService

**File**: `server/src/main/java/com/example/clickupsimplifier/sync/WorkspaceSyncService.java`

**Intent**: Orkiestrować pełny pull workspace asynchronicznie: trigger → słowniki (@Transactional)
→ taski (@Transactional) → update statusu. Parametr `since` wykłada kontrakt pod S-02.

**Contract**:

`@Service` z `AtomicReference<SyncJobStatus> currentStatus` (inicjalnie `idle()`).

`SyncJobStatus getStatus()` — publiczny, thread-safe odczyt statusu.

`@Async void triggerPull(@Nullable Instant since)`:
- Ustawia `currentStatus` na `running(now)` (akceptuje równoległy wywołanie bez blokowania —
  przy RUNNING po prostu nadpisze; kontroler sprawdza przed wywołaniem)
- Pobiera token z `SettingsStore`; bierze `teams.get(0).id()` z `getTeams`
- Woła `syncDictionaries(token, teamId, since)`, następnie `syncTasks(token, teamId, since)`
- Po sukcesie: `currentStatus.set(completed(now))`
- Przy wyjątku: `currentStatus.set(failed(e.getMessage(), now))`; loguje błąd

`@Transactional void syncDictionaries(String token, String teamId, @Nullable Instant since)`:
- Fetch: `getSpaces` → per space: `getFolders` + `getFolderlessLists` → per folder: `getListsByFolder`
- Collect fresh IDs: `freshSpaceIds`, `freshFolderIds`, `freshListIds`
- Gdy `since == null` (replace-all):
  - `listRepo.deleteByIdNotIn(freshListIds)` (CASCADE → stale taski pod stale listami usunięte)
  - `folderRepo.deleteByIdNotIn(freshFolderIds)` (CASCADE → stale listy pod stale folderami)
  - `spaceRepo.deleteByIdNotIn(freshSpaceIds)` (CASCADE → reszta)
- Upsert root→leaf: spaces → folders → lists (FK wymaga parent przed child)
- `syncSetRepo.updateLastSyncedAt("dictionaries", Instant.now())`

`@Transactional void syncTasks(String token, String teamId, @Nullable Instant since)`:
- Fetch `enabledLists = listRepo.findAllSyncEnabled()` z DB (po `syncDictionaries`; listy
  z `sync_enabled = false` nie są dotykane — ich taski nie są pobierane ani kasowane)
- Per lista z `enabledLists`: `getTasks(token, list.id(), since)` → flatten (top-level tasks
  + subtaski z dziedziczonym `listId` rodzica)
- Gdy `since == null`: `taskRepo.deleteStaleByListId(listId, freshTaskIds)` per enabled lista
- Upsert: top-level taski (parent=null) PRZED subtaskami (parent!=null) — FK `parent_id`
  wymaga istnienia rodzica
- `syncSetRepo.updateLastSyncedAt("tasks", Instant.now())`

#### 4. Testy WorkspaceSyncService

**File**: `server/src/test/java/com/example/clickupsimplifier/sync/WorkspaceSyncServiceTest.java`

**Intent**: Zweryfikować logikę replace-all (delete + upsert), parametr `since`, obsługę błędu
API i aktualizację `sync_set`.

**Contract**: Testy z Testcontainers Postgres (real DB) + mockiem `ClickupWorkspaceClient`
(Mockito). Scenariusze:
- Pełny pull (since=null): po pull'u DB zawiera dokładnie to co zwrócił mock; wcześniej
  zaseedowane stale encje usunięte
- Przyrostowy (since=Instant): brak delete; upsertowane tylko zwrócone przez mock
- Błąd w `getSpaces` (wyjątek): status FAILED; `last_synced_at` nie zaktualizowane;
  poprzednie dane w DB nienaruszone (rollback @Transactional)
- Subtaski: `list_id` odziedziczone z rodzica; parent-task upsertowany przed subtaskiem
- Filtrowanie sync_enabled: dwie listy zaseedowane w DB — jedna `sync_enabled = true`, druga
  `false`; po sync mock `getTasks` wołany tylko raz (dla enabled); DB zawiera taski tylko
  enabled listy; taski disabled listy (jeśli wcześniej zaseedowane) pozostają nienaruszone

### Success Criteria

#### Automated Verification

- `mvn test` (toolchain + Docker) przechodzi
- `WorkspaceSyncServiceTest`: replace-all (stale usunięte), przyrostowy (brak delete), błąd
  → rollback + status FAILED, subtaski z poprawnym `list_id`/`parent_id`
- Istniejące testy F-01 i F-02 nadal przechodzą

#### Manual Verification

- Brak (serwis bez REST surface nie jest jeszcze wyzwalainy ręcznie — Phase 4 to odblokuje)

**Implementation Note**: Zatrzymaj się przed Phase 4.

---

## Phase 4: REST Surface + Integration Smoke-Test

### Overview

Cienki kontroler eksponujący trigger pull i status. Integracyjny smoke-test weryfikuje
cały przepływ end-to-end z mockowanym ClickUp HTTP i realnym Postgres.

### Changes Required

#### 1. SyncController

**File**: `server/src/main/java/com/example/clickupsimplifier/sync/SyncController.java`

**Intent**: Wystawić dwa endpointy synchronizacji; kontroler tylko deleguje — zero logiki.
Oba triggery synkują wyłącznie listy z `sync_enabled = true` (zachowanie w `WorkspaceSyncService`).

**Contract**:
- `POST /api/sync/full-pull`:
  - Sprawdza `syncService.getStatus().state() == RUNNING` → 409 z komunikatem "Sync already in progress"
  - W przeciwnym razie: `syncService.triggerPull(null)` (fire-and-forget przez `@Async`) → 202 bez body
- `GET /api/sync/status` → 200 z `SyncStatusResponse`:
  `{ state, message, startedAt, completedAt, syncSets: { dictionaries: { lastSyncedAt }, tasks: { lastSyncedAt } } }`
  Dane z `syncService.getStatus()` (in-memory) + `syncSetRepo.findAll()` (persystowane timestamps).

#### 2. ListController

**File**: `server/src/main/java/com/example/clickupsimplifier/sync/ListController.java`

**Intent**: Wystawić zarządzanie znacznikiem `sync_enabled` per lista. Użytkownik przeglądą
wszystkie listy z workspace i oznacza te, których taski mają być synchronizowane.

**Contract**:
- `GET /api/lists` → 200 z listą `ListResponse`:
  `[{ id, name, syncEnabled, folderId, spaceId }]` — wszystkie listy z lokalnej kopii,
  posortowane `space_id, folder_id NULLS LAST, name`. Źródło: `workspaceListRepo.findAll()`
  + ręczne sortowanie lub ORDER BY w query.
- `PUT /api/lists/{id}/sync-enabled` z body `{ "enabled": true|false }` → 204;
  `workspaceListRepo.updateSyncEnabled(id, enabled)`.
  Gdy lista o danym `id` nie istnieje: 404.

**File**: `server/src/main/java/com/example/clickupsimplifier/sync/dto/ListResponse.java`

**Contract**: Rekord `ListResponse(String id, String name, boolean syncEnabled,
@Nullable String folderId, String spaceId)`.

**File**: `server/src/main/java/com/example/clickupsimplifier/sync/dto/SyncEnabledRequest.java`

**Contract**: Rekord `SyncEnabledRequest(boolean enabled)` — body `PUT` requesta.

#### 3. SyncStatusResponse DTO

**File**: `server/src/main/java/com/example/clickupsimplifier/sync/dto/SyncStatusResponse.java`

**Intent**: Jawny kontrakt JSON odpowiedzi statusu, łączący stan job (in-memory) z
persystowanymi timestamps per zestaw.

**Contract**: Rekord z polami: `String state`, `@Nullable String message`, `@Nullable Instant
startedAt`, `@Nullable Instant completedAt`, `Map<String, SyncSetStatus> syncSets` gdzie
`SyncSetStatus(Instant lastSyncedAt)`.

#### 4. SyncController test (slice)

**File**: `server/src/test/java/com/example/clickupsimplifier/sync/SyncControllerTest.java`

**Intent**: Zweryfikować kontrakty HTTP endpointów z mockowanym serwisem.

**Contract**: `@WebMvcTest(SyncController.class)` + `@MockitoBean WorkspaceSyncService`.
Scenariusze:
- `POST /api/sync/full-pull` gdy IDLE → 202; `triggerPull(null)` wywołany
- `POST /api/sync/full-pull` gdy RUNNING → 409
- `GET /api/sync/status` → 200 z polem `state` i `syncSets`

#### 5. ListController test (slice)

**File**: `server/src/test/java/com/example/clickupsimplifier/sync/ListControllerTest.java`

**Intent**: Zweryfikować kontrakty HTTP endpointów zarządzania znacznikiem.

**Contract**: `@WebMvcTest(ListController.class)` + `@MockitoBean WorkspaceListRepository`.
Scenariusze:
- `GET /api/lists` → 200 z poprawną listą (mock repozytorium zwraca 2 listy)
- `PUT /api/lists/{id}/sync-enabled` z `{ "enabled": true }` → 204; `updateSyncEnabled` wywołane
- `PUT /api/lists/{id}/sync-enabled` gdy lista nie istnieje (mock zwraca 0 rows updated) → 404

#### 6. Integracyjny smoke-test end-to-end

**File**: `server/src/test/java/com/example/clickupsimplifier/sync/FullPullIntegrationTest.java`

**Intent**: Zweryfikować pełen przepływ: trigger → async pull → DB wypełniona — z mockowanym
HTTP ClickUp i realnym Postgres (Testcontainers).

**Contract**: `@SpringBootTest` + `PostgresTestcontainersConfig` + `MockRestServiceServer`
(mock API ClickUp zwraca minimalny workspace: 1 space, 1 folder, 2 listy, 2 taski w tym 1
subtask — taski należą do pierwszej listy). Kroki:
1. Zapisz token przez `SettingsStore`
2. `POST /api/sync/full-pull` → 202; czekaj na `COMPLETED` (polling max 5 s)
3. Asercja: space/folder/listy obecne w DB; task tabela pusta (żadna lista nie ma
   `sync_enabled = true` po pierwszym pull'u)
4. `PUT /api/lists/{firstListId}/sync-enabled` z `{ "enabled": true }` → 204
5. `POST /api/sync/full-pull` → 202; czekaj na `COMPLETED`
6. Asercja DB: task/subtask pierwszej listy obecne; taski drugiej listy nieobecne;
   `sync_set.last_synced_at` nie-null dla obu rows

### Success Criteria

#### Automated Verification

- `mvn test` (toolchain + Docker) przechodzi
- `SyncControllerTest`: 202/409 na trigger, 200 z poprawnym body na status
- `ListControllerTest`: GET lista, PUT enable → 204, PUT nieistniejąca → 404
- `FullPullIntegrationTest`: pełny scenariusz z sync_enabled — po pierwszym pull task tabela
  pusta; po oznaczeniu listy i drugim pull taski enabled listy obecne, disabled listy puste

#### Manual Verification

- `POST /api/sync/full-pull` z realnym tokenem → 202; polling → COMPLETED;
  task tabela pusta (żadna lista nie oznaczona)
- `GET /api/lists` → lista wszystkich list z `syncEnabled: false`
- `PUT /api/lists/{id}/sync-enabled` z `{"enabled": true}` dla wybranej listy → 204
- Ponowny `POST /api/sync/full-pull` → COMPLETED; `SELECT count(*) FROM task` > 0 dla
  oznaczonej listy; `last_synced_at` zaktualizowane

**Implementation Note**: Po automatycznej weryfikacji zatrzymaj się na ręczne potwierdzenie domknięcia S-01.

---

## Testing Strategy

### Unit Tests

- `ClickupWorkspaceClientTest`: per-endpoint path/auth/params, paginacja, `since` param,
  429 retry (backoff + fail-after-3) — `MockRestServiceServer`
- `SyncControllerTest`: HTTP contracts — `@WebMvcTest`
- `ListControllerTest`: GET /api/lists, PUT /api/lists/{id}/sync-enabled — `@WebMvcTest`
- `WorkspaceSyncServiceTest`: replace-all logika, rollback na błąd, subtaski,
  filtrowanie sync_enabled — Testcontainers + Mockito

### Integration Tests

- `FullPullIntegrationTest`: end-to-end z mock ClickUp HTTP + real Postgres; weryfikuje
  scenariusz sync_enabled (oznacz listę → pull → taski present)
- `SchemaMigrationTest` (rozszerzony): V2 + V3 schemat poprawny; `sync_enabled` default false

### Manual Testing Steps

1. `POST /api/sync/full-pull` (curl lub klient REST) z realnym tokenem — oczekuj 202 + COMPLETED;
   `SELECT count(*) FROM task` = 0 (żadna lista nieoznaczona)
2. `GET /api/lists` — przegląd dostępnych list z workspace; wybierz id jednej
3. `PUT /api/lists/{id}/sync-enabled` z `{"enabled":true}` — 204
4. Ponowny `POST /api/sync/full-pull` → COMPLETED; `SELECT count(*) FROM task` > 0
5. `SELECT * FROM sync_set` — oba `last_synced_at` nie-null
6. Usuń manualnie 1 space z `psql` (symulacja stale); ponowny pull → space powróci

## Performance Considerations

NFR ~100 ms dla nawigacji (odczyt lokalnej kopii) — nienaruszone przez S-01 (pull jest
operacją bulk w tle, nie na ścieżce nawigacji). Indeksy V1 + V2 (`idx_task_parent_id`)
pokrywają ścieżki zapytań. Przy bardzo dużym workspace (> 1000 list) pull może trwać
kilka minut ze względu na rate limit 100 req/min — akceptowalne dla jednorazowej operacji.

## Migration Notes

V2 przebudowuje istniejące FK przez `DROP CONSTRAINT` + `ADD CONSTRAINT`. Postgres
wykonuje to atomowo; nie ma okna niespójności. Dane V1 są nienaruszone. Aplikacja wymaga
lokalnego Postgres z bazą `simplifier` (jak w F-02).

## References

- Roadmap: `context/foundation/roadmap.md` → S-01 + implementation notes z review F-02
- F-01 plan: `context/changes/clickup-token-and-connectivity/plan.md` (wzorzec RestClient, retry, testy)
- F-02 plan: `context/archive/2026-06-20-local-copy-persistence/plan.md` (schemat V1, repozytoria)
- Lessons: `context/foundation/lessons.md` (Jackson 3, test-slice'y Boot 4, Testcontainers 2.x)
- PRD: `context/foundation/prd.md` → FR-002, FR-003

---

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Schema V2 + Domain Update

#### Automated

- [x] 1.1 `mvn test` (toolchain + Docker) przechodzi; V2 migruje czysto na Testcontainers Postgres — ea38e33
- [x] 1.2 Test schematu: `parent_id` na task, `sync_set` z 2 rows, CASCADE FK (delete list → taski kaskadowo) — ea38e33
- [x] 1.3 Test integracyjny SyncSet: zapis, `updateLastSyncedAt`, odczyt `lastSyncedAt` — ea38e33
- [x] 1.4 Istniejące testy F-02 (`RepositoryIntegrationTest`) nadal przechodzą — ea38e33

#### Manual

- [x] 1.5 Schemat V2 zaaplikowany na lokalnym Postgresie; `sync_set` zawiera 2 rows; `parent_id` widoczny w psql — ea38e33

### Phase 2: ClickUp Workspace API Client

#### Automated

- [x] 2.1 `mvn test` (toolchain) przechodzi — f4bc50d
- [x] 2.2 `ClickupWorkspaceClientTest`: path/header/params poprawne dla każdej z 6 metod — f4bc50d
- [x] 2.3 Paginacja tasków: mock 2 strony → wynik = concat obu — f4bc50d
- [x] 2.4 `since` param: `date_updated_gt` = epochMillis od Instant w query — f4bc50d
- [x] 2.5 429 retry: 3 próby, backoff 1s→2s→4s; po wyczerpaniu wyjątek propagowany — f4bc50d

#### Manual

- [x] 2.6 (opcjonalne) `getTeams` z realnym tokenem zwraca workspace name — f4bc50d

### Phase 3: WorkspaceSyncService + Sync Marker Schema

#### Automated

- [x] 3.1 `mvn test` (toolchain + Docker) przechodzi — 083b02e
- [x] 3.2 V3 migracja: kolumna `sync_enabled` na `list`, domyślna wartość `false` — 083b02e
- [x] 3.3 `WorkspaceListRepository`: `findAllSyncEnabled` zwraca tylko enabled; `updateSyncEnabled` zmienia wartość; upsert nie resetuje flagi — 083b02e
- [x] 3.4 Replace-all (since=null): stale encje słownikowe usunięte; fresh upsertowane — 083b02e
- [x] 3.5 Przyrostowy (since=Instant): brak delete; upsert tylko zmienionych — 083b02e
- [x] 3.6 Błąd w fetch → rollback @Transactional; `last_synced_at` nie zaktualizowane; status FAILED — 083b02e
- [x] 3.7 Subtaski: `list_id` odziedziczone z rodzica; `parent_id` poprawny; parent upsertowany przed subtaskiem — 083b02e
- [x] 3.8 Filtrowanie sync_enabled: mock `getTasks` wywołany tylko dla enabled listy; disabled lista nienaruszona — 083b02e

#### Manual

- [x] 3.9 (brak ręcznego — Phase 4 odblokuje wyzwalanie) — 083b02e

### Phase 4: REST Surface + Integration Smoke-Test

#### Automated

- [x] 4.1 `mvn test` (toolchain + Docker) przechodzi — 810cca5
- [x] 4.2 `SyncControllerTest`: 202 gdy IDLE, 409 gdy RUNNING, 200 ze stanem na GET status — 810cca5
- [x] 4.3 `ListControllerTest`: GET lista, PUT enable → 204, PUT nieistniejąca → 404 — 810cca5
- [x] 4.4 `FullPullIntegrationTest`: po pierwszym pull task pusta; po oznaczeniu listy i drugim pull taski enabled listy obecne — 810cca5

#### Manual

- [x] 4.5 `POST /api/sync/full-pull` z realnym tokenem → 202; COMPLETED; task pusta (żadna lista nieoznaczona) — 810cca5
- [x] 4.6 `GET /api/lists` → lista list z `syncEnabled: false`; `PUT` dla wybranej → 204 — 810cca5
- [x] 4.7 Ponowny pull → taski oznaczonej listy w DB; `last_synced_at` zaktualizowane — 810cca5
