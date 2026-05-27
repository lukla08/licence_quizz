---
project: LicenceQuizz
researched_at: 2026-05-27
recommended_platform: Cloudflare Pages + Workers
runner_up: Vercel
context_type: mvp
tech_stack:
  language: TypeScript
  framework: Astro 6 SSR
  runtime: Cloudflare Workers (workerd)
  database: Supabase (external, PostgreSQL + Auth)
---

## Recommendation

**Deploy on Cloudflare Pages + Workers.**

The project already uses the `@astrojs/cloudflare` adapter, Wrangler-based dev server, and `astro:env/server` bindings — Cloudflare Pages is the only candidate with zero migration cost. It earned the highest score across all five agent-friendly criteria (CLI-first via `wrangler`, fully managed/serverless, llms.txt + GitHub markdown docs, deterministic deploy API, GA MCP servers as of May 2025), and the free tier comfortably covers the project's small private user group with no paid tier required at MVP scale.

## Platform Comparison

| Platform | CLI-first | Managed / Serverless | Agent-readable docs | Stable deploy API | MCP / Integration | Total |
|---|---|---|---|---|---|---|
| **Cloudflare Pages** | Pass | Pass | Pass | Pass | Pass | **5 / 5** |
| Vercel | Pass | Pass | Pass | Pass | Partial (beta MCP) | **4.5 / 5** |
| Netlify | Partial | Pass | Partial | Pass | Pass | **4 / 5** |
| Railway | Pass | Pass | Pass | Pass | Fail | **4 / 5** |
| Render | Pass | Pass | Pass | Pass | Pass | **4.5 / 5** |
| Fly.io | Pass | Partial | Pass | Pass | Fail | **3.5 / 5** |

**Soft-weight notes:**
- Cost neutral → Cloudflare's free tier (100k daily function invocations, unlimited static) is a differentiator at this scale.
- No platform familiarity → no tie-breaker applied.
- Single region (Poland) → edge-native is a nice-to-have, not a requirement; no reweighting.
- External providers (Supabase) → co-location preference not a factor.

### Shortlisted Platforms

#### 1. Cloudflare Pages + Workers (Recommended)

Native `@astrojs/cloudflare` adapter already installed; zero adapter migration cost. `wrangler` CLI covers deploy, log tailing, and rollback. Official docs ship as `llms.txt` and GitHub markdown — the only platform in the shortlist with a published agent-readable doc feed. GA MCP servers across Cloudflare's surface (Workers, docs, observability, AI Gateway) since May 2025. Free tier: 500 builds/month + 100k daily Pages Function requests (resets UTC midnight). Static assets (CSS, JS, images) are always free and do not count against the quota. Cloudflare-specific runtime (workerd) is the main friction point — Node.js builtins are not available, and any npm dependency that reaches for `fs` or `child_process` will fail at runtime.

#### 2. Vercel

Second-best overall score. Mature CLI (`vercel`, `vercel --prod`, `vercel rollback`, `vercel logs`), strong DX, official `@astrojs/vercel` adapter works with Astro 6 SSR. Hobby plan: 150k function invocations/month free. Gap vs Cloudflare: (a) a known Astro 6 + Vercel esbuild build-failure issue (GitHub astro#16258) requires monitoring, (b) Vercel MCP is public beta rather than GA, (c) switching from `@astrojs/cloudflare` to `@astrojs/vercel` requires adapter swap and env-var access refactoring since `astro:env/server` bindings are Cloudflare-specific.

#### 3. Render

Full GA MCP server (Aug 2025) with 20+ tools for logs, metrics, and service management. `render` CLI is GA. `@astrojs/node` adapter works. Gap vs Cloudflare: free tier has a 15-minute inactivity spin-down (30–60s cold start on wake) — effectively unusable for production without the $7/month Starter tier; requires adapter migration; no llms.txt equivalent in docs.

## Anti-Bias Cross-Check: Cloudflare Pages + Workers

### Devil's Advocate — Weaknesses

1. **Workerd runtime ≠ Node.js — hidden npm incompatibilities.** Any dependency that reaches for `fs`, `path`, `child_process`, or other Node builtins fails in the Workers runtime. Error messages are often generic ("module not found") with no clear import trace. This surfaces mid-feature, not at project start — a silent productivity tax.

2. **Known routing specificity bug (astro#14067).** The adapter incorrectly prioritises deeply nested dynamic routes over static routes, causing 404s on built static pages. If route depth grows (e.g., `/quiz/[category]/[id]`), this can silently break navigation in production while dev looks clean.

3. **Free tier daily reset is a spike trap.** The 100k/day quota resets at UTC midnight, not on a rolling 30-day window. A single viral share could exhaust the daily allowance and serve 429s until midnight. Low-probability for a private user group, but non-zero.

4. **`astro:env/server` creates invisible Cloudflare lock-in.** The project's env var access goes through Cloudflare's binding system, not `process.env`. Future platform migration requires refactoring every `astro:env/server` usage — invisible until mid-migration.

5. **Wrangler rollback is not a single clean command.** Unlike `vercel rollback`, reverting a Pages deployment requires `wrangler deployments list` then `wrangler rollback <version-id>` — documented but rough for automated recovery.

### Pre-mortem — How This Could Fail

The team shipped LicenceQuizz on Cloudflare Pages. Six months later, the decision had become a persistent low-grade friction. The launch went well — instant deploys, zero cost, the existing adapter "just worked." The first crack appeared when an admin needed a bulk question-import tool. A promising Node library for parsing a source XLSX was added; it worked in local Astro dev (Node.js process) but failed with a cryptic error in production because it used `fs.readFileSync` under the hood. Three hours of debugging later, the import was moved out of the app entirely and runs as a local script directly against Supabase. Not a disaster, but a time tax nobody budgeted for.

The second problem was quiz session durability. The workerd isolate model means no shared in-process state between requests. The "save every answer immediately" guardrail required every quiz answer to be written to Supabase synchronously. Under a training session with rapid answer clicks, this surfaced Supabase's connection-pool limits on the free tier. The fix (connection pooling via PgBouncer) was correct but added a configuration layer nobody wanted to maintain.

The final friction was key rotation. When it was time to rotate the Supabase service key, updating Workers Secrets required re-deploying the Worker — causing a brief window where the old secret was gone and the new one hadn't propagated. There is no atomic secret-swap operation. The team patched it with a maintenance window, but the experience revealed that the operational story for secret management was rougher than the "just use `.dev.vars`" docs implied.

### Unknown Unknowns

- **`astro:env/server` does not fall back to `process.env`.** A helper function using `process.env.SUPABASE_URL` will silently get `undefined` without a runtime error — the single most common "works locally, 403s in prod" mistake with this stack.
- **Pages Functions ≠ Workers — some bindings not available.** If you later add Cron Triggers, Queues consumers, or certain Analytics Engine bindings, you'll need to migrate from Pages to Workers + Assets — a meaningful topology change mid-project.
- **Static asset requests are not metered.** The 100k/day limit applies only to dynamic SSR route invocations, not to CSS, JS, or image requests. Real quota consumption is much lower than the headline number for a quiz app with heavy client-side assets.
- **Local dev fidelity is a feature.** `wrangler dev` runs the real workerd runtime, so Node-incompatible libraries surface immediately in dev, not production. This feels like friction but is the best possible DX outcome.
- **Cloudflare CDN caching can serve stale assets after deploy** if custom `Cache-Control` rules or Transform Rules are added via the dashboard. Astro's content-hash filenames handle this by default; the risk is zero until someone touches Cloudflare settings manually.

## Operational Story

- **Preview deploys**: Every GitHub push (non-master branch) automatically creates a preview URL at `<branch>.<project>.pages.dev`. Preview deployments share the production Workers Secrets unless overridden per-environment in the Pages dashboard. Fork PRs from external contributors do not receive production secrets by default.
- **Secrets**: Environment variables live in Cloudflare Pages → Settings → Environment Variables (or via `wrangler secret put`). Server-side secrets (`SUPABASE_URL`, `SUPABASE_KEY`) are stored as encrypted Workers Secrets, not in `.env`. Locally, they go in `.dev.vars` (gitignored). Rotation requires a Pages re-deploy; no atomic swap.
- **Rollback**: `wrangler deployments list --name <project>` to find the target deployment ID, then `wrangler rollback <deployment-id>`. Alternatively, promote a prior deployment to production via the Pages dashboard. Time-to-revert: ~1–2 minutes. Database migrations (Supabase) do not roll back automatically — revert app code only if the schema change is backwards-compatible.
- **Approval**: Secret rotation, Pages project deletion, DNS record changes, and billing tier changes are human-only (dashboard or scoped CLI token required). An agent may: deploy (`wrangler deploy`), tail logs (`wrangler tail`), list deployments, and rollback code — all with a project-scoped API token.
- **Logs**: `wrangler tail --name <project>` streams real-time runtime logs to the terminal. Build logs are accessible via `wrangler pages deployment list` and the Pages dashboard. For structured log queries, the Cloudflare Workers observability MCP server (`cloudflare-observability`) can be used with Claude Code.

## Risk Register

| Risk | Source | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| npm dependency uses Node builtin (`fs`, `child_process`) and fails silently in workerd | Devil's advocate | Medium | Medium | Audit dependencies at install time; run `wrangler dev` (not `node`) for all local dev to surface incompatibilities early |
| Routing specificity bug (astro#14067) causes 404 on nested static routes | Devil's advocate | Low-Medium | Medium | Avoid deeply nested static + dynamic route collisions; test full route tree in `wrangler dev` before each deploy; monitor the upstream issue |
| Daily 100k quota exhausted by traffic spike → 429s until midnight | Devil's advocate | Low | Medium | Monitor usage via Cloudflare Analytics; upgrade to paid Workers plan ($5/mo, 10M requests) if spike risk materialises |
| `astro:env/server` bindings create platform lock-in invisible until migration | Devil's advocate | High (if migrating) | Medium | Acceptable for MVP; document binding pattern in CLAUDE.md so future agent knows the constraint |
| Wrangler rollback requires two-step CLI sequence | Devil's advocate | Low | Low | Document exact rollback command sequence in CLAUDE.md; accept as minor ops friction |
| Bulk data import libraries require Node builtins not available in workerd | Pre-mortem | Medium | Low | Run admin scripts (question import, data migration) locally against Supabase directly — never through the Cloudflare-hosted app layer |
| Supabase free tier connection limits hit under rapid quiz answers | Pre-mortem | Low | Medium | Use Supabase's connection pooler (Transaction mode) from the start; set `?pgbouncer=true` in the connection string |
| Wrangler secret rotation causes brief outage window | Pre-mortem | Low | Low | Schedule rotations during off-hours; test rotation procedure in a preview environment first |
| Pages Functions surface area differs from standalone Workers (Cron, Queues unavailable) | Unknown unknowns | Low (MVP scope) | Low | Note in CLAUDE.md; if Cron or Queues are added post-MVP, plan migration to Workers + Assets |
| Custom Cloudflare caching rules override Astro's content-hash cache busting | Unknown unknowns | Low | Medium | Do not add manual Cloudflare caching/Transform rules without verifying they don't interfere with Astro's default asset fingerprinting |

## Getting Started

1. **Authenticate Wrangler**: `npx wrangler login` (opens browser OAuth to your Cloudflare account). Verify with `npx wrangler whoami`.
2. **Create the Pages project** (first deploy only): `npm run build && npx wrangler pages deploy ./dist --project-name licence-quizz`. Wrangler creates the project if it doesn't exist and returns a `*.pages.dev` URL.
3. **Set production secrets**: `npx wrangler pages secret put SUPABASE_URL --project-name licence-quizz` and `npx wrangler pages secret put SUPABASE_KEY --project-name licence-quizz`. Set the same vars for the Preview environment in the Pages dashboard.
4. **Connect GitHub for auto-deploys**: In the Cloudflare Pages dashboard → Your project → Settings → Git integration → connect the GitHub repo and select the `master` branch. The existing `.github/workflows/ci.yml` handles lint + build; Cloudflare's GitHub integration handles the actual deployment on merge.
5. **Tail logs**: `npx wrangler tail --name licence-quizz` to stream live runtime logs after deployment.

## Out of Scope

The following were not evaluated in this research:
- Docker image configuration
- CI/CD pipeline setup (GitHub Actions already configured in `.github/workflows/ci.yml`)
- Production-scale architecture (multi-region, HA, DR)
