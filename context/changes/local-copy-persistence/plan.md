# Minimalna lokalna warstwa trwałości (F-02) — Implementation Plan

## Overview

Dajemy rdzeniowi Spring pierwszą warstwę trwałości: lokalną kopię workspace ClickUp
w PostgreSQL, zorganizowaną wokół hierarchii **Space → Folder → List → Task** z
dwupoziomowym modelem milestone→task wyrażonym na encji Task. To fundament F-02 —
minimalny magazyn, do którego pierwszy pull (S-01) będzie zapisywał, a nawigacja
(S-03) czytała. Zakres celowo wąski: tylko encje potrzebne pierwszemu pullowi,
z **idempotentnym upsertem** (klucz = id ClickUp), nie cały schemat domeny z góry.

## Current State Analysis

- `server/` to Spring Boot **4.1.0** (Java 21, Maven). Po F-01 istnieją pakiety
  `config/` (`ClickupProperties`), `settings/` (`SettingsStore` — JSON na dysku) i
  `clickup/` (klient + łączność). **Zero trwałości danych domenowych.**
- `pom.xml` ma tylko `spring-boot-starter-webmvc`, `devtools`, `spring-boot-starter-webmvc-test`.
  Brak sterownika DB, Spring Data, migracji, Testcontainers.
- NFR (PRD): nawigacja/odczyt lokalnej kopii ~100 ms przy rozmiarze całego workspace;
  brak utraty lokalnej zmiany podczas sync. → magazyn musi być realnie odpytywalny
  i indeksowany (płaski JSON jak `SettingsStore` nie wystarczy).
- Sync modelowany jako dwa zestawy: „Podstawowe słowniki" (folders/lists) i „Zadania"
  (tasks). FR-005: wybór kontekstu (folder itp.). FR-008: tasks ściśle pod milestone'ami;
  nieprzypisane pod wirtualnym „no milestone" (NULL milestone_id).
- W ClickUp **milestone to task** z flagą; hierarchia to Space → Folder → List → Task
  (listy mogą wisieć bezpośrednio pod Space — „folderless").

## Desired End State

Po zakończeniu planu:
- Aplikacja łączy się z lokalnie zainstalowanym PostgreSQL; schemat zarządzany przez
  Flyway (V1) tworzy tabele `space`, `folder`, `list`, `task`.
- Istnieją repozytoria Spring Data JDBC (po jednym agregacie na encję) z **idempotentnym
  upsertem** po id ClickUp: dwukrotny zapis tego samego id aktualizuje wiersz, nie duplikuje.
- Można zapisać i odczytać pełną hierarchię; task niesie `is_milestone` + nullowalny
  `milestone_id` (self-ref), więc model milestone→task i „no milestone" są reprezentowalne.
- Wszystko pokryte testami integracyjnymi repozytoriów na efemerycznym Postgresie
  (Testcontainers), bez realnych wywołań ClickUp.

Weryfikacja: `mvn test` (toolchain F-01 + Docker) przechodzi; po starcie aplikacji
przeciw lokalnemu Postgresowi tabele i indeksy są obecne.

### Key Discoveries:

- Spring Boot 4.1 niesie Flyway 10+ → dla Postgresa wymagany moduł
  **`flyway-database-postgresql`** obok `flyway-core`.
- **Upsert z zewnętrznym PK**: Spring Data JDBC `save()` dla encji z już ustawionym
  `@Id` wykonuje UPDATE (0 wierszy przy pierwszym zapisie), a wymuszenie INSERT przez
  `Persistable.isNew()` wywali PK violation przy ponownym pullu. Idempotentny upsert =
  jawne `INSERT ... ON CONFLICT (id) DO UPDATE SET ...` jako metoda repozytorium.
- Każda encja to **własny agregat** (osobne repo); relacje to zwykłe kolumny-referencje
  po id (nie `@MappedCollection`), bo upsert idzie per-encja niezależnie.
- Testy slice'owe Boot 4 są w modularnych pakietach (lessons.md) — `@DataJdbcTest`
  importować z pakietu Boot 4, nie z Boot 3.
- Brak `docs/reference/contract-surfaces.md` — brak rejestru nazw do uwzględnienia.

## What We're NOT Doing

- **Pull / jakiekolwiek wywołania ClickUp** — to S-01. Tu tylko magazyn + kontrakt zapisu/odczytu.
- **Pełny schemat domeny** — tylko Space/Folder/List/Task + pola milestone na Task; bez
  komentarzy, załączników, custom fields, statusów jako osobnych słowników itd.
- **Tworzenie/edycja milestone'ów i tasków** (zapis do modelu z UI) — S-03/S-04/S-06.
- **Sync przyrostowy, kadencje, status zestawów** — S-02 / S-07 (schemat może wtedy ewoluować przez kolejne migracje Flyway).
- **Szyfrowanie danych on-device / bezpieczeństwo magazynu** — poza zakresem MVP.
- **Warstwa serwisowa nawigacji / prezentacja FR-008** — tu tylko zapytania-fundamenty; widok milestone→task buduje S-03.

## Implementation Approach

Trzy fazy, każda samodzielnie testowalna: (1) fundament trwałości — zależności,
datasource, Flyway, harness Testcontainers, test ładowania kontekstu; (2) migracja
schematu V1 z tabelami i indeksami; (3) model domenowy + repozytoria + idempotentny
upsert + testy integracyjne. Dane testowe są syntetyczne (fixtures); realne kształty
ClickUp dociśnie S-01.

## Critical Implementation Details

- **Build & toolchain + Docker (F1)** — testy uruchamiać jak w F-01:
  `JAVA_HOME="$JAVA_HOME21" "$MAVEN_HOME9/bin/mvn" -B test` w `server/`. **Dodatkowo:
  Testcontainers wymaga działającego Dockera** — to nowy warunek `mvn test`. Runtime
  aplikacji łączy się z **lokalnie zainstalowanym** Postgresem (osobno od testów).
- **Idempotentny upsert (F2)** — patrz Key Discoveries: użyć `INSERT ... ON CONFLICT (id) DO UPDATE`
  jako jawnej metody repo; nie polegać na `save()`/`Persistable` dla wierszy kluczowanych id ClickUp.
- **Kolizja nazwy `List`** — encji listy ClickUp NIE nazywać `List` (kolizja z `java.util.List`);
  użyć np. `WorkspaceList` (tabela `list`).
- **Folderless lists** — `list.folder_id` jest nullowalne; `list.space_id` NOT NULL, bo lista
  zawsze należy do Space (z folderem lub bez).
- **`@DataJdbcTest` pod Boot 4** — importować adnotację z modularnego pakietu Boot 4 (lessons.md:
  test-slice'y przeniesione); alternatywnie `@SpringBootTest` + Testcontainers, jeśli slice sprawia kłopot.

## Phase 1: Fundament trwałości (Postgres + Flyway + Testcontainers)

### Overview

Wprowadzenie zależności i konfiguracji, by aplikacja łączyła się z Postgresem, Flyway
był aktywny, a testy miały efemeryczny Postgres przez Testcontainers.

### Changes Required:

#### 1. Zależności Maven

**File**: `server/pom.xml`

**Intent**: Dodać trwałość (Postgres + Spring Data JDBC), migracje (Flyway) i harness testów (Testcontainers).

**Contract**: Dodać zależności: `org.springframework.boot:spring-boot-starter-data-jdbc`;
`org.postgresql:postgresql` (runtime); `org.flywaydb:flyway-core` + `org.flywaydb:flyway-database-postgresql`;
test-scope: `org.springframework.boot:spring-boot-testcontainers`, `org.testcontainers:postgresql`,
`org.testcontainers:junit-jupiter`. Wersje z BOM Spring Boot/Testcontainers (bez ręcznego pinowania).

#### 2. Konfiguracja datasource + Flyway

**File**: `server/src/main/resources/application.properties`

**Intent**: Wskazać lokalny Postgres i włączyć Flyway, z sensownymi domyślnymi do nadpisania przez env.

**Contract**: `spring.datasource.url` (np. `jdbc:postgresql://localhost:5432/clickup_simplifier`),
`spring.datasource.username`/`password` (domyślne lokalne, nadpisywalne env). Flyway włączony
(domyślnie on, gdy obecny). Lokalizacja migracji domyślna (`classpath:db/migration`).

#### 3. Harness Testcontainers

**File**: `server/src/test/java/com/example/clickupsimplifier/persistence/PostgresTestcontainersConfig.java` (lub klasa bazowa)

**Intent**: Dostarczyć testom Postgres w kontenerze, automatycznie wpięty jako DataSource.

**Contract**: `@TestConfiguration` z beanem `PostgreSQLContainer` oznaczonym `@ServiceConnection`
(Spring Boot wiąże datasource automatycznie), importowalna przez testy. Kontener reużywalny per klasa/run.

### Success Criteria:

#### Automated Verification:

- Testy przechodzą wg toolchainu (JAVA_HOME21 + MAVEN_HOME9) w `server/`, z działającym Dockerem
- Test ładowania kontekstu (`@SpringBootTest` + harness Testcontainers) bootuje aplikację na Postgresie z kontenera — dowodzi, że datasource + Spring Data JDBC + Flyway autoconfig wpinają się
- Istniejący test kontekstu F-01 nadal przechodzi

#### Manual Verification:

- Aplikacja startuje przeciw lokalnie zainstalowanemu Postgresowi (skonfigurowany URL) bez błędów połączenia/migracji

**Implementation Note**: Po automatycznej weryfikacji zatrzymaj się na ręczne potwierdzenie przed Fazą 2.

---

## Phase 2: Migracja schematu (V1)

### Overview

Pierwsza migracja Flyway tworząca tabele lokalnej kopii i indeksy pod NFR nawigacji.

### Changes Required:

#### 1. Migracja V1

**File**: `server/src/main/resources/db/migration/V1__create_workspace_copy.sql`

**Intent**: Utworzyć tabele Space/Folder/List/Task z kluczami id ClickUp, relacjami FK,
polami milestone i indeksami FK.

**Contract**:
- `space(id text PK, name text not null)`.
- `folder(id text PK, space_id text not null REFERENCES space(id), name text not null)`.
- `list(id text PK, name text not null, space_id text not null REFERENCES space(id), folder_id text NULL REFERENCES folder(id))`.
- `task(id text PK, list_id text not null REFERENCES list(id), name text not null, status text NULL, description text NULL, is_milestone boolean not null default false, milestone_id text NULL REFERENCES task(id))`.
- Indeksy na: `folder.space_id`, `list.space_id`, `list.folder_id`, `task.list_id`, `task.milestone_id`.

### Success Criteria:

#### Automated Verification:

- `mvn test` (toolchain + Docker) przechodzi; Flyway aplikuje V1 czysto na Postgresie z Testcontainers
- Test schematu asercją potwierdza obecność czterech tabel i kluczowych kolumn/ograniczeń (np. odpyt `information_schema` albo smoke repo)

#### Manual Verification:

- W lokalnym Postgresie po starcie aplikacji widoczne tabele + indeksy (psql/inspekcja)

**Implementation Note**: Po automatycznej weryfikacji zatrzymaj się na ręczne potwierdzenie przed Fazą 3.

---

## Phase 3: Model domenowy + repozytoria + idempotentny upsert

### Overview

Rekordy domenowe Spring Data JDBC, repozytoria z upsertem `ON CONFLICT` i testy
integracyjne dowodzące trwałości, idempotencji i relacji.

### Changes Required:

#### 1. Rekordy domenowe

**File**: `server/src/main/java/com/example/clickupsimplifier/persistence/` (rekordy `Space`, `Folder`, `WorkspaceList`, `Task`)

**Intent**: Zamapować wiersze tabel na agregaty (po jednym na encję), z id ClickUp jako `@Id`.

**Contract**: Rekordy z `@Table` i `@Id String id`; pozostałe pola wg kolumn z V1. Relacje jako
zwykłe pola-referencje po id (np. `Task.listId`, `Task.milestoneId`), bez `@MappedCollection`.
`WorkspaceList` mapuje tabelę `list` (unik kolizji z `java.util.List`).

#### 2. Repozytoria + upsert

**File**: `server/src/main/java/com/example/clickupsimplifier/persistence/` (interfejsy repozytoriów)

**Intent**: Dostarczyć CRUD + idempotentny upsert per encja oraz zapytania-fundamenty pod pull/nawigację.

**Contract**: Po jednym `CrudRepository<T, String>` na encję. Każde repo ma metodę
`upsert(...)` = `@Modifying @Query("INSERT ... ON CONFLICT (id) DO UPDATE SET ...")` (idempotentnie).
Zapytania nawigacyjne potrzebne dalej: `findByListId` (taski listy), `findByMilestoneId`/`findByListIdAndIsMilestoneTrue`
(milestone'y), `findBySpaceId` (foldery/listy). Bez warstwy serwisowej prezentacji (to S-03).

### Success Criteria:

#### Automated Verification:

- `mvn test` (toolchain + Docker) przechodzi
- Zapis→odczyt każdej encji zwraca te same dane (Testcontainers Postgres)
- Idempotencja upsertu: dwukrotny upsert tego samego id ClickUp aktualizuje wiersz (brak duplikatu, brak PK violation), zmienione pole odzwierciedlone
- Milestone: task z `milestone_id = NULL` („no milestone") oraz task z `milestone_id` wskazującym task-milestone (self-ref) zapisują się i odczytują; `is_milestone` zachowane
- Relacje FK: folder→space, list→folder/space, task→list rozwiązują się; zapytania nawigacyjne (taski po liście, milestone'y po liście) zwracają poprawne zbiory
- Trwałość: dane przeżywają ponowne odpytanie/„restart" repo w obrębie kontenera

#### Manual Verification:

- (opcjonalnie) po ręcznym zaseedowaniu w lokalnym Postgresie wiersze wyglądają poprawnie (psql)

**Implementation Note**: Po automatycznej weryfikacji zatrzymaj się na ręczne potwierdzenie domknięcia F-02.

---

## Testing Strategy

### Unit Tests:

- Brak czysto jednostkowych (warstwa jest I/O-bound); rdzeń to testy integracyjne repo.

### Integration Tests:

- Repozytoria na efemerycznym Postgresie (Testcontainers, `@ServiceConnection`): zapis-odczyt,
  idempotencja upsertu, milestone null/self-ref, relacje FK i zapytania nawigacyjne.
- Test migracji: Flyway V1 aplikuje się czysto; schemat zgodny z kontraktem.

### Manual Testing Steps:

1. Uruchom lokalny Postgres, ustaw URL/credentiale; wystartuj aplikację — Flyway tworzy schemat.
2. (opcjonalnie) zaseeduj kilka wierszy i sprawdź w `psql`, że hierarchia i milestone_id są poprawne.
3. Zrestartuj aplikację — dane nadal obecne (trwałość on-device).

## Performance Considerations

NFR ~100 ms na całym workspace: indeksy na kolumnach FK (powyżej) pokrywają ścieżki
nawigacji (taski po liście, foldery/listy po space, milestone'y po liście). Wolumen
danych „small" (PRD) — Postgres z indeksami z zapasem mieści budżet. Bez optymalizacji
przedwczesnych; realny profil dociśnie S-01/S-03.

## Migration Notes

Pierwsza warstwa trwałości — brak istniejących danych do migracji. Schemat zakładany
przez Flyway V1; kolejne zmiany (S-02 status zestawów, kadencje) dojdą jako V2+.

## References

- Roadmap: `context/foundation/roadmap.md` → F-02 (`local-copy-persistence`)
- PRD: `context/foundation/prd.md` → FR-008, NFR (~100 ms, brak utraty zmiany), FR-002/FR-003 (konsumenci)
- Tech-stack: `context/foundation/tech-stack.md` (Spring Boot 4.1, Maven, self-host)
- Lessons: `context/foundation/lessons.md` (Jackson 3; test-slice'y Boot 4 w modularnych pakietach)
- Baseline F-01: `server/src/main/java/com/example/clickupsimplifier/settings/SettingsStore.java`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Fundament trwałości (Postgres + Flyway + Testcontainers)

#### Automated

- [x] 1.1 Testy przechodzą wg toolchainu (JAVA_HOME21 + MAVEN_HOME9) w `server/`, z Dockerem — c54a229
- [x] 1.2 Test ładowania kontekstu na Postgresie z Testcontainers (datasource + Spring Data JDBC + Flyway wpięte) — c54a229
- [x] 1.3 Istniejący test kontekstu F-01 nadal przechodzi — c54a229

#### Manual

- [ ] 1.4 Aplikacja startuje przeciw lokalnie zainstalowanemu Postgresowi bez błędów

### Phase 2: Migracja schematu (V1)

#### Automated

- [ ] 2.1 `mvn test` (toolchain + Docker) przechodzi; Flyway aplikuje V1 czysto na Testcontainers
- [ ] 2.2 Test schematu potwierdza cztery tabele + kluczowe kolumny/ograniczenia

#### Manual

- [ ] 2.3 W lokalnym Postgresie widoczne tabele + indeksy po starcie

### Phase 3: Model domenowy + repozytoria + idempotentny upsert

#### Automated

- [ ] 3.1 `mvn test` (toolchain + Docker) przechodzi
- [ ] 3.2 Zapis→odczyt każdej encji zwraca te same dane
- [ ] 3.3 Idempotencja upsertu: ponowny upsert tego samego id aktualizuje, nie duplikuje
- [ ] 3.4 Milestone: null („no milestone") i self-ref task-milestone zapisują się/odczytują; `is_milestone` zachowane
- [ ] 3.5 Relacje FK + zapytania nawigacyjne (taski po liście, milestone'y po liście) poprawne
- [ ] 3.6 Trwałość: dane przeżywają ponowne odpytanie repo

#### Manual

- [ ] 3.7 (opcjonalnie) wiersze poprawne w lokalnym Postgresie po ręcznym seedzie
