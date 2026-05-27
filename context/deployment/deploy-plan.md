# Deploy Plan — LicenceQuizz → Cloudflare Workers

**Platform:** Cloudflare Workers + Assets (wrangler.jsonc, `@astrojs/cloudflare` adapter)
**Decided:** 2026-05-27 — see `context/foundation/infrastructure.md`
**Status:** IN PROGRESS — stopped at Step 3 (Wrangler login)

---

## Completed

- [x] `wrangler.jsonc` — Worker renamed `10x-astro-starter` → `licence-quizz` (commit `ad68375`)
- [x] `npm ci && npm run build` — `./dist` exists, build clean

---

## Remaining steps

### Step 3 — Wrangler login (MANUAL)

```
npx wrangler login
```

Opens browser OAuth to Cloudflare account. After login:

```
npx wrangler whoami
```

### Step 4 — Set Worker secrets (MANUAL — user types values)

```bash
npx wrangler secret put SUPABASE_URL --name licence-quizz
npx wrangler secret put SUPABASE_KEY --name licence-quizz
```

Values from: **Supabase dashboard → Project Settings → API**
- `SUPABASE_URL` → Project URL
- `SUPABASE_KEY` → `anon` `public` key

### Step 5 — First deploy

```bash
npx wrangler deploy
```

Returns: `https://licence-quizz.<subdomain>.workers.dev`

### Step 6 — Smoke test

| Route | Expected |
|---|---|
| `GET /` | 200 |
| `GET /auth/signin` | 200 |
| `GET /dashboard` | redirect → `/auth/signin` |

### Step 7 — GitHub auto-deploy

**7a. Create scoped Cloudflare API token (MANUAL)**

Cloudflare Dashboard → My Profile → API Tokens → Create Token
- Template: **Edit Cloudflare Workers**
- Scope: Account → All accounts; Zone → All zones

**7b. Add GitHub repository secrets (MANUAL)**

GitHub repo → Settings → Secrets and variables → Actions:
- `CLOUDFLARE_API_TOKEN` — from step 7a
- `SUPABASE_URL`
- `SUPABASE_KEY`

**7c. Update `.github/workflows/ci.yml`**

Add a `deploy` job after the existing `ci` job:

```yaml
  deploy:
    needs: ci
    if: github.ref == 'refs/heads/master' && github.event_name == 'push'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 22
          cache: npm
      - run: npm ci
      - run: npm run build
        env:
          SUPABASE_URL: ${{ secrets.SUPABASE_URL }}
          SUPABASE_KEY: ${{ secrets.SUPABASE_KEY }}
      - run: npx wrangler deploy
        env:
          CLOUDFLARE_API_TOKEN: ${{ secrets.CLOUDFLARE_API_TOKEN }}
```

### Step 8 — Record result

Update this file:
- Deployed URL
- Date of first deploy
- Secrets wired status

---

## Deployed (fill in after Step 5)

| Field | Value |
|---|---|
| URL | — |
| First deployed | — |
| SUPABASE_URL secret | — |
| SUPABASE_KEY secret | — |
| CLOUDFLARE_API_TOKEN (GitHub) | — |
| Auto-deploy active | — |
