---
change_id: local-copy-persistence
title: Minimalna lokalna warstwa trwałości na kopię workspace
status: impl_reviewed
created: 2026-06-20
updated: 2026-06-21
archived_at: null
---

## Notes

Fundament **F-02** z `context/foundation/roadmap.md`. Minimalny lokalny magazyn na kopię
workspace (słowniki, milestone'y, zadania) odwzorowujący dwupoziomowy model
milestone→task — **tylko encje potrzebne pierwszemu pullowi**, nie cały schemat z góry.

- **PRD refs:** FR-008, NFR (nawigacja ~100 ms na całym workspace), NFR (brak utraty lokalnej zmiany).
- **Unlocks:** S-01 (`full-workspace-pull` — pull zapisuje do magazynu), S-03 (odczyt do nawigacji).
- **Prerequisites:** — (ready, brak blokerów; równolegle do F-01/F-03).
- **Baseline:** Data: absent — brak sterownika DB, ORM i migracji w `server/pom.xml`. To pierwsza warstwa trwałości.
- **Guardrail:** warstwa danych to bramka NFR (~100 ms) i serce produktu; trzymać minimalnie — kontrakt pod pierwszy pull, który S-01 od razu ćwiczy realnym pullem.
