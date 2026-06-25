---
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
---

## Architecture revision (2026-06-25): JavaFX monolith

**Decyzja użytkownika (2026-06-25):** dla MVP **rezygnujemy z wielu klientów i z
wydzielonego serwera**. Budujemy **monolit desktopowy pod JavaFX** — jeden
uruchamialny artefakt, bez serwera HTTP, bez kontraktu sieciowego klient↔rdzeń.
Granice dziedzin pozostają wydzielone, a podział na warstwy zachowany; do
rozbudowanej (klient/serwer, wielo-klient) wersji być może wrócimy później.

Konkretne ustalenia (wszystkie potwierdzone 2026-06-25):

- **Jeden klient: JavaFX.** Szkielety `clients/flutter` i `clients/web` wypadają
  z zakresu MVP. Stara kolejność klientów (OQ-1: JavaFX → Flutter → web) jest
  nieaktualna.
- **Brak wydzielonego serwera.** Usuwamy `spring-boot-starter-webmvc` (Tomcat) i
  warstwę `@RestController`. **Spring zostaje jako kontener DI in-process**
  (`spring-boot-starter` + `data-jdbc` + Flyway); JavaFX startuje kontekst Springa
  i pobiera z niego beany (wzorzec FX-Spring). Cały zreviewowany kod F-01/F-02/S-01
  przeżywa — znika tylko warstwa sieciowa, którą UI zastępuje wywołaniem
  in-process.
- **Dwa moduły Maven: `core` + `ui`.** Parent pom agreguje oba. `core` =
  „przyszły serwer" (clickup, workspace/persistencja, sync, settings, config);
  `ui` = JavaFX, zależy od `core`, posiada `@SpringBootApplication` + `main()`.
  Kompilator wymusza granicę `ui → core` (nigdy odwrotnie) — to dokładnie
  przyszła granica serwer↔klient. Nadal jeden uruchamialny artefakt, więc
  charakter monolitu zachowany.
- **Baza: Postgres zostaje** (port 5444, Flyway V1–V3, Testcontainers). Lekki
  zgrzyt z monolitem desktopowym (osobny proces bazy), ale zero przepisywania;
  embedded (H2/SQLite) świadomie odłożone.

Refaktor kodu pod tę architekturę (usunięcie warstwy REST, scalenie
`server/` + `clients/desktop-java` w `core` + `ui`) to **nowa zmiana przez
`/10x-new`** — F-01/F-02/S-01 są zarchiwizowane i niemodyfikowalne.

## Why this stack (oryginał — częściowo zdezaktualizowany przez rewizję powyżej)

ClickUp Simplifier is a single-user, local-first tool: a shared core (local
copy, sync engine, domain model) with swappable frontends and no hosted server —
the only remote is ClickUp's API via a personal token. This hand-off scaffolds
that core, which the user fixed as Java/Spring; clients (native web, Flutter,
native Java desktop) are deferred to later, separate bootstraps. Spring is also
the registry's recommended default for the (backend/API, Java) cell and clears
all four agent-friendly gates with `verified` bootstrapper confidence, so
scaffolding will be smooth. It runs locally on-device (self-host), not in the
cloud, which is why the cloud-oriented JS web default was rejected as an
architectural mismatch. Background jobs is the one feature flag set — the named
sync sets run on recurring per-set cadences (FR-003, FR-019); auth is false
(single user, no login; the API token is secret storage, not app auth), and
payments, realtime, and AI are out of scope. CI on GitHub Actions with manual
artifact promotion fits a local desktop app with no remote deploy target. The
five-point self-check came back clean across all points, so no Socratic nudge
fired.

> **Uwaga:** zdanie „swappable frontends … clients deferred to later, separate
> bootstraps" jest zastąpione przez rewizję z 2026-06-25 (monolit JavaFX, jeden
> klient). Reszta uzasadnienia (Java/Spring, self-host, brak chmury, background
> jobs dla sync, brak auth/payments/realtime/AI) nadal obowiązuje.
