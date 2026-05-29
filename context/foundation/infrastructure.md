---
project: licence-quizz
researched_at: 2026-05-29
recommended_platform: Cloudflare Workers
runner_up: Vercel
context_type: mvp
tech_stack:
  language: TypeScript
  framework: Astro 6 SSR
  runtime: Cloudflare Workers (via @astrojs/cloudflare adapter)
  database: Supabase (external)
---

## Recommendation

**Deploy on Cloudflare Workers.**

The tech stack is already wired for Cloudflare — the `@astrojs/cloudflare` adapter targets the Workers V8 isolate runtime, Supabase connects over HTTP/REST (no native TCP needed), and the developer has confirmed hands-on Cloudflare familiarity. Cloudflare scores 5/5 on all agent-friendly criteria: `wrangler` CLI covers every operation, docs are served as markdown with a published `llms.txt`, 13 managed MCP servers are GA for agent-driven operations, and the deploy/rollback surface is fully scriptable. No adapter swap, no Dockerfile, no extra cost at MVP traffic levels.

## Platform Comparison

| Platform | CLI-first | Managed/Serverless | Agent-readable docs | Stable deploy API | MCP / Integration | Total |
|---|---|---|---|---|---|---|
| **Cloudflare Workers** | Pass | Pass | Pass | Pass | Pass | **5/5** |
| Vercel | Pass | Pass | Pass | Pass | Pass | **5/5** |
| Netlify | Pass | Pass | Pass | Partial | Pass | **4.5/5** |
| Fly.io | Pass | Partial | Partial | Pass | Pass | **4/5** |
| Railway | Pass | Pass | Partial | Pass | Fail | **3.5/5** |
| Render | Partial | Pass | Fail | Partial | Fail | **2/5** |

Hard filter applied: no platforms dropped — persistent connections not required (Q1: No), and all platforms support TypeScript/Node.js.

Soft weights applied: Cloudflare familiarity (Q3) breaks the tie with Vercel on the final ranking; single-region preference (Q4) has no negative impact on Cloudflare; external providers acceptable (Q5) means co-location is irrelevant.

### Shortlisted Platforms

#### 1. Cloudflare Workers (Recommended)

The only platform that requires zero adapter change — the project uses `@astrojs/cloudflare` and `wrangler deploy` already. Docs ship with `llms.txt` (GA since Feb 2026) plus per-page `index.md` suffixes and `Accept: text/markdown` header support. The 13 managed remote MCP servers (GA March 2025) cover Workers, Pages, KV, R2, D1, DNS, and observability. Free tier handles 100k requests/day with no credit card required. Familiarity confirmed.

#### 2. Vercel

Scores identically to Cloudflare on criteria (5/5). The `@astrojs/vercel` adapter is maintained by Astro core and well-documented. The Vercel MCP server is GA (April 2026, OAuth-backed, supports Claude Code). The gap vs. Cloudflare: requires an adapter swap from `@astrojs/cloudflare` to `@astrojs/vercel`, the Hobby free plan limits runtime log retention to 1 hour (makes debugging harder), and commercial use is prohibited on Hobby. At $20/month for Pro vs $5/month for Workers Paid, cost diverges as traffic grows.

#### 3. Netlify

Strong agent story: the Netlify MCP Server is official and GA (released June 2025). `@astrojs/netlify` is a first-class adapter. Docs ship with `llms.txt` at `docs.netlify.com/llms.txt` (GA). The free credit tier comfortably covers 100k monthly requests. Gap vs. the top two: no `netlify rollback` CLI command — rollback is dashboard-only (scored Partial on stable deploy API); free-tier serverless functions run in AWS us-east-2 (Ohio) — EU region placement requires a paid plan, which matters for Supabase round-trip latency from Poland. Requires adapter swap from `@astrojs/cloudflare`.

## Anti-Bias Cross-Check: Cloudflare Workers

### Devil's Advocate — Weaknesses

1. **Node.js compatibility gaps at runtime**: Workers runs on V8 isolates, not Node.js. Even with `nodejs_compat`, some npm packages that use `fs`, `Buffer`, or `child_process` fail at runtime — not at build time. Errors surface only in production or under `wrangler dev`, not during `npm run dev`.
2. **`npm run dev` ≠ Cloudflare runtime**: The default dev server runs on Node.js (Vite). Bugs specific to the Workers runtime won't reproduce locally unless you use `wrangler dev` or `npm run preview`. As of Astro 6.0, `astro dev` now runs on the real `workerd` engine — but developers who skip the adapter context and run Vite directly will miss this.
3. **Free tier CPU time limit**: Workers free tier enforces a 10ms CPU time limit per request (not wall clock). An SSR page making a Supabase auth check plus a DB query can exceed this limit under load. Workers Paid ($5/month) removes the cap.
4. **Pages secrets vs Workers secrets are two separate systems**: `wrangler pages secret put` (Pages deployments) and `wrangler secret put` (Workers deployments) are different commands that write to different stores. Updating the wrong one leaves the deployed app with stale credentials.
5. **1 MB uncompressed script size limit on Pages Functions**: Astro + React 19 + shadcn/ui can approach this limit. The adapter handles code splitting, but UI component libraries with poor tree-shaking can push the bundle over.

### Pre-Mortem — How This Could Fail

The team deployed LicenceQuizz on Cloudflare with Astro 6 SSR. Auth and the quiz loop worked well for the first few weeks. Mid-project, a video URL normalization library was added; it used Node.js's `url` module in a way that the `nodejs_compat` polyfill didn't cover. The build passed cleanly — Astro bundles at compile time — but the feature crashed at runtime with an opaque `ReferenceError` that appeared only in production and under `wrangler dev`, not during `npm run dev` with Vite. The team spent a weekend bisecting an error that had no local analog. Separately, three family members studying simultaneously during exam week pushed several SSR pages over the 10ms CPU limit on the free tier; responses degraded and some returned 1101 errors. Upgrading to Workers Paid ($5/month) resolved it, but the team hadn't tracked it as a risk. The final friction: Supabase rotated its anon key and the developer updated `.dev.vars` (local only) rather than `wrangler pages secret put` (deployed secret). Auth broke in production for a day before the split secret-management surfaces were understood.

### Unknown Unknowns

- **`npm run dev` and `wrangler dev` are different runtimes.** The project's `npm run dev` starts the Vite/Astro dev server. As of Astro 6, `astro dev` with `@astrojs/cloudflare` runs on the real `workerd` engine — but this only applies when running through the Astro CLI with the adapter active. Running `wrangler dev` directly gives the most faithful local reproduction.
- **Bundle size limit**: Pages Functions have a 1 MB uncompressed script limit. Large shadcn/ui + React bundles can approach this ceiling; monitor with `wrangler pages deploy --dry-run`.
- **Two distinct Cloudflare MCP tools with similar names**: Cloudflare's *deployment management* MCP server (for remote deploy/logs from Claude Code) is different from `@cloudflare/mcp-server-cloudflare` (for building MCP servers on Workers). Using the wrong one when granting agent access to deployments won't work.
- **Supabase SSR cookie plumbing**: `@supabase/ssr` requires `Request`/`Response` header-based cookie handling. Astro middleware abstracts this correctly, but any Supabase client use outside Astro's middleware context (e.g., in a raw fetch handler) will break auth silently.
- **Preview URLs are publicly accessible by default**: Cloudflare Pages branch preview deployments are public — no built-in password protection. For a private app, configure Cloudflare Access (free tier) to gate preview URLs.

## Operational Story

- **Preview deploys**: Every push to a non-main branch triggers a Cloudflare Pages preview build automatically (via GitHub integration). Each preview gets a unique `*.pages.dev` subdomain. Preview URLs are publicly accessible by default — add Cloudflare Access (free) to password-gate them for this private app.
- **Secrets**: Environment variables and secrets live in the Cloudflare dashboard under the project's Settings → Environment Variables, or set via `wrangler pages secret put <KEY>` for Pages deployments. Never commit `.dev.vars` (local-only). Rotation means running `wrangler pages secret put` again — the new value is picked up on the next deployment.
- **Rollback**: `npx wrangler rollback` (Workers) or via the Cloudflare dashboard Deployments tab → pick a prior build → Rollback. Typical time-to-revert is under 30 seconds. DB migrations do not auto-rollback — revert schema changes separately via Supabase.
- **Approval**: Deployments to production (`wrangler deploy` / `wrangler pages deploy`) may be performed by the agent. Dropping database tables, rotating primary Supabase secrets, billing changes, or deleting the Workers project are human-only actions.
- **Logs**: `npx wrangler tail` streams live request logs (filterable by `--status error`, `--search`, `--format json`). Build logs are visible via `wrangler pages deployment list` and the Cloudflare dashboard. Cloudflare's remote MCP servers (GA) expose structured log access from Claude Code.

## Risk Register

| Risk | Source | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| Third-party npm package fails at Workers runtime (Node.js compat gap) | Devil's advocate | Medium | Medium | Run `wrangler dev` for runtime-accurate local testing before deploying new dependencies; check `nodejs_compat` docs before adding packages that use `fs`/`net`/`child_process` |
| Free tier CPU limit (10ms/request) exceeded under light concurrent load | Devil's advocate | Low | Medium | Upgrade to Workers Paid ($5/mo) at first sign of 1101 errors; pre-emptively upgrade before launch |
| Secrets updated in `.dev.vars` but not in Cloudflare Pages secrets | Pre-mortem | Medium | High | Document the split secret system; add a rotation checklist to deploy-plan.md; update `.dev.vars` and run `wrangler pages secret put` together |
| Pages Functions bundle exceeds 1 MB limit | Devil's advocate | Low | High | Monitor bundle size with `wrangler pages deploy --dry-run`; audit shadcn/ui imports for unused components |
| Preview URLs publicly accessible for private app | Unknown unknowns | High | Low | Configure Cloudflare Access (free tier) on the `*.pages.dev` project to require email-based auth for preview branches |
| Supabase SSR cookie handling breaks outside Astro middleware | Unknown unknowns | Low | High | Keep all Supabase client initialization inside Astro middleware or page context; never instantiate it in raw Workers fetch handlers |
| Cloudflare Access token or MCP server misconfigured with too-broad scope | Pre-mortem | Low | High | Use project-scoped API tokens; grant agent only Pages deploy + tail logs, not DNS or account-level permissions |

## Getting Started

1. **Authenticate**: `npx wrangler login` — opens browser OAuth to link your Cloudflare account. Run once per machine.
2. **Verify the adapter**: `@astrojs/cloudflare` is already installed and configured in `astro.config.mjs`. Run `npm run build` to confirm the Workers-compatible bundle builds cleanly.
3. **Set secrets**: `npx wrangler pages secret put SUPABASE_URL` then `npx wrangler pages secret put SUPABASE_KEY` — these map to the `astro:env/server` schema declared in `astro.config.mjs`.
4. **Deploy**: `npx wrangler pages deploy ./dist` (Pages) or follow `context/deployment/deploy-plan.md` for the step-by-step sequence if a plan already exists.
5. **Verify live**: `npx wrangler tail` streams request logs; test the sign-in flow at the deployed `.pages.dev` URL to confirm Supabase auth is wired correctly.

## Out of Scope

The following were not evaluated in this research:
- Docker image configuration
- CI/CD pipeline setup (GitHub Actions is already configured in `.github/workflows/ci.yml`)
- Production-scale architecture (multi-region, HA, DR)
