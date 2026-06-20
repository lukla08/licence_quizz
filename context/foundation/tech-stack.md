---
starter_id: spring
package_manager: maven
project_name: clickup-simplifier
hints:
  language_family: java
  team_size: solo
  deployment_target: self-host
  ci_provider: github-actions
  ci_default_flow: manual-promotion
  bootstrapper_confidence: verified
  path_taken: custom
  quality_override: false
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
  has_background_jobs: true
---

## Why this stack

ClickUp Simplifier is a single-user, local-first tool: a shared core (local
copy, sync engine, domain model) with swappable frontends and no hosted server —
the only remote is ClickUp's API via a personal token. This hand-off scaffolds
that core, which the user fixed as Java/Spring; clients (native web, Flutter,
native Java desktop) are deferred to later, separate bootstraps. Spring is also
the registry's recommended default for the (backend/API, Java) cell and clears
all four agent-friendly gates with `verified` bootstrapper confidence, so
scaffolding will be smooth. It runs locally on-device (self-host), not in the
cloud, which is why the cloud-oriented JS web default was rejected as an
architectural mismatch. Background jobs is the one feature flag set — the named
sync sets run on recurring per-set cadences (FR-003, FR-019); auth is false
(single user, no login; the API token is secret storage, not app auth), and
payments, realtime, and AI are out of scope. CI on GitHub Actions with manual
artifact promotion fits a local desktop app with no remote deploy target. The
five-point self-check came back clean across all points, so no Socratic nudge
fired.
