# Repository Guidelines

Native web client for ClickUp Simplifier — Vite + React 19 + TypeScript SPA calling the local Spring core over REST. Repo-wide rules: @AGENTS.md at the repo root.

## Local Rules

- UI only — no sync engine, write-back queue, or ClickUp API calls in this tree; delegate to `server/`.
- Every user-facing action must be keyboard-reachable; focus moves only on direct user action.
- Never store, log, or display the ClickUp token in browser code or env vars checked into git.

## Adding a UI Unit

Add screens as `PascalCase.tsx` under `src/`, following @./src/App.tsx: default export, co-located CSS import, `type="button"` on non-submit buttons. Wire new roots through @./src/main.tsx only when replacing the app shell.

## File Layout & Naming

- `src/main.tsx` — React bootstrap (`StrictMode` + `createRoot`)
- `src/App.tsx` — reference root component shape
- `src/assets/` — bundled images; `public/` — static files at `/`
- Config: @package.json, @vite.config.ts, @eslint.config.js, @tsconfig.app.json

No `@/*` path alias yet — use relative imports. TypeScript runs strict (`verbatimModuleSyntax`, `noUnusedLocals` in @tsconfig.app.json).

## Commands & Lint

From `clients/web/`: `npm run dev` (HMR), `npm run build` (`tsc -b && vite build`), `npm run lint` (ESLint flat config). Run `npm run lint` before finishing UI work.

## Testing

No test runner yet. When added, co-locate `*.test.tsx` next to the component.

## Tripwires

- Do not import from `server/` or embed domain invariants beyond presentation.
- Do not add direct ClickUp HTTP clients.
