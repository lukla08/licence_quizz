---
bootstrapped_at: 2026-06-20T15:11:00Z
starter_id: spring
starter_name: Spring Boot
project_name: clickup-simplifier
language_family: java
package_manager: maven
cwd_strategy: subdir-then-move
bootstrapper_confidence: verified
phase_3_status: ok
audit_command: null
---

## Hand-off

```yaml
starter_id: spring
package_manager: maven
project_name: clickup-simplifier
hints:
  language_family: java
  team_size: solo
  deployment_target: self-host
  ci_provider: github-actions
  ci_default_flow: manual-promotion
  bootstrapper_confidence: verified
  path_taken: custom
  quality_override: false
  self_check_answers:
    typed: true
    from_official_starter: true
    conventions: true
    docs_current: true
    can_judge_agent: true
  has_auth: false
  has_payments: false
  has_realtime: false
  has_ai: false
  has_background_jobs: true
```

> ClickUp Simplifier is a single-user, local-first tool: a shared core (local
> copy, sync engine, domain model) with swappable frontends and no hosted server —
> the only remote is ClickUp's API via a personal token. This hand-off scaffolds
> that core, which the user fixed as Java/Spring; clients (native web, Flutter,
> native Java desktop) are deferred to later, separate bootstraps. Spring is also
> the registry's recommended default for the (backend/API, Java) cell and clears
> all four agent-friendly gates with `verified` bootstrapper confidence, so
> scaffolding will be smooth. It runs locally on-device (self-host), not in the
> cloud, which is why the cloud-oriented JS web default was rejected as an
> architectural mismatch. Background jobs is the one feature flag set — the named
> sync sets run on recurring per-set cadences (FR-003, FR-019); auth is false
> (single user, no login; the API token is secret storage, not app auth), and
> payments, realtime, and AI are out of scope. CI on GitHub Actions with manual
> artifact promotion fits a local desktop app with no remote deploy target. The
> five-point self-check came back clean across all points, so no Socratic nudge
> fired.

## Pre-scaffold verification

| Signal      | Value   | Severity | Notes                                                          |
| ----------- | ------- | -------- | -------------------------------------------------------------- |
| npm package | not run | n/a      | non-JS starter; cmd_template uses curl, not an npm create CLI  |
| GitHub repo | not run | n/a      | card docs_url (docs.spring.io) is not a github.com/<owner>/<repo> |

No automated recency signal available for this starter. Proceeded without warning.

## Scaffold log

**Resolved invocation**: `curl -sS "https://start.spring.io/starter.tgz" -d dependencies=web,devtools -d type=maven-project -d javaVersion=21 -d groupId=com.example -d artifactId=clickup-simplifier -d name=clickup-simplifier -d packageName=com.example.clickupsimplifier | tar -xzf -` (run inside `.bootstrap-scaffold/`)
**Strategy**: subdir-then-move
**Exit code**: 0
**Files moved**: 9 (`.gitattributes`, `.mvn/wrapper/maven-wrapper.properties`, `HELP.md`, `mvnw`, `mvnw.cmd`, `pom.xml`, `src/main/java/com/example/clickupsimplifier/ClickupSimplifierApplication.java`, `src/main/resources/application.properties`, `src/test/java/com/example/clickupsimplifier/ClickupSimplifierApplicationTests.java`)
**Conflicts (.scaffold siblings)**: none
**.gitignore handling**: append-merged (cwd lines kept; Spring lines appended under a `# from spring` separator; `build/` and `.vscode/` de-duped as already present)
**.bootstrap-scaffold cleanup**: deleted

Adaptation note: the Spring card's `cmd_template` uses `{name}` as the Maven `artifactId`, not as a target directory (the Initializr tarball extracts files at archive root). The temp directory `.bootstrap-scaffold/` was created manually to honor the subdir-then-move strategy, and `{name}` was bound to the real `project_name` (`clickup-simplifier`) so the generated `pom.xml` carries a valid, meaningful artifactId.

Post-bootstrap relocation: the core's files (`pom.xml`, `src/`, `mvnw`, `mvnw.cmd`, `.mvn/`, `HELP.md`, `.gitattributes`) were later moved from the repo root into `server/` at the user's request, so the shared core and the swappable clients (`clients/web/`, etc.) live in separate directories. The repo-root `.gitignore` (with the appended Spring rules) was kept at the root.

## Post-scaffold audit

**Tool**: skipped — no built-in audit tool for java
**Recommended external tool**: OWASP Dependency-Check or Snyk, configured separately against the Maven build.

## Hints recorded but not acted on

| Hint                    | Value           |
| ----------------------- | --------------- |
| bootstrapper_confidence | verified        |
| quality_override        | false           |
| path_taken              | custom          |
| self_check_answers      | all five true   |
| team_size               | solo            |
| deployment_target       | self-host       |
| ci_provider             | github-actions  |
| ci_default_flow         | manual-promotion |
| has_auth                | false           |
| has_payments            | false           |
| has_realtime            | false           |
| has_ai                  | false           |
| has_background_jobs     | true            |

## Next steps

Next: a future skill will set up agent context (CLAUDE.md, AGENTS.md). For now, your project is scaffolded and verified — happy hacking.

Useful manual steps in the meantime:
- `git init` is not needed — this directory is already a git repo; review and commit the scaffold when ready.
- No `.scaffold` siblings were created, so there is nothing to reconcile.
- Java has no built-in audit tool wired in; configure OWASP Dependency-Check or Snyk if you want dependency scanning.
- Per the project roadmap (`context/foundation/roadmap.md`), the next chain step is re-running `/10x-tech-stack-selector` for the first client (web / Flutter / desktop) — a separate hand-off and bootstrap.
