---
change_id: data-isolation-and-roles
title: "Izolacja danych i role dostępu (F-01)"
roadmap_id: F-01
status: implementing
created: 2026-06-01
updated: 2026-06-01
---

## Identity

Fundament danych i kontroli dostępu dla LicenceQuizz (roadmap F-01). Dostarcza
minimalny schemat (kategorie, pytania, opcje odpowiedzi, tagi trudności, sesje,
kolejka+odpowiedzi), izolację RLS per-user, flagę administratora w bazie oraz
seed — kontrakt, na którym budują S-01 (pierwsza grywalna sesja) i S-04 (panel CMS).

- Roadmap: `context/foundation/roadmap.md` → F-01
- PRD: `context/foundation/prd.md` → FR-001, FR-002, Access Control, guardrail izolacji
