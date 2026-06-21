# Repository Guidelines

ClickUp Simplifier is a personal, keyboard-driven ClickUp client: a local Spring/Java core with swappable web, Flutter, and JavaFX frontends. Scope: @context/foundation/prd.md. Layout: @context/foundation/roadmap.md.

## Hard Rules

- Domain logic lives in `server/`; clients call the core over REST — do not embed sync or write-back rules in UI-only code.
- Tasks always nest under milestones; tasks without a milestone belong in a visible virtual "no milestone" node — never hide them or flatten the hierarchy.
- MVP sync sets are exactly two: `Podstawowe słowniki` and `Zadania`, each with independent `lastSuccessAt` / `lastFailureAt` / `errorDescription`; a failure must not clear the last success timestamp.
- Push to ClickUp only after explicit user approval of the pending-changes queue — no silent or automatic write-back.
- Never log, embed, or show the ClickUp personal API token.
- Keyboard-first: every user-facing action must be reachable without a mouse; focus moves only on direct user action.
- Do not write under `context/archive/`. No half-finished scaffolds or premature helpers — see @.cursor/rules/coding-style.mdc.

## Project Structure

- `server/` — Spring Boot 4 core (@server/pom.xml)
- `clients/web/` — Vite + React + TypeScript (@clients/web/package.json)
- `clients/flutter/` — Flutter web/desktop (@clients/flutter/pubspec.yaml)
- `clients/desktop-java/` — JavaFX (@clients/desktop-java/pom.xml); `build`/`run` wrappers next to `pom.xml`
- `context/foundation/` — PRD, tech-stack hand-offs, roadmap
- `.cursorrules`, `.cursor/rules/` — domain and style rules for agents

## Build, Test, and Development Commands

Java targets **Java 21**. Default shell `JAVA_HOME` / `mvn` may be JDK 11 / Maven 3.5.0 — use the `build`/`run` wrappers that sit next to each `pom.xml` and require `JAVA_HOME21` and `MAVEN_HOME9`: `server/build.sh`, `server/run.sh`, `clients/desktop-java/build.sh`, `clients/desktop-java/run.sh` (`.cmd` equivalents for cmd.exe / PowerShell).

Web (`clients/web/`): `npm run dev`, `npm run build`, `npm run lint`.

Flutter (`clients/flutter/`): `flutter pub get`, `flutter analyze`, `flutter build web` (or `flutter run -d windows`).

Spring tests: `server/build.sh clean test`.

## Coding Style & Naming

Java 21 in `server/` and `clients/desktop-java/`; Dart `^3.12.0` in Flutter; TypeScript in web. Comment only non-obvious *why* (@.cursor/rules/coding-style.mdc). Web lint: @clients/web/eslint.config.js. Flutter lint: @clients/flutter/analysis_options.yaml.

## Commits & Pull Requests

Recent commits use short Polish imperative subjects; older history mixed `feat(scope):` / `chore(scope):` — match the branch. Remote `origin` on GitHub; active branch `variants/clickup_simplifier`. No root `.github/workflows/` — run the relevant build/lint locally before pushing.

## Deeper Docs

Domain: @.cursorrules, @.cursor/rules/domain.mdc. Architecture: @context/foundation/roadmap.md. 10xDevs agent toolchain: @CLAUDE.md (not app runtime).
