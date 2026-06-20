---
starter_id: javafx
package_manager: maven
project_name: clickup-simplifier-desktop-java
hints:
  language_family: java
  team_size: solo
  deployment_target: self-host
  ci_provider: github-actions
  ci_default_flow: manual-promotion
  bootstrapper_confidence: best-effort
  path_taken: custom
  quality_override: true
  self_check_answers:
    typed: true
    from_official_starter: true
    conventions: true
    docs_current: true
    can_judge_agent: true
  has_auth: false
  has_payments: false
  has_realtime: false
  has_ai: false
  has_background_jobs: false
---

## Why this stack

Native Java desktop client of the swappable-frontend set, rendering UI on the
JVM and talking to the local Spring core over REST. The registry had no
(desktop, java) starter, so a JavaFX card (OpenJFX Maven archetype) was added to
the registry for this pick; it is `best-effort` for scaffolding — registered
with a valid CLI but not run end-to-end, so expect manual touch-up (JavaFX
module path, jpackage for installers). JavaFX is typed (Java), convention-based
(FXML + controllers), and well-documented, but fails the popular-in-training
gate — its corpus is smaller than web frameworks and agents stumble more on
FXML/binding specifics — so `quality_override` is true; the gap is to be
compensated via CLAUDE.md/AGENTS.md guidance later. The user accepted this
knowingly after the registry-gap was surfaced and after considering that Flutter
already covers desktop. Maven keeps tooling aligned with the Spring core.
Deployment is self-host (local build/installer), matching the on-device model.
No feature flags are set on the client — sync, token storage, and write-back
live in the Spring core. CI mirrors the rest: GitHub Actions with manual
promotion. The five-point self-check came back clean across all points.
