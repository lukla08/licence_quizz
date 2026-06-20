# Repository Guidelines

JavaFX desktop client for ClickUp Simplifier — native JVM UI calling the local Spring core over REST. Repo-wide rules: @AGENTS.md at the repo root.

## Local Rules

- UI only — no sync engine, write-back queue, or ClickUp API calls in this tree.
- Every user-facing action must be keyboard-reachable; focus moves only on direct user action.
- Never store, log, or display the ClickUp token in Java source or local config files.

## Adding a UI Unit

Add classes under `src/main/java/com/example/`, following @./src/main/java/com/example/App.java: subclass `Application`, build scenes in code, `main` calls `launch()`. Register new JavaFX modules in @./src/main/java/module-info.java when adding beyond `javafx.controls`.

## File Layout & Naming

- `src/main/java/com/example/` — application classes (`PascalCase.java`)
- `module-info.java` — JPMS module `com.example`, exports application package
- @pom.xml — Java **21**, JavaFX **21**, `javafx-maven-plugin` main class `com.example.App`

UI is code-first (`Scene`/`Stage` in Java), not FXML-driven.

## Build & Run

From repo root use @../../scripts/README.md: `scripts/build-desktop.sh`, `scripts/run-desktop.sh` (need `JAVA_HOME21`, `MAVEN_HOME9`).

## Testing

No `src/test/` yet. When added, use JUnit 5 under `src/test/java/com/example/` and run via `scripts/build-desktop.sh clean test`.

## Tripwires

- Do not call ClickUp HTTP APIs directly — the Spring core owns integration.
- Do not add JavaFX modules without updating `module-info.java` `requires` clauses.
- Do not target Java below 21 — @pom.xml sets `release 21` and depends on JavaFX 21.
