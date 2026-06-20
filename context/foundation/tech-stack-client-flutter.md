---
starter_id: flutter
package_manager: pub
project_name: clickup-simplifier-flutter
hints:
  language_family: dart
  team_size: solo
  deployment_target: self-host
  ci_provider: github-actions
  ci_default_flow: manual-promotion
  bootstrapper_confidence: verified
  path_taken: standard
  quality_override: false
  self_check_answers: null
  has_auth: false
  has_payments: false
  has_realtime: false
  has_ai: false
  has_background_jobs: false
---

## Why this stack

Flutter client of the swappable-frontend set: one Dart codebase rendering the UI
across desktop and web, talking to the local Spring core over REST. This is the
recommended default for the (desktop/mobile, dart) cell and clears all four
agent-friendly gates with `verified` scaffolding confidence, so the standard
path was taken — no custom walk needed. Flutter is the strongest "multi-client
from one codebase" story in the registry, which is exactly the architectural
intent here. Deployment is self-host: local desktop/web builds, not app-store
distribution, matching the on-device model. No feature flags are set on the
client — scheduled sync, token storage, and write-back all live in the Spring
core; the client only renders and calls the API. CI mirrors the core: GitHub
Actions with manual artifact promotion, appropriate for a locally-built app with
no remote deploy target. Standard path means no five-point self-check was run.
