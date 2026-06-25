---
change_id: first-client-shell
title: "Monolit JavaFX: moduły core+ui, usunięcie REST, powłoka klienta"
roadmap_ref: F-03
status: implementing
created: 2026-06-25
updated: 2026-06-25
prd_refs:
  - FR-007
  - US-01
---

# Change: Monolit JavaFX — moduły core+ui + powłoka klienta (F-03)

Fundament F-03 z `context/foundation/roadmap.md`, po rewizji architektury z
2026-06-25 (monolit desktopowy pod JavaFX zamiast multi-client / klient-serwer).
Przebudowuje repo w dwumodułowy monolit Maven (`core` + `ui`), usuwa warstwę REST
(`@RestController` + Tomcat/webmvc), zachowuje mapowanie kontrolerów jako warstwę
aplikacyjną wołaną in-process, i stawia powłokę JavaFX startującą kontekst Spring,
która czyta łączność z ClickUp wprost z `core`.

- **Outcome:** `ui` (JavaFX) startuje kontekst Spring i woła `core` in-process
  (bez kontraktu sieciowego); powłoka pokazuje stan łączności + ma bazowy,
  spójny szkielet nawigacji klawiaturą.
- **Plan:** `plan.md` · **Brief:** `plan-brief.md`
- **Unlocks:** S-03, S-04 (gwiazda), S-05, S-06, S-07, S-08, S-09 — cała ścieżka UI.
- **Uwaga:** refaktor dotyka kodu z zarchiwizowanych F-01/F-02/S-01, ale jedzie
  pod TYM change-id; `context/archive/` pozostaje nietknięte.
