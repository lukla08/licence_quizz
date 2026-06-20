# Lessons Learned

> Append-only register of recurring rules and patterns. Re-read at start by /10x-frame, /10x-research, /10x-plan, /10x-plan-review, /10x-implement, /10x-impl-review.

## Spring Boot 4 = Jackson 3 (pakiet tools.jackson)

- **Context**: Każdy kod rdzenia Spring (`server/`) dotykający JSON na Spring Boot 4.x / Spring Framework 7 — ObjectMapper, serializacja/deserializacja, kontrolery REST, klienci HTTP.
- **Problem**: Importy `com.fasterxml.jackson.databind.*` (Jackson 2) nie kompilują się ("package does not exist"), bo Boot 4 dostarcza Jackson 3 pod pakietem `tools.jackson.*`. Dodanie `spring-boot-starter-json` nie pomaga (Jackson jest już tranzytywny przez `webmvc` → `starter-jackson`). Kosztowało to dwa nieudane buildy w F-01.
- **Rule**: Na Spring Boot 4.x używaj Jackson 3: importuj `tools.jackson.databind.*` (np. `ObjectMapper`), buduj mapper przez `JsonMapper.builder().build()`, traktuj wyjątki Jacksona jako unchecked (`tools.jackson.core.JacksonException`); adnotacje pozostają w `com.fasterxml.jackson.annotation.*`. Nie dodawaj `spring-boot-starter-json` — Jackson jest tranzytywny przez `spring-boot-starter-webmvc`.
- **Applies to**: plan, implement, plan-review
