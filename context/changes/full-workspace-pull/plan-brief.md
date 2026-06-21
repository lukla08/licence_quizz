# Pełny Pull Workspace do Lokalnej Kopii (S-01) — Plan Brief

> Full plan: `context/changes/full-workspace-pull/plan.md`

## What & Why

S-01 dostarcza pierwszy realny pull danych ClickUp do lokalnej kopii. Bez niego
wszystkie slice'y konsumujące lokalną kopię (nawigacja S-03, tworzenie S-04, panel S-07)
nie mają danych. To odblokowanie dla całej gałęzi Stream A (rdzeń sync).

## Starting Point

F-01 daje uwierzytelniony `RestClient` + token na dysku. F-02 daje schemat V1
(`space/folder/list/task`) z repozytoriami i idempotentnym upsertem. Brakuje klienta
API do pobierania danych workspace, warstwy sync i obsługi subtasków (brak `parent_id`).

## Desired End State

`POST /api/sync/full-pull` → 202; pull idzie w tle i kończy się stanem `COMPLETED`
dostępnym przez `GET /api/sync/status`. Lokalny Postgres zawiera wierne odwzorowanie
pierwszego workspace'u ClickUp — spaces, foldery, listy, taski i subtaski. Ponowny pull
jest replace-all: elementy usunięte z ClickUp znikają z DB. `sync_set.last_synced_at`
per zestaw persystuje timestamp sukcesu (kontrakt pod S-02 i S-07).

## Key Decisions Made

| Decyzja | Wybór | Dlaczego | Source |
|---|---|---|---|
| Model async/sync | @Async fire-and-forget → 202 | Pull może trwać minuty; blokowanie HTTP nieakceptowalne | Plan |
| Semantyka replace-all | Diff ID + delete stale + upsert | Lokalna kopia wierna ClickUp po każdym pull'u | Plan |
| Subtaski | V2 Flyway + `parent_id NULL` + dziedziczony `list_id` | Subtaski mają `parent` zamiast `list_id` w API; schemat V1 niekompatybilny | Plan |
| Wiele teamów | Tylko `teams[0]` | Uproszczenie MVP; single-user narzędzie lokalne | Plan |
| Rate limit 429 | Exponential backoff 1s/2s/4s, max 3 próby | Ochrona przed throttlingiem bez blokowania na stałe | Plan |
| Granica transakcji | Osobna `@Transactional` per zestaw sync | Krótsze locki; słowniki dostępne po fazie 1 nawet gdy taski jeszcze trwają | Plan |
| FK przy replace-all | CASCADE w V2 (nie ręczna kolejność) | DELETE space kaskaduje do folder→list→task; prostszy kod serwisu | Plan |
| Parametr `since` | `since: Instant?` już w S-01 | S-02 dostaje gotowy kontrakt; null = pełny pull | Plan |
| `last_synced_at` | Tabela `sync_set` (V2 Flyway) | Persystowane przez restarty; S-07 rozszerzy o `last_error`, `frequency` | Plan |
| Testy HTTP | `MockRestServiceServer` (wzorzec F-01) | Spójność; zero nowych zależności | Plan |
| Zakres tasków | `include_closed=true`, `subtasks=true` | Pełna kopia: wszystkie statusy, bezpośrednie subtaski | Plan |
| `milestone_id` po pull | Zawsze `null` | Lokalny koncept; S-03/S-04 ustawiają; `task.milestone` (bool) to flaga z ClickUp | Plan |

## Scope

**In scope:**
- `POST /api/sync/full-pull` + `GET /api/sync/status`
- ClickupWorkspaceClient (6 metod, retry, paginacja)
- WorkspaceSyncService (orchestracja, replace-all, `since` param)
- Flyway V2 (`parent_id`, CASCADE FK, `sync_set` table)
- Subtaski 1 poziomu (parent = task z listy)

**Out of scope:**
- Sync przyrostowy (S-02)
- Panel sync z historią błędów i częstotliwością (S-07)
- Wiele teamów / wybór workspace'u
- Głębokie zagnieżdżenie subtasków (> 1 poziom)
- Lokalne edycje / zapis zwrotny do ClickUp (S-04/S-05)

## Architecture / Approach

```
SyncController
  POST /api/sync/full-pull ──► WorkspaceSyncService.triggerPull(since=null)  [@Async]
  GET  /api/sync/status    ◄── currentStatus (AtomicRef) + syncSetRepo.findAll()

WorkspaceSyncService
  ├─ syncDictionaries(token, teamId, since)  [@Transactional]
  │    ClickupWorkspaceClient: getSpaces → getFolders → getLists
  │    replace-all: deleteByIdNotIn (CASCADE czyści dzieci) → upsert root→leaf
  │    syncSetRepo.updateLastSyncedAt("dictionaries")
  └─ syncTasks(token, teamId, since)  [@Transactional]
       ClickupWorkspaceClient: getTasks per list (paginacja + since)
       flatten subtaski (dziedzicz list_id rodzica)
       replace-all: deleteStaleByListId → upsert top-level first, subtaski second
       syncSetRepo.updateLastSyncedAt("tasks")

ClickupWorkspaceClient  (wzorzec F-01 RestClient + withRetry na 429)
  getTeams / getSpaces / getFolders / getFolderlessLists / getListsByFolder / getTasks
```

Nowe pakiety: `clickup/workspace/` (klient + DTOs), `sync/` (serwis + kontroler + status).

## Phases at a Glance

| Faza | Co dostarcza | Główne ryzyko |
|---|---|---|
| 1. Schema V2 + domain | `parent_id`, CASCADE FK, `sync_set` table; `Task` + `SyncSet` rekordy | Przebudowa FK bez naruszenia V1 danych |
| 2. Workspace API client | `ClickupWorkspaceClient` (6 metod), DTOs, retry 429, paginacja | Mapowanie zagnieżdżonych JSON pól ClickUp (status, folder.hidden, space.id) |
| 3. WorkspaceSyncService | Orchestracja @Async, replace-all, `since` param, unit testy | Kolejność upsert subtasków; rollback przy błędzie API w środku pull'u |
| 4. REST + smoke-test | Endpoints, status DTO, integracyjny E2E test | @Async AOP proxy (nie self-call); stabilność testu async |

**Prerequisites:** F-01 ✅, F-02 ✅ — oba gotowe.
**Estimated effort:** ~3-4 sesje, 4 fazy.

## Open Risks & Assumptions

- **Duży workspace**: przy > 500 listach pull może trwać > 5 min (rate limit 100 req/min).
  Akceptowalne dla jednorazowej operacji MVP; S-02 (przyrost) rozwiąże świeżość.
- **ClickUp API zmiany**: plan zakłada stabilność ClickUp v2 API (pola `milestone`,
  `parent`, `last_page`). Weryfikacja ręczna w Phase 2 potwierdzi.
- **Subtaski > 1 poziomu**: ignorowane; jeśli użytkownik ma głęboko zagnieżdżone subtaski,
  nie pojawią się w lokalnej kopii.

## Success Criteria (Summary)

- `POST /api/sync/full-pull` → 202; `GET /api/sync/status` przechodzi przez `RUNNING` → `COMPLETED`
- Lokalny Postgres zawiera spaces/folders/lists/tasks wiernie odwzorowujące ClickUp
- Ponowny pull jest idempotentny i replace-all (stale dane usunięte)
