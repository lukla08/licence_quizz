---
starter_id: vite-react
package_manager: npm
project_name: clickup-simplifier-web
hints:
  language_family: js
  team_size: solo
  deployment_target: self-host
  ci_provider: github-actions
  ci_default_flow: manual-promotion
  bootstrapper_confidence: verified
  path_taken: custom
  quality_override: true
  self_check_answers:
    typed: true
    from_official_starter: true
    conventions: true
    docs_current: true
    can_judge_agent: false
  has_auth: false
  has_payments: false
  has_realtime: false
  has_ai: false
  has_background_jobs: false
---

## Why this stack

Web client #1 of the swappable-frontend set: a thin browser UI over the local
Spring core, talking to it purely over REST. A single-page app is the right
shape — there is no own server to build (the core is the local Spring server),
so SSR/meta-framework machinery would only add a second runtime to run
on-device. Vite + React is the chosen SPA: typed (TypeScript), popular in
training data, well-documented, and `verified` for scaffolding. The cloud-first
recommended default for (web, js), 10x-astro-starter, was rejected as an
architectural mismatch — its Supabase database and Cloudflare-edge deploy are
irrelevant to a client that only renders UI and calls a local API. vite-react
fails the convention-based agent-friendly gate (minimal opinions on layout and
data-loading); the user chose it over the all-gates-clear Vue alternative, so
`quality_override` is true — conventions will be imposed via CLAUDE.md/AGENTS.md
later rather than inherited from the starter. Deployment is self-host (static
files served locally, e.g. from Spring's static resources). No feature flags are
set on the client — scheduled sync, token storage, and write-back all live in
the Spring core. CI mirrors the core: GitHub Actions with manual promotion. The
self-check came back four-of-five clean (one not-true), below the nudge
threshold.
