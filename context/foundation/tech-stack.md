---
starter_id: 10x-astro-starter
package_manager: npm
project_name: licence-quizz
hints:
  language_family: js
  team_size: solo
  deployment_target: cloudflare-pages
  ci_provider: github-actions
  ci_default_flow: auto-deploy-on-merge
  bootstrapper_confidence: first-class
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

LicenceQuizz is a solo, after-hours, web-only MVP targeting a small private
user group (driver's licence candidates plus family/friends) with a 3-week
timeline. The only technology-forcing feature from the PRD is auth (FR-001/002
— e-mail + password, two-role model: user and admin). The `10x-astro-starter`
(Astro 6 + Supabase + Cloudflare) is the recommended default for `(web-app, js)`
and passes all four agent-friendly gates: TypeScript project-wide with Zod
schemas (typed), strong file-based conventions (convention-based), well-covered
in training data (popular), and current link-able docs (well-documented).
Supabase covers auth and PostgreSQL with no extra wiring; the admin panel
(FR-010) can be built on a lightweight Astro route backed by Supabase's table
API. Cloudflare Pages is the starter's default deploy target; GitHub Actions
with auto-deploy-on-merge is the standard CI/CD shape. Bootstrapper confidence
is `first-class` — the CLI is registered and the stack is well-known, though
not yet battle-tested end-to-end in the bootstrapper.
