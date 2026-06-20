---
bootstrapped_at: 2026-06-20T15:38:00Z
starter_id: vite-react
starter_name: Vite + React
project_name: clickup-simplifier-web
language_family: js
package_manager: npm
cwd_strategy: subdir-then-move (adapted — scaffolded into dedicated clients/web/)
bootstrapper_confidence: verified
phase_3_status: ok
audit_command: npm audit --json
---

## Hand-off

```yaml
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
```

> Web client #1 of the swappable-frontend set: a thin browser UI over the local
> Spring core, talking to it purely over REST. A single-page app is the right
> shape — there is no own server to build (the core is the local Spring server),
> so SSR/meta-framework machinery would only add a second runtime to run
> on-device. Vite + React is the chosen SPA: typed (TypeScript), popular in
> training data, well-documented, and `verified` for scaffolding. The cloud-first
> recommended default for (web, js), 10x-astro-starter, was rejected as an
> architectural mismatch. vite-react fails the convention-based agent-friendly
> gate; the user chose it over the all-gates-clear Vue alternative, so
> `quality_override` is true. Deployment is self-host. CI mirrors the core.

## Pre-scaffold verification

| Signal      | Value                                  | Severity | Notes                                  |
| ----------- | -------------------------------------- | -------- | -------------------------------------- |
| npm package | create-vite v9.0.7 published 2026-05-11 | fresh    | resolved from cmd_template (npm create vite) |
| GitHub repo | not run                                | n/a      | card docs_url (vitejs.dev) is not a github.com repo |

## Scaffold log

**Resolved invocation**: `npm create vite@latest web -- --template react-ts` (run inside `clients/`), followed by `npm install` in `clients/web/`
**Strategy**: subdir-then-move, adapted — scaffolded into a dedicated component directory `clients/web/` rather than the repo root, because the repo root holds shared `context/` and the Spring core was relocated to `server/`. No move-up into the repo root was performed; the client is intentionally isolated from the server.
**Exit code**: 0 (scaffold), 0 (install)
**Files written**: full vite-react TypeScript template (`package.json`, `index.html`, `vite.config.ts`, `tsconfig*.json`, `eslint.config.js`, `src/`, `public/`, `README.md`, `.gitignore`)
**Conflicts (.scaffold siblings)**: none (dedicated empty subdirectory)
**.gitignore handling**: the scaffold's own `clients/web/.gitignore` was kept in place (component-local); the repo-root `.gitignore` was not touched
**.bootstrap-scaffold cleanup**: n/a (no temp directory used)

Layout note: this run also relocated the previously-bootstrapped Spring core from the repo root into `server/`, at the user's request, to keep the shared core and the swappable clients in separate directories.

## Post-scaffold audit

**Tool**: `npm audit --json`
**Summary**: 0 CRITICAL, 0 HIGH, 0 MODERATE, 0 LOW
**Direct vs transitive**: 0 of 153 audited packages flagged — clean tree
**Notes**: `npm install` reported `found 0 vulnerabilities`; the explicit audit confirmed an empty `vulnerabilities` object.

## Hints recorded but not acted on

| Hint                    | Value           |
| ----------------------- | --------------- |
| bootstrapper_confidence | verified        |
| quality_override        | true            |
| path_taken              | custom          |
| self_check_answers      | 4/5 true (can_judge_agent: false) |
| team_size               | solo            |
| deployment_target       | self-host       |
| ci_provider             | github-actions  |
| ci_default_flow         | manual-promotion |
| has_auth                | false           |
| has_payments            | false           |
| has_realtime            | false           |
| has_ai                  | false           |
| has_background_jobs     | false           |

Note: `quality_override: true` — vite-react fails the convention-based agent-friendly gate. v1 surfaces this but applies no compensation; impose layout/data-loading conventions via CLAUDE.md/AGENTS.md in a later step.

## Next steps

Next: a future skill will set up agent context (CLAUDE.md, AGENTS.md). For now, the web client is scaffolded and verified — happy hacking.

Useful manual steps in the meantime:
- `cd clients/web && npm run dev` to start the dev server.
- Wire the client to the local Spring core's REST API (the core lives in `server/`).
- Decide how the SPA is served on-device (e.g., bundled into Spring's `src/main/resources/static`, per the self-host deployment target).
- Remaining clients per `context/foundation/roadmap.md`: Flutter (`tech-stack-client-flutter.md`) and Java desktop (`tech-stack-client-desktop-java.md`).
