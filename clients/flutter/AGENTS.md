# Repository Guidelines

Flutter client for ClickUp Simplifier — one Dart codebase for web and desktop, talking to the local Spring core over REST. Repo-wide rules: @AGENTS.md at the repo root.

## Local Rules

- UI and platform glue only — no sync engine, write-back queue, or ClickUp API calls in this tree.
- Every user-facing action must be keyboard-reachable; focus moves only on direct user action.
- Never store, log, or display the ClickUp token in Dart code or committed config.

## Adding a UI Unit

Add widgets under `lib/`, following @./lib/main.dart (`MaterialApp`, const constructors). Split features into `lib/<feature>/<widget>.dart`; keep `main()` thin. Package name: `clickup_simplifier` (@pubspec.yaml), not directory `flutter`.

## File Layout & Naming

- `lib/main.dart` — entry point and reference app shell
- `pubspec.yaml` — Dart `^3.12.0`, dependencies via `pub`
- `analysis_options.yaml` — `flutter_lints` (@analysis_options.yaml)
- Platform folders: `web/`, `windows/`, `macos/`, `linux/` (no Android/iOS target in this client)

## Commands & Lint

From `clients/flutter/`: `flutter pub get`, `flutter analyze`, `flutter build web`, `flutter run -d windows` (or `-d chrome`). Run `flutter analyze` before finishing UI work.

## Testing

No `test/` directory yet. When added, mirror `lib/` under `test/` (e.g. `test/main_test.dart`) and run `flutter test`.

## Tripwires

- Do not call ClickUp HTTP APIs directly — use the Spring core REST surface.
- Do not embed domain invariants (milestone→task nesting, sync-set timestamps) beyond presentation.
- Do not rename the package to `flutter` — it is a reserved/invalid Dart package name.
