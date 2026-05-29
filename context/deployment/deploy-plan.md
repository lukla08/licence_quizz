# Deploy Plan — LicenceQuizz → Cloudflare Workers

**Platform:** Cloudflare Workers + Assets (wrangler.jsonc, `@astrojs/cloudflare` adapter)
**Decided:** 2026-05-27 — see `context/foundation/infrastructure.md`
**Status:** COMPLETE — deployed 2026-05-29

---

## Completed

- [x] `wrangler.jsonc` — Worker renamed `10x-astro-starter` → `licence-quizz` (commit `ad68375`)
- [x] `npm ci && npm run build` — `./dist` exists, build clean
- [x] Step 3 — Wrangler login (jaroslaw.chybowski@ekspert.biz)
- [x] Step 4 — Secrets set: `SUPABASE_URL`, `SUPABASE_KEY`
- [x] Step 5 — First deploy: `npx wrangler deploy` → `https://licence-quizz.licquizz.workers.dev`
- [x] Step 6 — Smoke tests passed (200/200/302)
- [x] Step 7c — `.github/workflows/ci.yml` updated with `deploy` job
- [x] Step 7a — Scoped Cloudflare API token created
- [x] Step 7b — GitHub secrets added: `CLOUDFLARE_API_TOKEN`, `SUPABASE_URL`, `SUPABASE_KEY`

---

## Deployed

| Field | Value |
|---|---|
| URL | https://licence-quizz.licquizz.workers.dev |
| First deployed | 2026-05-29 |
| SUPABASE_URL secret | set ✓ |
| SUPABASE_KEY secret | set ✓ |
| CLOUDFLARE_API_TOKEN (GitHub) | set ✓ |
| Auto-deploy active | yes ✓ (verified 2026-05-29) |
