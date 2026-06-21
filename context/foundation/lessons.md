# Lessons Learned

> Append-only register of recurring rules and patterns. Re-read at start by /10x-frame, /10x-research, /10x-plan, /10x-plan-review, /10x-implement, /10x-impl-review.

## Spring Boot 4 = Jackson 3 (pakiet tools.jackson)

- **Context**: Każdy kod rdzenia Spring (`server/`) dotykający JSON na Spring Boot 4.x / Spring Framework 7 — ObjectMapper, serializacja/deserializacja, kontrolery REST, klienci HTTP.
- **Problem**: Importy `com.fasterxml.jackson.databind.*` (Jackson 2) nie kompilują się ("package does not exist"), bo Boot 4 dostarcza Jackson 3 pod pakietem `tools.jackson.*`. Dodanie `spring-boot-starter-json` nie pomaga (Jackson jest już tranzytywny przez `webmvc` → `starter-jackson`). Kosztowało to dwa nieudane buildy w F-01.
- **Rule**: Na Spring Boot 4.x używaj Jackson 3: importuj `tools.jackson.databind.*` (np. `ObjectMapper`), buduj mapper przez `JsonMapper.builder().build()`, traktuj wyjątki Jacksona jako unchecked (`tools.jackson.core.JacksonException`); adnotacje pozostają w `com.fasterxml.jackson.annotation.*`. Nie dodawaj `spring-boot-starter-json` — Jackson jest tranzytywny przez `spring-boot-starter-webmvc`.
- **Applies to**: plan, implement, plan-review

## Spring Boot 4 = test-slice'y w modularnych pakietach (`spring-boot-<moduł>-test`)

- **Context**: Testy slice'owe w rdzeniu Spring (`server/`) na Spring Boot 4.x — `@WebMvcTest`, `@DataJpaTest` i pokrewne adnotacje autokonfiguracji testów.
- **Problem**: Import `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` (ścieżka z Boot 3) nie kompiluje się na Boot 4 ("package does not exist" / "cannot find symbol: class WebMvcTest"). Boot 4 zmodularyzował test-autoconfigure: zależność to `spring-boot-starter-webmvc-test` (nie monolityczny `spring-boot-starter-test`), a adnotacje przeniesiono. Kosztowało nieudany testCompile w F-01 (faza 3).
- **Rule**: Na Boot 4.x importuj `@WebMvcTest` z `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` (analogicznie inne slice'y z `org.springframework.boot.<moduł>.test.autoconfigure.*`). Klasy z Frameworka (`org.springframework.test.web.servlet.*` — `MockMvc`, `MockMvcRequestBuilders`) zostają na swoich miejscach. Zależność testowa: `spring-boot-starter-webmvc-test`.
- **Applies to**: plan, implement, plan-review

## Spring Boot 4 = Testcontainers 2.x (artefakty z prefiksem `testcontainers-`)

- **Context**: Dodawanie modułów Testcontainers do `server/pom.xml` na Spring Boot 4.x (BOM zarządza Testcontainers **2.0.5**).
- **Problem**: Współrzędne z Testcontainers 1.x — `org.testcontainers:postgresql`, `org.testcontainers:junit-jupiter` — nie istnieją w 2.x → Maven: "'dependencies.dependency.version' ... is missing" (BOM nie ma tych artefaktów, więc wersja nieзаrządzana). Kosztowało nieudany `scanning for projects` w F-02 (faza 1).
- **Rule**: W Testcontainers 2.x moduły mają prefiks `testcontainers-`: `org.testcontainers:testcontainers-postgresql`, `org.testcontainers:testcontainers-junit-jupiter` (i analogicznie inne). Wersje zarządza BOM Spring Boota (bez ręcznego pinowania). Klasy kontenerów zostają w starych pakietach (`org.testcontainers.containers.PostgreSQLContainer` działa); wiązanie z aplikacją przez `spring-boot-testcontainers` + `@ServiceConnection`. `mvn test` z Testcontainers wymaga działającego Dockera.
- **Applies to**: plan, implement, plan-review

## Spring Boot 4 = autokonfiguracja Flyway w osobnym module (`spring-boot-flyway`)

- **Context**: Dowolny `pom.xml` w `server/` na Spring Boot 4.x — każda faza dodająca Flyway lub migracje.
- **Problem**: `flyway-core` obecny w zależnościach, ale Flyway nie odpala żadnej migracji przy starcie (zero linii „Migrating schema" w logach, tabele nie powstają). Boot 4 wydzielił autokonfigurację Flyway z `flyway-core` do osobnego modułu — bez niego `FlywayAutoConfiguration` nie jest rejestrowana. Kosztowało to nieudane testy schematu w F-02 (faza 2).
- **Rule**: W Spring Boot 4.x dodaj `org.springframework.boot:spring-boot-flyway` obok `org.flywaydb:flyway-core` i `flyway-database-postgresql`. Wersją zarządza BOM (bez ręcznego pinowania). Bez tego modułu Flyway jest na ścieżce klas, ale nie uruchamia migracji.
- **Applies to**: plan, implement, plan-review
