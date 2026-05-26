---
bootstrapped_at: 2026-05-26T09:23:07Z
starter_id: spring
starter_name: Spring Boot
project_name: licence-quizz-api
language_family: java
package_manager: maven
cwd_strategy: subdir-then-move (adapted — scaffolded into ./licence-quizz-api/ subfolder, not cwd root)
bootstrapper_confidence: verified
phase_3_status: ok
audit_command: null
---

## Hand-off

Verbatim from `context/foundation/tech-stack.md`:

```yaml
starter_id: spring
package_manager: maven
project_name: licence-quizz-api
hints:
  language_family: java
  team_size: solo
  deployment_target: render
  ci_provider: github-actions
  ci_default_flow: auto-deploy-on-merge
  bootstrapper_confidence: verified
  path_taken: standard
  quality_override: false
  self_check_answers: null
  has_auth: true
  has_payments: false
  has_realtime: false
  has_ai: false
  has_background_jobs: false
```

**Why this stack** (from hand-off body): This hand-off covers only the shared backend API — the foundation all five client variants depend on for email/password auth, per-account data isolation, cross-device sync, the CMS question store, and video metadata. The project is deliberately a comparative multi-client build (Flutter, two Android-native variants, JavaFX, Java Swing), so each client gets its own stack decision later; this run scopes the backend alone. Java/Spring Boot was chosen explicitly: it reuses the JVM skills already exercised by three of the five clients, and it clears all four agent-friendly gates with verified scaffolder support. Scale is small and the timeline is 7 after-hours weeks. Render is the deployment target; auto-deploy-on-merge on GitHub Actions matches that. Auth is the only stack-forcing feature flag in scope. Open item: video hosting (external vs. database) is unresolved.

## Pre-scaffold verification

| Signal       | Value   | Severity | Notes                                                                 |
| ------------ | ------- | -------- | --------------------------------------------------------------------- |
| npm package  | not run | n/a      | non-JS starter; scaffold is a curl pull from start.spring.io, not npm |
| GitHub repo  | not run | n/a      | card docs_url is docs.spring.io (not a github.com/owner/repo)         |

start.spring.io is a live, maintained service (Spring Initializr); it always emits a current Spring Boot release. Generated `pom.xml` pinned to spring-boot-starter-parent 4.0.6, Java 21.

## Scaffold log

**Resolved invocation**: `curl -fsS --max-time 60 https://start.spring.io/starter.tgz -d dependencies=web,devtools -d type=maven-project -d javaVersion=21 -d groupId=com.example -d artifactId=licence-quizz-api -d name=licence-quizz-api -d packageName=com.example.licencequizz | tar -xzf -` (run inside `./licence-quizz-api/`)
**Strategy**: subdir-then-move, adapted — extracted directly into the `licence-quizz-api/` subfolder per the user's multi-component layout decision (backend isolated from cwd root, which is the umbrella repo for context/ + 5 future client dirs). No `.bootstrap-scaffold` temp dir was used.
**Exit code**: 0
**Files written**: 9 (`.gitattributes`, `.gitignore`, `HELP.md`, `mvnw`, `mvnw.cmd`, `pom.xml`, `src/main/java/com/example/licencequizz/LicenceQuizzApiApplication.java`, `src/main/resources/application.properties`, `src/test/java/com/example/licencequizz/LicenceQuizzApiApplicationTests.java`) plus `.mvn/wrapper/`.
**Conflicts (.scaffold siblings)**: none (fresh subfolder)
**.gitignore handling**: scaffold's own `.gitignore` lives inside `licence-quizz-api/`; cwd-root `.gitignore` untouched (separate subfolder, no merge)
**.bootstrap-scaffold cleanup**: n/a (no temp dir)
**tar warnings**: benign — clock-skew "timestamp in the future" notices and `LIBARCHIVE.creationtime` unknown-header notices from Git Bash's bsdtar; no extraction errors.

## Post-scaffold audit

**Tool**: skipped — no built-in audit tool for java
**Recommended external tool**: OWASP Dependency-Check (Maven plugin `org.owasp:dependency-check-maven`, goal `dependency-check:check`) for CVE scanning of the dependency tree; or Snyk / GitHub Dependabot once the repo is pushed.

## Hints recorded but not acted on

| Hint                    | Value               |
| ----------------------- | ------------------- |
| bootstrapper_confidence | verified            |
| quality_override        | false               |
| path_taken              | standard            |
| self_check_answers      | null                |
| team_size               | solo                |
| deployment_target       | render              |
| ci_provider             | github-actions      |
| ci_default_flow         | auto-deploy-on-merge |
| has_auth                | true                |
| has_payments            | false               |
| has_realtime            | false               |
| has_ai                  | false               |
| has_background_jobs     | false               |

## Next steps

Next: a future skill will set up agent context (CLAUDE.md, AGENTS.md). For now, your backend is scaffolded and verified — happy hacking.

Useful manual steps in the meantime:
- Decide whether `licence-quizz-api/` is its own git repo or a folder within this umbrella repo (the umbrella already has a `.git/`).
- Add the dependencies the auth + data requirements need (none were auto-added — only `web` + `devtools` from the template): Spring Security, Spring Data JPA, a PostgreSQL driver, Bean Validation. These are deliberate design choices left to you.
- Resolve the open PRD question on video hosting (external store/CDN vs. database) before modelling that part of the data layer.
- Set up the GitHub Actions workflow and Render deployment when ready (not scaffolded in v1).
