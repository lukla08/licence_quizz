# Minimalna lokalna warstwa trwałości (F-02) — Plan Brief

> Full plan: `context/changes/local-copy-persistence/plan.md`

## What & Why

Pierwsza warstwa trwałości rdzenia: lokalna kopia workspace ClickUp w PostgreSQL.
To fundament F-02 — minimalny magazyn, do którego pierwszy pull (S-01) zapisze, a
nawigacja klawiaturą (S-03) odczyta. Bez niego rdzeń sync nie ma gdzie składować danych.

## Starting Point

Po F-01 `server/` (Spring Boot 4.1, Java 21) ma klienta ClickUp i magazyn tokenu
(`SettingsStore`, JSON na dysku), ale **zero trwałości danych domenowych** — brak DB,
Spring Data, migracji. To pierwszy realny magazyn.

## Desired End State

Aplikacja łączy się z lokalnym PostgreSQL; Flyway tworzy tabele Space/Folder/List/Task;
repozytoria Spring Data JDBC zapisują i czytają hierarchię z **idempotentnym upsertem**
po id ClickUp (ponowny pull aktualizuje, nie duplikuje). Task niesie `is_milestone` +
nullowalny `milestone_id` (self-ref), więc model milestone→task i „no milestone" są reprezentowalne.

## Key Decisions Made

| Decision            | Choice                                  | Why (1 sentence)                                                        | Source |
| ------------------- | --------------------------------------- | ---------------------------------------------------------------------- | ------ |
| Silnik magazynu     | PostgreSQL (lokalnie zainstalowany)     | Wybór użytkownika; znany, robustny, z zapasem na NFR.                   | Plan   |
| Dostęp do danych    | Spring Data JDBC                        | Lekki, jawny, bez magii lazy-load; pasuje do prostego schematu.        | Plan   |
| Schemat             | Flyway (migracje wersjonowane)          | Trwała ewolucja schematu (S-02+), reprodukowalne.                       | Plan   |
| Encje               | Space → Folder → List → Task            | Pełna hierarchia ClickUp; future-proof na kontekst space-level.        | Plan   |
| Tożsamość           | Id ClickUp jako PK + upsert             | Idempotentny re-pull i sync przyrostowy (S-02) wychodzą naturalnie.    | Plan   |
| Milestone           | Nullowalny ref + flaga na Task          | Milestone to task w ClickUp; wspiera FR-008 i „no milestone" (NULL).   | Plan   |
| Weryfikacja         | Testy repo na Testcontainers Postgres   | Dowodzi trwałości/upsertu na realnym silniku, bez pulla.               | Plan   |

## Scope

**In scope:** Postgres + Flyway + Spring Data JDBC; tabele Space/Folder/List/Task + pola
milestone; idempotentny upsert; zapytania-fundamenty (taski po liście, milestone'y, foldery/listy
po space); testy integracyjne na Testcontainers.

**Out of scope:** pull / wywołania ClickUp (S-01); pełny schemat domeny; tworzenie/edycja z UI
(S-03/S-04/S-06); sync przyrostowy / status zestawów / kadencje (S-02/S-07); szyfrowanie magazynu;
warstwa prezentacji FR-008 (S-03).

## Architecture / Approach

Każda encja to własny agregat Spring Data JDBC (osobne repo); relacje to kolumny-referencje po id
(nie embedded collections), bo upsert idzie per-encja. Idempotencja przez jawne
`INSERT ... ON CONFLICT (id) DO UPDATE` (bo `save()` nie upsertuje wierszy z zewnętrznym PK).
Schemat zarządza Flyway (V1). Runtime łączy się z lokalnym Postgresem; testy z efemerycznym
Postgresem (Testcontainers, `@ServiceConnection`).

## Phases at a Glance

| Phase                                     | What it delivers                                        | Key risk                                              |
| ----------------------------------------- | ------------------------------------------------------- | ---------------------------------------------------- |
| 1. Fundament trwałości                    | Zależności + datasource + Flyway + harness Testcontainers | Wpięcie autoconfig + Docker dostępny dla testów      |
| 2. Migracja schematu (V1)                 | Tabele Space/Folder/List/Task + indeksy FK              | Poprawność FK/self-ref i pokrycie indeksami pod NFR  |
| 3. Model + repozytoria + upsert           | Rekordy + repo + idempotentny upsert + testy            | Upsert z zewnętrznym PK (ON CONFLICT, nie `save()`)  |

**Prerequisites:** Lokalnie zainstalowany PostgreSQL (runtime) oraz **Docker** dla testów
(Testcontainers). Toolchain F-01: `JAVA_HOME21` + `MAVEN_HOME9`.
**Estimated effort:** ~1–2 sesje, 3 fazy.

## Open Risks & Assumptions

- **Docker dla testów** — `mvn test` wymaga teraz działającego Dockera (Testcontainers); nowy warunek względem F-01.
- **Postgres jako runtime-dependency** — aplikacja nie wstanie bez osiągalnego Postgresa (odejście od „local-first zero-install"; świadomy wybór użytkownika).
- **Upsert z zewnętrznym PK** — `ON CONFLICT` zamiast `save()`; pomyłka → duplikaty/PK violation.
- **`@DataJdbcTest` pod Boot 4** — adnotacja w modularnym pakiecie (lessons.md); zła ścieżka = błąd kompilacji.
- Fixtures są syntetyczne; realne kształty ClickUp zweryfikuje dopiero S-01.

## Success Criteria (Summary)

- Aplikacja łączy się z Postgresem, Flyway tworzy schemat; tabele + indeksy obecne.
- Zapis-odczyt hierarchii działa; upsert idempotentny (ponowny zapis aktualizuje, nie duplikuje).
- Milestone null/self-ref i relacje FK reprezentowalne i odpytywalne; testy na Testcontainers zielone.
