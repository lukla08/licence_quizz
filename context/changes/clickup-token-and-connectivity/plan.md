# ClickUp Token + Uwierzytelniona Łączność (F-01) — Implementation Plan

## Overview

Dajemy rdzeniowi Spring sposób na zapisanie osobistego tokenu API ClickUp i wykonanie
uwierzytelnionego wywołania, które potwierdza, że token działa. Token trafia do pliku
ustawień na dysku; łączność weryfikujemy przez `GET /api/v2/user`, zwracając tożsamość
użytkownika albo strukturalny błąd. Całość wystawiona jako serwis domenowy + cienki
kontroler REST. To fundament F-01 — najmniejszy enabler odblokowujący S-01 (pełny pull).

## Current State Analysis

- `server/` to goły szkielet **Spring Boot 4.1.0** (Java 21, Maven), starter `web-mvc`.
  Jedyny kod to `ClickupSimplifierApplication` (samo `main`,
  `server/src/main/java/com/example/clickupsimplifier/ClickupSimplifierApplication.java:7`)
  i domyślny test kontekstu. `application.properties` zawiera tylko
  `spring.application.name=clickup-simplifier`.
- Brak klienta HTTP, konfiguracji, kontrolerów, modelu błędów, warstwy danych.
- **Warstwa danych nie istnieje** (F-02 jeszcze nie zbudowane) → przechowywanie tokenu
  NIE może zależeć od DB.
- PRD `Access Control`: token to jedyny sekret; **bezpieczne (szyfrowane) przechowywanie
  on-device jest świadomie odłożone na downstream** — ten slice przechowuje token
  funkcjonalnie (plaintext w pliku ustawień), nie kryptograficznie.

## Desired End State

Po zakończeniu planu:
- Można zapisać token ClickUp (przez serwis lub `POST` endpoint); token przeżywa restart
  (plik ustawień na dysku).
- Można wywołać sprawdzenie łączności, które zwraca albo tożsamość zalogowanego
  użytkownika ClickUp (id + username), albo strukturalny błąd z czytelnym opisem
  rozróżniający: brak tokenu / token odrzucony (401) / ClickUp nieosiągalny.
- Wszystko pokryte testami z zamockowanym serwerem HTTP (bez realnych wywołań / sekretów).

Weryfikacja: `mvn test` przechodzi; ręczne wpięcie realnego tokenu zwraca tożsamość,
a błędny token zwraca błąd „token odrzucony".

### Key Discoveries:

- Spring Boot 4.1 → użyj **`RestClient`** (nowoczesny synchroniczny klient HTTP); buduj
  z `RestClient.Builder` (testowalne przez `MockRestServiceServer`).
- ClickUp API: baza `https://api.clickup.com/api/v2`; token osobisty leci w nagłówku
  **`Authorization`** wprost (BEZ prefiksu `Bearer`); `GET /user` to kanoniczny test „kim jestem".
- Brak `context/foundation/lessons.md` i `docs/reference/contract-surfaces.md` — brak
  dodatkowych priorów/rejestru nazw do uwzględnienia.

## What We're NOT Doing

- **Szyfrowane / bezpieczne przechowywanie tokenu** (keychain, DPAPI, szyfrowanie pliku) —
  świadomie downstream (PRD Access Control).
- **Pobieranie workspace'ów / jakichkolwiek danych ClickUp** — to S-01.
- **OAuth** — PRD Non-Goal; tylko token osobisty.
- **UI / ekran ustawień** — klient (JavaFX) to F-03; tu tylko rdzeń (serwis + REST).
- **Warstwa danych / DB** — F-02.
- **Wielokrotne sekrety / ogólny moduł ustawień** — dziś sekret jest jeden (token).
- **Logika ponawiania / limity API** — nie na tym etapie (połączenie to pojedynczy strzał).

## Implementation Approach

Trzy fazy, każda samodzielnie testowalna: (1) magazyn ustawień trzymający token na dysku;
(2) klient ClickUp + serwis łączności zwracający strukturalny wynik; (3) cienka powierzchnia
REST nad serwisem. Bazowy URL i ścieżka pliku ustawień są properties z sensownymi domyślnymi,
żeby testy mogły wskazać mock-URL i katalog tymczasowy.

## Critical Implementation Details

- **Nagłówek auth ClickUp** — token osobisty idzie jako `Authorization: <token>` bez
  `Bearer`. To kontrakt, na którym opiera się sprawdzenie łączności; pomyłka tu da fałszywe 401.
- **Rozróżnianie błędów** — 401 z ClickUp = „token odrzucony"; wyjątek I/O (timeout, brak DNS)
  = „ClickUp nieosiągalny"; pusty/brak tokenu = „nie skonfigurowano" (wykryte przed wywołaniem).
- **Build & toolchain (F3)** — domyślny `mvn` (3.5.0) i `JAVA_HOME` (JDK 11) na tej maszynie są
  za stare dla Boot 4.1 / Java 21. Kanoniczna komenda buildu/testów:
  `JAVA_HOME="$JAVA_HOME21" "$MAVEN_HOME9/bin/mvn" -B test` uruchamiana w `server/`. Wszystkie
  kroki „testy przechodzą" odnoszą się do tej komendy.
- **Sekret nigdy w logach (F4)** — token nie trafia do logów, komunikatów wyjątków ani odpowiedzi
  HTTP. `ConnectivityResponse` niesie tylko usera + message; nagłówek `Authorization` nie jest logowany.
- **Bind do loopback (F2)** — serwer nasłuchuje tylko na `127.0.0.1` (`server.address`), bo endpoint
  ustawiający token nie może być osiągalny z sieci (aplikacja lokalna, single-user).

## Phase 1: Magazyn ustawień (token)

### Overview

Trwałe przechowywanie tokenu w pliku JSON na dysku, z konfigurowalną ścieżką i lekką walidacją.

### Changes Required:

#### 1. Properties konfiguracji

**File**: `server/src/main/resources/application.properties`

**Intent**: Wprowadzić konfigurowalne ustawienia z domyślnymi, by reszta kodu i testy nie
zaszywały ścieżek/URL-i.

**Contract**: Dodaj `clickup.settings-file` (domyślnie `${user.home}/.clickup-simplifier/settings.json`)
oraz `clickup.api.base-url` (domyślnie `https://api.clickup.com/api/v2`). Dodaj też
`server.address=127.0.0.1` (F2 — serwer tylko lokalnie; endpoint tokenu nieosiągalny z sieci).

#### 2. Obiekt konfiguracji

**File**: `server/src/main/java/com/example/clickupsimplifier/config/ClickupProperties.java`

**Intent**: Typowany dostęp do powyższych properties.

**Contract**: `@ConfigurationProperties(prefix = "clickup")` z polami `settingsFile` i
zagnieżdżonym `api.baseUrl`. Zarejestrowany przez `@ConfigurationPropertiesScan` lub
`@EnableConfigurationProperties`.

#### 3. Magazyn ustawień / tokenu

**File**: `server/src/main/java/com/example/clickupsimplifier/settings/SettingsStore.java`

**Intent**: Zapis i odczyt tokenu z pliku JSON; tworzy katalog/plik gdy brak; lekka walidacja
(odrzuca pusty/whitespace token). O ważności tokenu decyduje dopiero ClickUp (Faza 2).

**Contract**: Metody `void saveToken(String token)` (rzuca przy pustym), `Optional<String> getToken()`.
Serializacja przez Jacksona (obecny w web-mvc) do prostego rekordu `Settings(String clickupToken)`.
Zapis **atomowy** (F5): zapis do pliku tymczasowego + rename/move-replace, by awaria w trakcie
zapisu nie uszkodziła `settings.json` i nie zgubiła tokenu.

### Success Criteria:

#### Automated Verification:

- Testy przechodzą wg toolchainu (JAVA_HOME21 + MAVEN_HOME9), w `server/` — patrz Critical Implementation Details
- Test odczyt-po-zapisie: `saveToken` → `getToken` zwraca ten sam token (plik w katalogu tymczasowym)
- Test walidacji: `saveToken("")` / whitespace rzuca wyjątek; plik nie powstaje
- `getToken()` na braku pliku zwraca `Optional.empty()`
- Test atomowości: po zapisie nie zostaje plik tymczasowy; istniejący `settings.json` nie jest tracony przy ponownym zapisie

#### Manual Verification:

- Po zapisie token jest obecny w pliku JSON pod skonfigurowaną ścieżką
- Plik przeżywa restart aplikacji (token odczytany ponownie)

**Implementation Note**: Po zakończeniu fazy i przejściu automatycznej weryfikacji zatrzymaj się
na ręczne potwierdzenie, zanim ruszysz do Fazy 2.

---

## Phase 2: Klient ClickUp + sprawdzenie łączności

### Overview

`RestClient` skonfigurowany pod ClickUp + serwis łączności wołający `GET /user` i zwracający
strukturalny wynik.

### Changes Required:

#### 1. Bean RestClient

**File**: `server/src/main/java/com/example/clickupsimplifier/clickup/ClickupClientConfig.java`

**Intent**: Dostarczyć `RestClient.Builder`/`RestClient` z bazowym URL z `ClickupProperties`.
Token nie jest wpinany na stałe (jest per-wywołanie, bo może się zmienić w ustawieniach).

**Contract**: Bean `RestClient` (lub `RestClient.Builder`) z `baseUrl(properties.api.baseUrl)`.
Pozostawić builder testowalnym przez `MockRestServiceServer`.

#### 2. Klient ClickUp (GET /user)

**File**: `server/src/main/java/com/example/clickupsimplifier/clickup/ClickupClient.java`

**Intent**: Wykonać uwierzytelnione `GET /user` z nagłówkiem `Authorization: <token>` i
zmapować odpowiedź na tożsamość.

**Contract**: `ClickupUser getCurrentUser(String token)`, gdzie `ClickupUser(String id, String username)`
mapuje pole `user` z odpowiedzi ClickUp. Nagłówek `Authorization` = surowy token (bez `Bearer`).

#### 3. Serwis łączności + wynik

**File**: `server/src/main/java/com/example/clickupsimplifier/clickup/ConnectivityService.java`

**Intent**: Połączyć magazyn tokenu (Faza 1) z klientem; zwrócić strukturalny wynik
rozróżniający sukces i klasy błędów.

**Contract**: `ConnectivityResult checkConnectivity()`. `ConnectivityResult` to typ sumacyjny
(np. `record` ze statusem `OK | NOT_CONFIGURED | TOKEN_REJECTED | UNREACHABLE`, opcjonalnym
`ClickupUser` i czytelnym `message`). Mapowanie: brak tokenu → `NOT_CONFIGURED`; 401 →
`TOKEN_REJECTED`; `RestClientException`/I/O → `UNREACHABLE`.

### Success Criteria:

#### Automated Verification:

- Testy przechodzą wg toolchainu (JAVA_HOME21 + MAVEN_HOME9), w `server/`
- Test sukcesu (MockRestServiceServer zwraca 200 + JSON usera): wynik `OK` z poprawnym id/username; wysłany nagłówek `Authorization` = token (bez `Bearer`), trafiony `GET /user`
- Test 401: wynik `TOKEN_REJECTED` z czytelnym opisem
- Test braku tokenu: wynik `NOT_CONFIGURED` (bez ruchu sieciowego)
- Test błędu sieci (mock rzuca/timeout): wynik `UNREACHABLE`

#### Manual Verification:

- Realny prawidłowy token → `OK` z tożsamością
- Celowo błędny token → `TOKEN_REJECTED`

**Implementation Note**: Po automatycznej weryfikacji zatrzymaj się na ręczne potwierdzenie przed Fazą 3.

---

## Phase 3: Powierzchnia REST + spięcie

### Overview

Cienki kontroler nad serwisem: ustawienie tokenu i sprawdzenie łączności przez HTTP.

### Changes Required:

#### 1. Kontroler REST

**File**: `server/src/main/java/com/example/clickupsimplifier/clickup/SettingsController.java`

**Intent**: Wystawić dwie operacje: zapis tokenu i sprawdzenie łączności — używalne przez
klientów HTTP (web/flutter), a serwis pozostaje wołalny in-process przez JavaFX.

**Contract**:
- `PUT /api/settings/clickup-token` z ciałem `{ "token": "..." }` → 204 przy sukcesie,
  400 przy pustym tokenie.
- `GET /api/clickup/connectivity` → 200 z ciałem odwzorowującym `ConnectivityResult`
  (status + opcjonalny user + message). Mapowanie statusów na odpowiedź: `OK` → 200;
  `NOT_CONFIGURED`/`TOKEN_REJECTED`/`UNREACHABLE` → 200 z polem statusu (klient czyta status
  z ciała; brak twardego mapowania na kody 4xx/5xx, by panel sync mógł pokazać opis).

#### 2. DTO żądania/odpowiedzi

**File**: `server/src/main/java/com/example/clickupsimplifier/clickup/dto/` (rekordy)

**Intent**: Jawny kontrakt JSON wejścia/wyjścia, odseparowany od typów domenowych.

**Contract**: `SetTokenRequest(String token)`, `ConnectivityResponse(String status, ClickupUser user, String message)`.

### Success Criteria:

#### Automated Verification:

- Testy przechodzą wg toolchainu (JAVA_HOME21 + MAVEN_HOME9), w `server/`
- Test slice'owy kontrolera (`@WebMvcTest` z zamockowanym serwisem): `PUT` poprawnego tokenu → 204; pusty token → 400
- `GET /api/clickup/connectivity` zwraca 200 z polem `status` odwzorowującym wynik serwisu (np. `OK` z userem; `TOKEN_REJECTED` z message)

#### Manual Verification:

- `PUT` tokenu a potem `GET` connectivity przez curl/klienta REST zwraca tożsamość
- Brak regresji: aplikacja startuje czysto (`spring-boot:run` przez toolchain jw.), istniejący test kontekstu nadal przechodzi

**Implementation Note**: Po automatycznej weryfikacji zatrzymaj się na ręczne potwierdzenie domknięcia F-01.

---

## Testing Strategy

### Unit Tests:

- `SettingsStore`: odczyt-po-zapisie, walidacja pustego tokenu, brak pliku → empty (katalog tymczasowy)
- `ConnectivityService`/`ClickupClient`: ścieżki OK / 401 / brak-tokenu / sieć, z `MockRestServiceServer`
- Asercja kontraktu: nagłówek `Authorization` = surowy token, trafiony `GET /user`

### Integration Tests:

- Test slice'owy kontrolera (`@WebMvcTest`) dla obu endpointów z zamockowanym `ConnectivityService`

### Manual Testing Steps:

1. Ustaw realny token (`PUT /api/settings/clickup-token`), sprawdź plik ustawień na dysku
2. `GET /api/clickup/connectivity` → oczekuj `OK` + Twoja tożsamość ClickUp
3. Ustaw celowo błędny token → oczekuj `TOKEN_REJECTED` z opisem
4. Restart aplikacji → token nadal obecny (plik przeżył)

## Performance Considerations

Brak istotnych — pojedyncze wywołanie HTTP na żądanie. Bez pętli/wsadów (to S-01/S-02).

## Migration Notes

Brak istniejących danych do migracji. Pierwszy kod funkcjonalny w rdzeniu.

## References

- Roadmap: `context/foundation/roadmap.md` → F-01 (`clickup-token-and-connectivity`)
- PRD: `context/foundation/prd.md` → FR-001, `## Access Control`
- Tech-stack: `context/foundation/tech-stack.md` (Spring Boot 4.1, Maven, self-host)
- Baseline: `server/src/main/java/com/example/clickupsimplifier/ClickupSimplifierApplication.java:7`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Magazyn ustawień (token)

#### Automated

- [x] 1.1 Testy przechodzą wg toolchainu (JAVA_HOME21 + MAVEN_HOME9), w `server/` — a380fb3
- [x] 1.2 Test odczyt-po-zapisie: `saveToken` → `getToken` zwraca ten sam token — a380fb3
- [x] 1.3 Test walidacji: pusty/whitespace token rzuca; plik nie powstaje — a380fb3
- [x] 1.4 `getToken()` bez pliku zwraca `Optional.empty()` — a380fb3
- [x] 1.5 Test atomowości: brak pliku tymczasowego po zapisie; istniejący settings.json nietracony — a380fb3

#### Manual

- [x] 1.6 Token obecny w pliku JSON pod skonfigurowaną ścieżką — a380fb3
- [x] 1.7 Plik przeżywa restart aplikacji — a380fb3

### Phase 2: Klient ClickUp + sprawdzenie łączności

#### Automated

- [ ] 2.1 Testy przechodzą wg toolchainu (JAVA_HOME21 + MAVEN_HOME9), w `server/`
- [ ] 2.2 Test sukcesu: 200 → `OK` z id/username; `Authorization` = token bez `Bearer`, trafiony `GET /user`
- [ ] 2.3 Test 401 → `TOKEN_REJECTED` z opisem
- [ ] 2.4 Test braku tokenu → `NOT_CONFIGURED` bez ruchu sieciowego
- [ ] 2.5 Test błędu sieci → `UNREACHABLE`

#### Manual

- [ ] 2.6 Realny prawidłowy token → `OK` z tożsamością
- [ ] 2.7 Błędny token → `TOKEN_REJECTED`

### Phase 3: Powierzchnia REST + spięcie

#### Automated

- [ ] 3.1 Testy przechodzą wg toolchainu (JAVA_HOME21 + MAVEN_HOME9), w `server/`
- [ ] 3.2 Test kontrolera: `PUT` poprawnego tokenu → 204; pusty → 400
- [ ] 3.3 `GET /api/clickup/connectivity` → 200 z polem `status` odwzorowującym wynik serwisu

#### Manual

- [ ] 3.4 `PUT` tokenu + `GET` connectivity przez curl zwraca tożsamość
- [ ] 3.5 Aplikacja startuje czysto; domyślny test kontekstu nadal przechodzi
