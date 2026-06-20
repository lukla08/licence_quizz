---
change_id: clickup-token-and-connectivity
title: "ClickUp token + uwierzytelniona łączność z API"
roadmap_ref: F-01
status: implemented
created: 2026-06-20
updated: 2026-06-20
prd_refs:
  - FR-001
  - Access Control
---

# Change: ClickUp token + uwierzytelniona łączność z API (F-01)

Fundament F-01 z `context/foundation/roadmap.md`. Daje rdzeniowi Spring sposób na
zapisanie osobistego tokenu API ClickUp i wykonanie uwierzytelnionego wywołania,
które potwierdza, że token działa. Najmniejszy enabler odblokowujący **S-01**
(pełny pull workspace).

- **Outcome:** rdzeń przechowuje token (plik ustawień) i potwierdza go przez
  `GET /api/v2/user`, zwracając tożsamość lub strukturalny błąd.
- **Plan:** `plan.md` · **Brief:** `plan-brief.md`
- **Unlocks:** S-01 (`full-workspace-pull`)
