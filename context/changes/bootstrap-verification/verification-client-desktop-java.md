---
bootstrapped_at: 2026-06-20T15:48:00Z
starter_id: javafx
starter_name: JavaFX (desktop via OpenJFX Maven archetype)
project_name: clickup-simplifier-desktop-java
language_family: java
package_manager: maven
cwd_strategy: subdir-then-move (adapted — scaffolded into clients/desktop-java/)
bootstrapper_confidence: best-effort
phase_3_status: ok
audit_command: null
---

## Hand-off

```yaml
starter_id: javafx
package_manager: maven
project_name: clickup-simplifier-desktop-java
hints:
  language_family: java
  team_size: solo
  deployment_target: self-host
  ci_provider: github-actions
  ci_default_flow: manual-promotion
  bootstrapper_confidence: best-effort
  path_taken: custom
  quality_override: true
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
  has_background_jobs: false
```

> Native Java desktop client of the swappable-frontend set. The registry had no
> (desktop, java) starter, so a JavaFX card (OpenJFX Maven archetype) was added
> for this pick; it is `best-effort` for scaffolding. JavaFX fails the
> popular-in-training gate, so `quality_override` is true. Maven keeps tooling
> aligned with the Spring core. Deployment is self-host.

## Pre-scaffold verification

| Signal      | Value   | Severity | Notes                                                       |
| ----------- | ------- | -------- | ----------------------------------------------------------- |
| npm package | not run | n/a      | non-JS starter; cmd_template is the Maven archetype CLI     |
| GitHub repo | not run | n/a      | card docs_url (openjfx.io) is not a github.com/<owner>/<repo> |

Local-toolchain note: JDK 21 (`C:\Program Files\Java\jdk-21.0.11+10`) is installed
but the default `JAVA_HOME` points at JDK 11; Maven is 3.5.0 (2017). The project
targets Java 21 per the user's decision — builds must run with `JAVA_HOME` set to
the JDK 21 path.

## Scaffold log

**Resolved invocation**: `mvn -B archetype:generate -DarchetypeGroupId=org.openjfx -DarchetypeArtifactId=javafx-archetype-simple -DarchetypeVersion=0.0.6 -DgroupId=com.example -DartifactId=desktop-java -Dversion=0.0.1 -Djavafx-version=21` (run inside `clients/`)
**Strategy**: subdir-then-move, adapted — scaffolded into a dedicated component directory `clients/desktop-java/`, not the repo root.
**Exit code**: 0 (succeeded on the second attempt — see below)
**Files written**: JavaFX project under `clients/desktop-java/` (`pom.xml`, `src/main/java/com/example/App.java` + supporting classes)
**Conflicts (.scaffold siblings)**: none (dedicated empty subdirectory)
**.gitignore handling**: not shipped by this archetype; repo-root `.gitignore` not touched
**.bootstrap-scaffold cleanup**: n/a (no temp directory used)

### First attempt — failed (environment)

The first run failed at plugin resolution: the corporate Maven mirror
(`nexus.ekspert.firma:8081`) was unreachable, so `maven-archetype-plugin` could
not be resolved (BUILD FAILURE, exit 1). No files were created. Once the
corporate network/mirror was reachable, the retry succeeded.

### Post-scaffold Java 21 alignment (manual)

The user chose to standardize on **Java 21** (both Spring Boot 4 and JavaFX 21
require JDK 17+; JDK 21 is installed). The archetype generated the client with
`maven.compiler.{source,target}=11` and `maven-compiler-plugin` `release 11`
while depending on JavaFX 21 — which cannot compile (source 11 against a Java-17
library). Fixes applied to `clients/desktop-java/pom.xml`:
- `maven.compiler.source` / `maven.compiler.target`: `11` → `21`
- `maven-compiler-plugin` `<release>`: `11` → `21`
- `maven-compiler-plugin` version: `3.8.0` → `3.8.1` (initially, to stay
  compatible with the then-only Maven 3.5.0 — a first try at `3.13.0` failed
  because it requires Maven 3.6.3+)

### Toolchain finalized — Maven 3.9.15 + JDK 21

The user exposed a modern toolchain via environment variables:
- `JAVA_HOME21` → `c:\Program Files\Java\jdk-21.0.11+10`
- `MAVEN_HOME9` → `c:\Program Files\maven\3.9.15` (the message said
  `MAVEN_HOME39`, but the actual variable name on this machine is `MAVEN_HOME9`)

With Maven 3.9.15 available, the `3.8.1` workaround is no longer needed, so
`maven-compiler-plugin` was restored to **`3.13.0`**.

**Verification build (both components, finalized toolchain)**:
`JAVA_HOME="$JAVA_HOME21" "$MAVEN_HOME9/bin/mvn" -B clean compile`
- `clients/desktop-java` → BUILD SUCCESS (compiler 3.13.0, `release 21 module-path`, 3 source files)
- `server` (Spring Boot 4) → BUILD SUCCESS (`release 21`) — confirms Boot 4 runs on JDK 21

## Post-scaffold audit

**Tool**: skipped — no built-in audit tool for java
**Recommended external tool**: OWASP Dependency-Check or Snyk, configured separately against the Maven build.

## Hints recorded but not acted on

| Hint                    | Value           |
| ----------------------- | --------------- |
| bootstrapper_confidence | best-effort     |
| quality_override        | true            |
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
| has_background_jobs     | false           |

## Next steps

Next: a future skill will set up agent context (CLAUDE.md, AGENTS.md). For now,
the JavaFX client is scaffolded and compiles under JDK 21 — happy hacking.

Useful manual steps in the meantime:
- Build/run Java components with the finalized toolchain (JDK 21 + Maven 3.9.15):
  `JAVA_HOME="$JAVA_HOME21" "$MAVEN_HOME9/bin/mvn" -B clean javafx:run` (from `clients/desktop-java`).
- Same pattern builds the Spring core from `server/`.
- Wire the client to the local Spring core's REST API (the core lives in `server/`).
