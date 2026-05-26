# Backend — kolejność dodawania zależności (Spring Boot)

Dotyczy: `licence-quizz-api/` (Spring Boot 4.0.6 / Java 21 / Maven). Scaffold startowy ma tylko `web` (`spring-boot-starter-webmvc`) + `devtools` + test — zależności pod auth i dane **nie są** dodane celowo.

## Zasada

Dodawaj każdą zależność dokładnie wtedy, gdy zaczynasz implementować pierwszą funkcję, która jej wymaga (just-in-time) — nie wszystkie spekulacyjnie na zapas. Powód: np. Spring Security domyślnie blokuje wszystkie endpointy, więc dodany przedwcześnie utrudnia pracę, zanim jest co chronić.

## Gdzie w cyklu projektu

Łańcuch foundation (shape → prd → tech-stack → bootstrap) jest zakończony. Dodawanie tych bibliotek to praca implementacyjna nad pierwszą funkcją, nie kolejny krok foundation — robisz to, budując pierwszy pionowy plaster backendu.

## Mapa: zależność → funkcja, która ją wymusza

| Zależność | Dodaj, gdy implementujesz | Powiązane FR |
|---|---|---|
| Spring Data JPA + sterownik PostgreSQL | pierwszą trwałą encję (pytania / użytkownicy / tagi) | FR-003, FR-005, FR-010 |
| Migracje (Flyway lub Liquibase) | pierwszy schemat bazy (razem z JPA) | — |
| Spring Security | logowanie / rejestrację | FR-001, FR-002 |
| Bean Validation (`spring-boot-starter-validation`) | pierwsze DTO z walidacją żądań | FR-001, FR-003 |

## Rekomendowana kolejność pierwszego pionowego plastra

1. **JPA + PostgreSQL + migracje** — postaw bazę i jedną encję end-to-end (np. `Question`), bo prawie wszystko inne na niej polega.
2. **Security** — dopiero gdy masz co chronić, wepnij auth (FR-001/002); izolacja danych per-konto (guardrail z PRD) wisi właśnie tu.
3. **Validation** — utwardź endpointy, gdy już działają.

## Jak technicznie dodać

Edytuj `<dependencies>` w `licence-quizz-api/pom.xml` (albo wygeneruj nowy zestaw na start.spring.io i scal), potem `./mvnw` pobiera je przy następnym buildzie. Każdy `spring-boot-starter-*` przyciąga spójną wersję przez `spring-boot-starter-parent` — nie podajesz numerów wersji ręcznie.

## Uwaga blokująca

Zanim dodasz JPA, rozstrzygnij otwarte pytanie z PRD o **hosting wideo** (zewnętrznie/CDN vs w bazie) — wpływa na model danych pytania.
