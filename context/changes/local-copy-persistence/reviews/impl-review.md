<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: Minimalna lokalna warstwa trwałości (F-02)

- **Plan**: context/changes/local-copy-persistence/plan.md
- **Scope**: Wszystkie fazy (1–3)
- **Date**: 2026-06-21
- **Verdict**: NEEDS ATTENTION
- **Findings**: 0 critical  2 warnings  5 observations

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | PASS |
| Scope Discipline | WARNING |
| Safety & Quality | WARNING |
| Architecture | PASS |
| Pattern Consistency | PASS |
| Success Criteria | PASS |

## Findings

### F1 — Credentials plaintext w pliku śledzonym przez git

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Safety & Quality
- **Location**: server/src/main/resources/application.properties:12-13
- **Detail**: spring.datasource.username=clickup / password=clickup plaintext w application.properties śledzonym przez git. Ryzyko sieciowe zerowe (local tool), ale hasło wchodzi w historię commitów.
- **Fix A ⭐ Recommended**: Przenieść do application-local.properties + .gitignore
  - Strength: Standard Spring Boot; czysta historia od teraz.
  - Tradeoff: Stare commity (c54a229–ff5f423) i tak mają te dane.
  - Confidence: HIGH
  - Blind spot: Jeśli repo ma być publiczne — potrzebny BFG dla historii.
- **Fix B**: Przekazywać przez zmienne środowiskowe (SPRING_DATASOURCE_* env)
  - Strength: Zero nowych plików.
  - Tradeoff: Domyślne wartości nadal w repo.
  - Confidence: MED
  - Blind spot: Brak .gitignore nie chroni przed przypadkowym commitem silniejszych credentiali.
- **Decision**: FIXED via Fix A — application-local.properties + .gitignore; credentials usunięte z application.properties

### F2 — Brak @Transactional wrappera dla batch-upsertów

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Safety & Quality
- **Location**: SpaceRepository.java, FolderRepository.java, WorkspaceListRepository.java, TaskRepository.java
- **Detail**: @Modifying bez @Transactional — każdy upsert to osobna transakcja. Przy crash w trakcie pull'a (S-01) workspace w DB będzie w stanie częściowym bez rollbacku. Nie aktywny teraz (brak warstwy sync), ale musi być zaadresowany przed S-01.
- **Fix**: Zanotować jako wymaganie dla S-01: warstwa serwisowa sync musi owijać cały import workspace w @Transactional.
  - Strength: Repozytoria świadomie cienkie; @Transactional należy do warstwy serwisowej.
  - Tradeoff: Nic do naprawienia teraz — tylko wpis w kontekście S-01.
  - Confidence: HIGH
  - Blind spot: Nie sprawdzono czy plan S-01 już to uwzględnia.
- **Decision**: FIXED — wymaganie @Transactional zapisane w roadmap.md przy S-01

### F3 — findByFolderId poza kontraktem planu

- **Severity**: OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Scope Discipline
- **Location**: WorkspaceListRepository.java:18
- **Detail**: Plan wymieniał findBySpaceId; implementacja dorzuciła findByFolderId. Poprawna domenowo, nie narusza guardrailsów.
- **Fix**: Zaakceptować lub usunąć — prawdopodobnie przyda się S-03.
- **Decision**: SKIPPED — findByFolderId zaakceptowana; przyda się S-03

### F4 — Brak paginacji na findBy* w TaskRepository

- **Severity**: OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Safety & Quality
- **Location**: TaskRepository.java:21-23
- **Detail**: findByListId / findByMilestoneId zwracają List<Task> bez paginacji. Nie problem w MVP desktop use-case.
- **Fix**: Do zaadresowania przy warstwie serwisowej/nawigacyjnej (S-03).
- **Decision**: SKIPPED — do zaadresowania przy warstwie serwisowej S-03

### F5 — Brak ON DELETE CASCADE — zaplanować przed S-01

- **Severity**: OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Safety & Quality
- **Location**: V1__create_workspace_copy.sql:11,21,26,31
- **Detail**: FK bez ON DELETE. Przy "replace all" sync (S-01/S-02) usunięcie space bez kolejności leaf→root spowoduje FK violation.
- **Fix**: Zanotować jako wymaganie dla S-01/S-02 plan.
- **Decision**: FIXED — wymaganie ON DELETE CASCADE zapisane w roadmap.md przy S-01

### F6 — clean() w testach: 4×deleteAll zamiast TRUNCATE CASCADE

- **Severity**: OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Pattern Consistency
- **Location**: RepositoryIntegrationTest.java:28-31
- **Detail**: Cztery deleteAll() w kolejności FK. Poprawne, ale kruche i wolniejsze przy rozroście zestawu testów.
- **Fix**: Przy rozroście zamienić na TRUNCATE ... CASCADE lub @Transactional rollback.
- **Decision**: SKIPPED — tylko lokalne testy integracyjne, efemeryczny kontener; OK dla MVP
