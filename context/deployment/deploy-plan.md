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
- [ ] Step 7a — Create scoped Cloudflare API token (MANUAL — still needed for CI)
- [ ] Step 7b — Add GitHub secrets: `CLOUDFLARE_API_TOKEN`, `SUPABASE_URL`, `SUPABASE_KEY` (MANUAL)

---

## Remaining manual steps for CI auto-deploy

### Step 7a — Create scoped Cloudflare API token

Cloudflare Dashboard → My Profile → API Tokens → Create Token
- Template: **Edit Cloudflare Workers**
- Scope: Account → All accounts; Zone → All zones

### Step 7b — Add GitHub repository secrets

GitHub repo → Settings → Secrets and variables → Actions:
- `CLOUDFLARE_API_TOKEN` — from step 7a
- `SUPABASE_URL` — Supabase Project URL
- `SUPABASE_KEY` — Supabase anon public key

---

## Deployed

| Field | Value |
|---|---|
| URL | https://licence-quizz.licquizz.workers.dev |
| First deployed | 2026-05-29 |
| SUPABASE_URL secret | set ✓ |
| SUPABASE_KEY secret | set ✓ |
| CLOUDFLARE_API_TOKEN (GitHub) | pending — Step 7a/7b |
| Auto-deploy active | pending — Step 7a/7b |
