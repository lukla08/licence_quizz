# Repository Guidelines

Licence Quizz is a quiz application built with Astro 6 SSR, React 19 islands, Tailwind 4, Supabase auth, and shadcn/ui, deployed to Cloudflare Workers.

## Hard Rules

- Never use Next.js directives (`"use client"`, `"use server"`) — this is an Astro app.
- Never concatenate Tailwind class strings manually — always use `cn()` from `@/lib/utils`.
- Every API route must export `const prerender = false` — the app uses `output: "server"` globally.
- API routes must export uppercase `GET`/`POST` handlers and validate all input with zod.
- Always enable RLS on new Supabase tables with per-operation, per-role policies.
- Use Astro components for static content and layout; add a React component only when interactivity is needed.

## Project Structure

- `src/components/` — `auth/`, `ui/` (shadcn/ui), shared Astro components
- `src/layouts/` — `Layout.astro` (root shell)
- `src/lib/` — `supabase.ts`, `utils.ts`; business logic in `lib/services/`
- `src/pages/` — `api/auth/`, `auth/`, `index.astro`, `dashboard.astro`
- `src/middleware.ts` — auth guard; `PROTECTED_ROUTES = ["/dashboard"]`
- `src/types.ts` — shared entity types and DTOs
- `supabase/migrations/` — SQL migrations (`YYYYMMDDHHmmss_description.sql`)

## Commands

See `@README.md` (Available Scripts section) for all `npm run` commands. CI gate (`.github/workflows/ci.yml`) runs `npm ci` → `astro sync` → `lint` → `build`; note the `astro sync` step (not in README) — it regenerates type stubs and must precede lint/build. Requires `SUPABASE_URL` and `SUPABASE_KEY` repo secrets.

## Conventions

- Path alias `@/*` → `./src/*`; use it in all imports.
- shadcn/ui: "new-york" variant, components in `src/components/ui/`. Add with `npx shadcn@latest add [name]`.
- React hooks go in `src/components/hooks/`; services in `src/lib/services/`.
- Migration naming: `YYYYMMDDHHmmss_short_description.sql`.

## Commits & PRs

No CI-enforced prefix convention. Recent history uses `chore:` for tooling and bare imperatives for features. PR target: `master`.

## Secrets

See `@README.md` (Getting Started, step 4) for env-file setup. See `@CLAUDE.md` for full auth-flow and environment details.
