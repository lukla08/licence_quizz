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
