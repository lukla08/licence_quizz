# Repository Guidelines

Spring Boot 4.0.6 / Java 21 REST API backing the LicenceQuizz multi-client app. Maven wrapper (`./mvnw`) is the only supported build tool — do not invoke system `mvn` directly.

## Hard Rules

- Each user's quiz results, tags, and error history must never leak to another account. Enforce isolation at the service layer with per-account queries (not just at the API boundary).
- Use `${ENV_VAR_NAME}` placeholders in `application.properties`; actual values live in the Render environment dashboard. Never hardcode credentials or API keys.

## Package Structure

Place each feature under `com.example.licencequizz.<feature>` (e.g., `auth`, `quiz`, `question`). A feature package holds its own controller, service, repository, and DTOs. Do not add classes directly to `com.example.licencequizz`.

## Build, Test, and Run Commands

- `./mvnw spring-boot:run` — start the dev server
- `./mvnw test` — run all tests
- `./mvnw test -Dtest=ClassName` — run a single test class
- `./mvnw package -DskipTests` — build the deployable JAR

## Testing

Tests live under `src/test/java/com/example/licencequizz/`. See `@src/test/java/com/example/licencequizz/LicenceQuizzApiApplicationTests.java` as the bootstrap reference.

## Deployment

GitHub Actions runs `./mvnw test` on every push; merge to `master` triggers an auto-deploy to Render. A failing test blocks the deploy — do not skip tests in CI.
