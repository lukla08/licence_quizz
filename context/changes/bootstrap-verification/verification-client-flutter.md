---
bootstrapped_at: 2026-06-20T15:44:00Z
starter_id: flutter
starter_name: Flutter
project_name: clickup-simplifier-flutter
language_family: dart
package_manager: pub
cwd_strategy: subdir-then-move (adapted — scaffolded into clients/flutter/)
bootstrapper_confidence: verified
phase_3_status: ok
audit_command: null
---

## Hand-off

```yaml
starter_id: flutter
package_manager: pub
project_name: clickup-simplifier-flutter
hints:
  language_family: dart
  team_size: solo
  deployment_target: self-host
  ci_provider: github-actions
  ci_default_flow: manual-promotion
  bootstrapper_confidence: verified
  path_taken: standard
  quality_override: false
  self_check_answers: null
  has_auth: false
  has_payments: false
  has_realtime: false
  has_ai: false
  has_background_jobs: false
```

> Flutter client of the swappable-frontend set: one Dart codebase rendering the
> UI across desktop and web, talking to the local Spring core over REST. This is
> the recommended default for the (desktop/mobile, dart) cell and clears all four
> agent-friendly gates with `verified` scaffolding confidence, so the standard
> path was taken. Deployment is self-host: local desktop/web builds. No feature
> flags are set on the client — sync, token storage, and write-back live in the
> Spring core. CI mirrors the core: GitHub Actions with manual promotion.

## Pre-scaffold verification

| Signal      | Value   | Severity | Notes                                                       |
| ----------- | ------- | -------- | ----------------------------------------------------------- |
| npm package | not run | n/a      | non-JS starter; cmd_template is the flutter CLI, not npm    |
| GitHub repo | not run | n/a      | card docs_url (flutter.dev) is not a github.com/<owner>/<repo> |

Local-toolchain note (not part of the standard recency slot): at scaffold time the installed Flutter SDK was `3.25.0-1.0.pre.190` on the master channel, revision dated 2024-08-29 — quite old. **Resolved below** (see "SDK upgrade").

## Scaffold log

**Resolved invocation**: `flutter create -e --project-name clickup_simplifier --org com.example --platforms web,windows,macos,linux flutter` (run inside `clients/`, creating `clients/flutter/`)
**Strategy**: subdir-then-move, adapted — scaffolded into a dedicated component directory `clients/flutter/`, not the repo root.
**Exit code**: 0 (flutter create also resolved dependencies via pub)
**Files written**: 66 (Dart app under `lib/`, platform folders `web/`, `windows/`, `macos/`, `linux/`, `pubspec.yaml`, `pubspec.lock`, `analysis_options.yaml`, `.metadata`, `.idea/`)
**Conflicts (.scaffold siblings)**: none (dedicated empty subdirectory)
**.gitignore handling**: the scaffold's own `clients/flutter/.gitignore` was kept in place (component-local); the repo-root `.gitignore` was not touched
**.bootstrap-scaffold cleanup**: n/a (no temp directory used)

Forced adaptations of the card's `cmd_template` (`flutter create -e {name} --org com.example --platforms android,ios,web`):
- **Project name**: the hand-off `project_name` (`clickup-simplifier-flutter`) is not a valid Dart package name (dashes), and the directory name `flutter` is a reserved package name. Resolved by passing `--project-name clickup_simplifier` explicitly while keeping the directory `clients/flutter/`.
- **Platforms**: changed from `android,ios,web` to `web,windows,macos,linux` to match the client's actual targets (desktop + web), since this is not a mobile client.

## SDK upgrade — master prerelease → stable 3.44.2

The global Flutter SDK (`C:\projekty\sdk\flutter`, shared across the user's
projects) was on the `master` contributor channel at an old prerelease. The user
chose to switch to `stable` and upgrade:

```bash
flutter channel stable && flutter upgrade
```

- **Before**: Flutter `3.25.0-1.0.pre.190` (master, 2024-08-29), Dart 3.6.0
- **After**: Flutter **`3.44.2`** (stable, 2026-06-10), Dart **3.12.2**
- `flutter doctor`: clean except unaccepted Android licenses — irrelevant to this
  web+desktop client (no Android target).

Project adjustment for the new SDK (`clients/flutter/pubspec.yaml`):
- `environment.sdk`: `^3.6.0-195.0.dev` → `^3.12.0` (clean stable constraint)

**Verification on the new SDK**:
- `flutter pub get` → resolved (15 transitive deps bumped)
- `flutter analyze` → **No issues found**
- `flutter build web` → **Built build\web** (compiles end-to-end)

## Post-scaffold audit

**Tool**: skipped — no built-in audit tool for dart
**Recommended external tool**: `dart pub outdated --mode=null-safety` (closest stand-in) for dependency freshness; no first-party vulnerability scanner ships with the Dart toolchain.

## Hints recorded but not acted on

| Hint                    | Value           |
| ----------------------- | --------------- |
| bootstrapper_confidence | verified        |
| quality_override        | false           |
| path_taken              | standard        |
| self_check_answers      | null (standard path) |
| team_size               | solo            |
| deployment_target       | self-host       |
| ci_provider             | github-actions  |
| ci_default_flow         | manual-promotion |
| has_auth                | false           |
| has_payments            | false           |
| has_realtime            | false           |
| has_ai                  | false           |
| has_background_jobs     | false           |

## Next steps

Next: a future skill will set up agent context (CLAUDE.md, AGENTS.md). For now, the Flutter client is scaffolded and verified — happy hacking.

Useful manual steps in the meantime:
- `cd clients/flutter && flutter run -d chrome` (web) or `-d windows` (desktop) to launch.
- SDK is now on stable 3.44.2 (Dart 3.12.2); `flutter analyze` and `flutter build web` are clean.
- Wire the client to the local Spring core's REST API (the core lives in `server/`).
- Remaining client per `context/foundation/roadmap.md`: Java desktop (`tech-stack-client-desktop-java.md`, `best-effort`).
