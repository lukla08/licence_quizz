---
starter_id: spring
package_manager: maven
project_name: licence-quizz-api
hints:
  language_family: java
  team_size: solo
  deployment_target: render
  ci_provider: github-actions
  ci_default_flow: auto-deploy-on-merge
  bootstrapper_confidence: verified
  path_taken: standard
  quality_override: false
  self_check_answers: null
  has_auth: true
  has_payments: false
  has_realtime: false
  has_ai: false
  has_background_jobs: false
---

## Why this stack

This hand-off covers only the shared backend API — the foundation all five client variants depend on for email/password auth, per-account data isolation, cross-device sync, the CMS question store, and video metadata. The project is deliberately a comparative multi-client build (Flutter, two Android-native variants, JavaFX, Java Swing), so each client gets its own stack decision later; this run scopes the backend alone. Java/Spring Boot was chosen explicitly: it reuses the JVM skills already exercised by three of the five clients, and it clears all four agent-friendly gates (typed, convention-based, popular within Java training data, well-documented) with verified scaffolder support. Scale is small and the timeline is 7 after-hours weeks, so a battle-tested batteries-included backend beats anything exotic. Render is the deployment target — a managed host whose push-to-deploy model fits a solo builder; auto-deploy-on-merge on GitHub Actions matches that. Auth is the only stack-forcing feature flag in scope. Open item from the PRD: video hosting (external store/CDN vs. database) is unresolved and is not a Render/Spring concern to settle here.
