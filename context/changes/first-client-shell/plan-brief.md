# Monolit JavaFX — moduły core+ui + powłoka klienta (F-03) — Plan Brief

> Full plan: `context/changes/first-client-shell/plan.md`

## What & Why

Realizujemy fundament F-03 po rewizji architektury z 2026-06-25: rezygnacja z
multi-client / klient-serwer na rzecz **monolitu desktopowego pod JavaFX**.
Przebudowujemy repo w dwumodułowy reaktor Maven (`core` + `ui`), usuwamy warstwę
REST i wpinamy powłokę JavaFX wołającą `core` **in-process** — dowodząc, że
JavaFX↔Spring działa bez kontraktu sieciowego. To odblokowuje całą ścieżkę UI
(S-03 → gwiazda S-04 → S-05..S-09).

## Starting Point

`server/` to działający Spring Boot 4.1.0 (webmvc/Tomcat) z czystymi beanami
domeny (`ConnectivityService`, `WorkspaceSyncService`, `SettingsStore`), Spring
Data JDBC + Flyway V1–V3 (Postgres) i ~40 zielonymi testami. Jedyna warstwa
„serwerowa" to 3 cienkie kontrolery REST + ich DTO. `clients/desktop-java` to goły
scaffold JavaFX; `clients/{flutter,web}` to puste scaffoldy.

## Desired End State

Reaktor `pom.xml` + `core/` + `ui/`. `core` = dotychczasowy kod bez REST i bez
Tomcata (kontekst non-web), z cienką warstwą aplikacyjną (fasady) zwracającą DTO
widoku in-process. `ui` = aplikacja JavaFX startująca z classpath, bootująca
kontekst Spring i pokazująca stan łączności z ClickUp w nawigowalnej klawiaturą
powłoce. Brak `clients/`.

## Key Decisions Made

| Decyzja | Wybór | Dlaczego | Source |
| --- | --- | --- | --- |
| Architektura MVP | Monolit JavaFX, moduły `core`+`ui` | Rezygnacja z multi-client/serwera; granica `ui→core` = przyszła granica serwer↔klient | Pivot 2026-06-25 |
| Spring | Zostaje jako kontener DI in-process | Cały zreviewowany kod F-01/F-02/S-01 przeżywa; znika tylko warstwa sieciowa | Pivot 2026-06-25 |
| Baza | Postgres zostaje | Zero przepisywania; embedded odłożone | Pivot 2026-06-25 |
| Strip web | Pełne usunięcie `-webmvc`, jawny `spring-web` | Czysty graf, zero Tomcata; RestClient zachowany | Plan |
| Zakres powłoki | Odczyt connectivity in-process | Minimalny realny dowód kontraktu ui→core, bez wchodzenia w S-03 | Plan |
| Uruchamianie | Classpath + `Launcher`, dev-run | Omija ból Spring+JPMS; pakowanie odłożone | Plan |
| Klawiatura | Mały szkielet akceleratorów/fokusu | Ustanawia wzorzec NFR wcześnie, tanio | Plan |
| Testy GUI | TestFX (headless/Monocle) | Automatyczna weryfikacja powłoki i klawiatury | Plan |
| Los DTO | Zachowane jako typy widoku warstwy aplikacji | Granica/anti-corruption gdyby serwer wrócił; producent = fasady | Plan |

## Scope

**In scope:** reaktor Maven `core`+`ui`; przeniesienie `server/`→`core`; usunięcie
`clients/{flutter,web,desktop-java}`; zdjęcie webmvc/Tomcata + kontrolerów; fasady
aplikacyjne zwracające DTO; powłoka JavaFX z odczytem łączności; szkielet
klawiatury; TestFX headless.

**Out of scope:** nawigacja milestone→task (S-03); UI tokenu i wyzwalania syncu;
pakowanie (jlink/jpackage/fat-jar); zmiany schematu/logiki sync/integracji ClickUp;
pełny system keybindingów; edycja `context/archive/`.

## Architecture / Approach

Parent pom (packaging=pom, parent=`spring-boot-starter-parent`) agreguje `core` i
`ui`. `core` trzyma domenę/persistencję/sync/integrację + warstwę aplikacyjną
(fasady); kontekst rozwiązuje się jako `WebApplicationType.NONE`. `ui` zależy od
`core`: `Launcher` (classpath, nie-`Application`) startuje `App extends Application`,
która w `init()` buduje kontekst Spring, w `start()` pokazuje scenę z beana widoku
wołającego fasadę łączności na wątku tła, w `stop()` zamyka kontekst. Kompilator
wymusza `ui → core`.

## Phases at a Glance

| Faza | Co dostarcza | Główne ryzyko |
| --- | --- | --- |
| 1. Reaktor + przeniesienie `core` | Dwumodułowy reaktor, `server/`→`core`, kasacja flutter/web, pusty `ui`; testy zielone bez zmiany zachowania | Przeniesienie psuje wiring Flyway/Testcontainers/Jackson (lessons.md) |
| 2. Usunięcie web + fasady | Brak webmvc/Tomcata, kontekst non-web, fasady zwracające DTO, testy fasad zamiast kontrolerów | RestClient/Jackson przestają się autokonfigurować bez startera web |
| 3. Powłoka JavaFX + TestFX | `ui` bootuje Spring, czyta łączność in-process, szkielet klawiatury, test headless | FX↔Spring lifecycle; JPMS-vs-classpath; headless Monocle w CI |

**Prerequisites:** F-01/F-02/S-01 w kodzie (są); JDK 21 + Maven 3.9.x
(`$JAVA_HOME21`/`$MAVEN_HOME9`); Docker dla testów Testcontainers; lokalny Postgres
(5444) do ręcznego uruchomienia.
**Estimated effort:** ~3 sesje (po jednej na fazę).

## Open Risks & Assumptions

- **RestClient bez startera web** — założenie: `spring-web` + `spring-boot-starter`
  wystarczą do autokonfiguracji `RestClient.Builder`. Weryfikowane bramką Fazy 2.
- **Headless TestFX** — Monocle bywa wrażliwy na wersje JavaFX/JDK; może wymagać
  dostrojenia property surefire.
- **FX↔Spring lifecycle** — wzorzec Launcher+init/start/stop jest standardowy, ale
  kolejność zamykania kontekstu vs `Platform.exit()` wymaga uwagi (wiszące wątki).
- Założenie: pakiet bazowy `core` pozostaje `com.example.clickupsimplifier` (zero
  churn importów); `ui` pod `...ui`.

## Success Criteria (Summary)

- `mvn -B -f pom.xml test` w korzeniu reaktora przechodzi (core + fasady + TestFX headless).
- `mvn -pl ui javafx:run` pokazuje okno z poprawnym stanem łączności, czytanym z `core` in-process.
- Aplikacja startuje bez nasłuchu na porcie HTTP; klawiatura przewidywalna (brak skoków fokusu).
