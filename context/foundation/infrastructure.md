---
project: licence-quizz-api
researched_at: 2026-05-27
recommended_platform: localhost (local development)
runner_up: Railway (deferred — cloud deployment not yet needed)
context_type: mvp
tech_stack:
  language: java
  framework: spring-boot-4.0.6
  runtime: jvm-21
  build_tool: maven
deployment_status: local-only
---

## Recommendation

**Uruchamiaj lokalnie przez `./mvnw spring-boot:run`.**

Projekt jest w fazie wczesnego MVP, skierowany do małej, zamkniętej grupy użytkowników (znajomi/rodzina). Zdalne wdrożenie jest odłożone — aktualnie liczy się szybkość iteracji na lokalnej maszynie. Gdy zajdzie potrzeba wdrożenia, preferowaną platformą cloud jest **Railway** (natywna obsługa Java/Maven via Railpack, oficjalny MCP server z integracją Claude Code, $5/month Hobby bez sleep).

---

## Lokalne uruchomienie

### Wymagania

- Java 21 (zweryfikuj: `java -version`)
- Maven Wrapper dołączony do repo — nie trzeba instalować Mavena globalnie

### Uruchomienie w trybie development

```bash
cd licence-quizz-api
./mvnw spring-boot:run
```

`spring-boot-devtools` jest na classpath (`runtime` scope) — zmiany klas i zasobów przeładowują aplikację automatycznie bez restartu JVM.

Aplikacja nasłuchuje na `http://localhost:8080`.

### Budowanie fat JAR

```bash
./mvnw clean package -DskipTests
java -jar target/licence-quizz-api-0.0.1-SNAPSHOT.jar
```

### Uruchomienie testów

```bash
./mvnw test
```

---

## Operational Story (lokalny tryb)

- **Przeglądanie zmian**: localhost:8080 — DevTools przeładowuje automatycznie po zapisie pliku; pełny restart przy zmianach konfiguracji Spring context
- **Sekrety / env vars**: `src/main/resources/application.properties` lub `application-local.properties` (nie commitować haseł do repo)
- **Rollback**: `git checkout <commit>` + `./mvnw spring-boot:run` — powrót do poprzedniej wersji w <30 sekund
- **Zatwierdzenie do produkcji**: ręczny krok — deployment cloud nie jest jeszcze skonfigurowany
- **Logi**: stdout w terminalu; DevTools wyświetla hot-reload events; Spring Boot Actuator można dodać dla `/actuator/health`

---

## Stan pom.xml na dzień decyzji

Projekt jest na etapie szkieletu — w `pom.xml` brakuje jeszcze zależności, które będą wymagane przez PRD:

| Zależność | Status | Wymagana przez |
|---|---|---|
| `spring-boot-starter-webmvc` | ✅ obecna | routing REST |
| `spring-boot-devtools` | ✅ obecna | hot reload |
| `spring-boot-starter-data-jpa` | ❌ brak | historia błędów, tagi, sesje |
| `spring-boot-starter-security` | ❌ brak | auth (FR-001, FR-002) |
| Driver bazy danych (PostgreSQL) | ❌ brak | trwałość danych |

Przed implementacją auth i quizu należy dodać te trzy zależności.

---

## Cloud deployment — odłożone

Gdy będzie potrzebne wdrożenie zdalne:

### Rekomendowana platforma: Railway

**Dlaczego Railway:**
- Railpack auto-detects `pom.xml` → Spring Boot — brak potrzeby pisania Dockerfile
- Oficjalny MCP server (GA) z integracją Claude Code: `claude mcp add railway-mcp-server -- npx -y @railway/mcp-server`
- `$5/month` Hobby (bez sleep) vs $7/month Render
- `railway up` → deploy, `railway logs` → logi, `railway rollback` → rollback

**Pierwsze kroki gdy zajdzie potrzeba:**

```bash
npm install -g @railway/cli
railway login
railway init
railway up
```

**Obowiązkowe przed pierwszym deplojem:**

Dodaj do `src/main/resources/application.properties`:
```properties
server.port=${PORT:8080}
```

Bez tej linii Railway wstrzyknie dynamiczny port, Spring Boot zbinduje 8080, health check będzie failować.

### Runner-up: Render

Alternatywa jeśli Railway nie będzie dostępne — oficjalny MCP server (GA sierpień 2025), CLI z `render logs` i rolback, $7/month always-on. Docker lub buildpack.

---

## Risk Register

| Ryzyko | Źródło | Prawdopodob. | Wpływ | Mitigacja |
|---|---|---|---|---|
| Lokalne środowisko ≠ produkcja (brak bazy, inny PORT) | Unknown unknowns | H | M | Testuj z lokalną bazą PostgreSQL od początku, nie in-memory H2 |
| Spring Boot 4.0.6 — mała liczba community examples (nowa major wersja) | Research finding | M | M | Weryfikuj składnię w oficjalnych docs Spring Boot 4.x, nie tutorialach dla 3.x |
| Brak `spring-boot-starter-security` w pom.xml przy planowanej auth | Research finding | H | H | Dodaj security dependency zanim zaczniesz implementację auth (FR-001) |
| Committing haseł do `application.properties` | Devil's advocate | M | H | Użyj `application-local.properties` w `.gitignore` na sekrety lokalne |
| Zmiana PORT przy przejściu na Railway (jeśli zapomnisz `${PORT:8080}`) | Pre-mortem | H | M | Dodaj `server.port=${PORT:8080}` do properties już teraz — koszt: 1 linia |

---

## Out of Scope

Poniższe tematy nie były przedmiotem tej decyzji:
- Konfiguracja Docker / Docker Compose
- Konfiguracja CI/CD pipeline (GitHub Actions)
- Architektura produkcyjna (multi-region, HA, DR)
- Wybór konkretnej bazy danych i ORM (osobna decyzja przy dodawaniu JPA)
