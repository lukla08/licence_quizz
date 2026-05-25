---
bootstrapped_at: 2026-05-25T20:08:00Z
starter_id: 10x-astro-starter
starter_name: "10x Astro Starter (Astro + Supabase + Cloudflare)"
project_name: licence-quizz
language_family: js
package_manager: npm
cwd_strategy: git-clone
bootstrapper_confidence: first-class
phase_3_status: ok
audit_command: "npm audit --json"
---

## Hand-off

```yaml
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
```

### Why this stack

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

## Pre-scaffold verification

| Signal      | Value                                             | Severity | Notes                                              |
| ----------- | ------------------------------------------------- | -------- | -------------------------------------------------- |
| npm package | not run                                           | n/a      | cmd_template starts with `git clone`; npm step skipped |
| GitHub repo | przeprogramowani/10x-astro-starter pushed 2026-05-17 | fresh    | from card.docs_url; within 3 months of 2026-05-25 |

## Scaffold log

**Resolved invocation**: `git clone https://github.com/przeprogramowani/10x-astro-starter .bootstrap-scaffold && cd .bootstrap-scaffold && npm install`
**Strategy**: git-clone (clone starter repo without keeping its git history)
**Exit code**: 0
**Files moved**: 18 (`.env.example`, `.github/`, `.husky/`, `.nvmrc`, `.prettierrc.json`, `.vscode/`, `astro.config.mjs`, `components.json`, `eslint.config.js`, `node_modules/`, `package.json`, `package-lock.json`, `public/`, `README.md`, `src/`, `supabase/`, `tsconfig.json`, `wrangler.jsonc`)
**Conflicts (.scaffold siblings)**: `CLAUDE.md` → `CLAUDE.md.scaffold`
**.gitignore handling**: append-merged — 4 new patterns added from scaffold (`.astro/`, `.dev.vars`, `.env.production`, `.wrangler/`); existing cwd patterns preserved in order
**.bootstrap-scaffold cleanup**: deleted

## Post-scaffold audit

**Tool**: `npm audit --json`
**Summary**: 0 CRITICAL, 1 HIGH, 9 MODERATE, 0 LOW
**Direct vs transitive**: 0/0/2/0 direct of total 0/1/9/0 (direct counts: CRITICAL 0, HIGH 0, MODERATE 2 — `@astrojs/check`, `wrangler`)

#### CRITICAL findings

None.

#### HIGH findings

- **devalue** v5.6.3–5.8.0 — "Svelte devalue: DoS via sparse array deserialization"
  Advisory: GHSA-77vg-94rm-hx3p | CVSS 7.5 (AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H)
  Type: transitive (via wrangler / astro build toolchain)
  Fix available: yes (`npm audit fix`)

#### MODERATE findings

1. **@astrojs/check** ≥0.9.3 (direct) — via `@astrojs/language-server` → `volar-service-yaml`. Fix: downgrade to `@astrojs/check@0.9.2` (semver major).
2. **@astrojs/language-server** ≥2.14.0 (transitive) — via `volar-service-yaml`. Fix available with major version bump on `@astrojs/check`.
3. **@cloudflare/vite-plugin** various ranges (transitive) — via `miniflare`, `wrangler`, `ws`. Fix available.
4. **miniflare** various ranges (transitive) — via `ws`. Fix available.
5. **volar-service-yaml** ≤0.0.70 (transitive) — via `yaml-language-server`. Fix available via `@astrojs/check@0.9.2` major bump.
6. **wrangler** various ranges (direct) — via `miniflare`. Fix available.
7. **ws** v8.0.0–8.20.0 (transitive) — "ws: Uninitialized memory disclosure". GHSA-58qx-3vcg-4xpx | CVSS 4.4. Fix available.
8. **yaml** v2.0.0–2.8.2 (transitive) — "Stack Overflow via deeply nested YAML collections". GHSA-48c2-rrv3-qjmp | CVSS 4.3. Fix available via `@astrojs/check@0.9.2` major bump.
9. **yaml-language-server** various ranges (transitive) — via `yaml`. Fix available via `@astrojs/check@0.9.2` major bump.

#### LOW / INFO findings

None.

## Hints recorded but not acted on

| Hint                    | Value               |
| ----------------------- | ------------------- |
| bootstrapper_confidence | first-class         |
| quality_override        | false               |
| path_taken              | standard            |
| self_check_answers      | null                |
| team_size               | solo                |
| deployment_target       | cloudflare-pages    |
| ci_provider             | github-actions      |
| ci_default_flow         | auto-deploy-on-merge |
| has_auth                | true                |
| has_payments            | false               |
| has_realtime            | false               |
| has_ai                  | false               |
| has_background_jobs     | false               |

## Next steps

Next: a future skill will set up agent context (CLAUDE.md, AGENTS.md). For now, your project is scaffolded and verified — happy hacking.

Useful manual steps in the meantime:
- Review `CLAUDE.md.scaffold` (the starter's AI rules file) and decide whether to merge its content into your existing `CLAUDE.md`.
- Address audit findings per your project's risk tolerance — the full breakdown is in this log. The 1 HIGH finding (`devalue`) is in transitive dev-tooling; run `npm audit fix` to address the fixable ones.
- Copy `.env.example` to `.env` and fill in your Supabase credentials to start local development.
- Run `npm run dev` to start the dev server and verify the scaffold works.
