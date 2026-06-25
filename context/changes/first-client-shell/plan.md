# Monolit JavaFX — moduły core+ui + powłoka klienta (F-03) Implementation Plan

## Overview

Przebudowujemy repozytorium w **dwumodułowy monolit Maven** (`core` + `ui`) zgodnie
z rewizją architektury z 2026-06-25. `server/` staje się modułem `core`; warstwa
REST (`@RestController` + webmvc/Tomcat) znika, a jej mapowanie domena→DTO
przenosi się do **warstwy aplikacyjnej** w `core`, wołanej in-process. Powstaje
moduł `ui` (JavaFX), który startuje kontekst Spring, woła `core` bez kontraktu
sieciowego i pokazuje stan łączności z ClickUp w pustej, ale nawigowalnej
klawiaturą powłoce. Szkielety `clients/flutter` i `clients/web` są usuwane.

## Current State Analysis

- **`server/`** to standalone Spring Boot 4.1.0 (`spring-boot-starter-webmvc`,
  Tomcat), Spring Data JDBC + Flyway V1–V3 (Postgres), Testcontainers. Maven
  parent = `spring-boot-starter-parent`. Pakiet bazowy `com.example.clickupsimplifier`.
- **Beany rdzenia są już czyste** (zero zależności od HTTP): `ConnectivityService.checkConnectivity()`
  (`server/.../clickup/ConnectivityService.java:22`), `WorkspaceSyncService`
  (`tryClaimRunning()` + `@Async triggerPull(since)` + `getStatus()`,
  `.../sync/WorkspaceSyncService.java:78`), `SettingsStore` (token JSON,
  `.../settings/SettingsStore.java:40`).
- **Jedyna warstwa „serwerowa"** to 3 kontrolery i ich DTO:
  - `clickup/SettingsController.java` → `dto/SetTokenRequest`, `dto/ConnectivityResponse`
  - `sync/SyncController.java` → `dto/SyncStatusResponse`
  - `sync/ListController.java` → `dto/ListResponse`, `dto/SyncEnabledRequest`
  Mapowanie domena→DTO w kontrolerach jest cienkie i mechaniczne (np.
  `SettingsController.java:36`, `SyncController.java:39`, `ListController.java:27`).
- **RestClient (wychodzący do ClickUp)** żyje w `spring-web` — dziś tranzytywnie
  przez `-webmvc`. Usunięcie webmvc wymaga jawnego `spring-web`, inaczej
  `ClickupClient`/`ClickupWorkspaceClient` się nie skompilują.
- **`clients/desktop-java`** to goły scaffold JavaFX 21 (`App.java` „Hello",
  `module-info.java` JPMS, `javafx-maven-plugin`). `clients/flutter`, `clients/web`
  — scaffoldy bez kodu aplikacji.
- **lessons.md (priory):** Boot 4 = Jackson 3 (`tools.jackson.*`), test-slice'y w
  modularnych pakietach (`spring-boot-webmvc-test`), Testcontainers 2.x (prefiks
  `testcontainers-`), Flyway wymaga `spring-boot-flyway`. Restrukturyzacja musi je
  utrzymać, by ~40 istniejących testów zostało zielonych.
- **Toolchain:** `JAVA_HOME="$JAVA_HOME21" "$MAVEN_HOME9/bin/mvn"` (JDK 21 + Maven 3.9.x),
  dziś przez `server/build.sh`.

## Desired End State

Reaktor Maven w korzeniu repo z modułami `core` i `ui`. `core` to ten sam kod
domeny/persistencji/sync/integracji co dziś `server/`, ale bez warstwy REST i bez
Tomcata (kontekst rozwiązuje się jako **non-web**), z cienką warstwą aplikacyjną
zwracającą DTO widoku. `ui` to aplikacja JavaFX uruchamiana z classpath, która
bootuje kontekst Spring, woła fasadę łączności in-process i pokazuje wynik w oknie
ze spójnym, klawiaturowym szkieletem nawigacji. `clients/flutter` i `clients/web`
nie istnieją.

**Weryfikacja:** `mvn -B test` w korzeniu reaktora przechodzi (testy `core`
zielone, testy kontrolerów zastąpione testami fasad, test TestFX powłoki zielony
headless); `mvn -pl ui javafx:run` pokazuje okno ze stanem łączności; brak
nasłuchu na porcie HTTP przy starcie.

### Key Discoveries:

- Beany `core` są bez-HTTP-owe → UI woła je wprost; jedyne do przeniesienia to
  mapowanie z 3 kontrolerów (`SettingsController.java:36`, `SyncController.java:39`,
  `ListController.java:27`).
- `RestClientAutoConfiguration` aktywuje się na obecności klasy `RestClient`
  (`spring-web`), niezależnie od serwera web → RestClient przeżywa usunięcie webmvc,
  jeśli dodamy jawny `spring-web`.
- `module-info.java` w scaffoldzie JavaFX + Spring na module-path = ból
  (`opens`/automatic modules); decyzja: classpath + osobny `Launcher`.
- Decyzje użytkownika (2026-06-25, ta sesja): pełne usunięcie webmvc; powłoka =
  odczyt connectivity; classpath+Launcher, pakowanie odłożone; mały szkielet
  akceleratorów; **TestFX** (nie smoke); **DTO zachowane jako typy widoku warstwy
  aplikacji**.

## What We're NOT Doing

- **Nie** budujemy realnej nawigacji milestone→task ani widoku kontekstu (to S-03).
- **Nie** budujemy UI wpisywania tokenu ani wyzwalania syncu (to późniejsze slice'y /
  S-07); powłoka tylko *czyta* łączność.
- **Nie** pakujemy aplikacji (jlink/jpackage/fat-jar) — dev-run wystarcza w fundamencie.
- **Nie** zmieniamy schematu bazy, logiki sync, integracji ClickUp ani kontraktu
  DTO (poza miejscem ich produkcji: kontroler → fasada).
- **Nie** edytujemy `context/archive/` — kod F-01/F-02/S-01 refaktorujemy pod F-03.
- **Nie** wprowadzamy pełnego systemu keybindingów (warstwy kontekstu, konfiguracja).

## Implementation Approach

Trzy fazy o rosnącym ryzyku, każda z bramką „testy zielone":

1. **Czysta restrukturyzacja** (reactor + `server/`→`core`, kasacja flutter/web,
   pusty `ui`) **bez zmiany zachowania** — Tomcat i kontrolery jeszcze są, więc
   wszystkie istniejące testy muszą przejść 1:1. To izoluje ryzyko „przeniesienia"
   od ryzyka „zmiany architektury".
2. **Usunięcie warstwy web + fasady aplikacyjne** — wymiana zależności (webmvc →
   `spring-web` + `spring-boot-starter`), kasacja kontrolerów i ich `@WebMvcTest`,
   przeniesienie mapowania do fasad zwracających zachowane DTO, kontekst non-web.
3. **Powłoka JavaFX na Springu** — `ui` startuje kontekst, czyta łączność
   in-process, szkielet klawiatury, TestFX headless.

## Critical Implementation Details

- **RestClient bez startera web (Faza 2):** po usunięciu `spring-boot-starter-webmvc`
  dodaj jawnie `org.springframework:spring-web` (i `spring-boot-starter` jako
  rdzeń kontekstu). `RestClient.Builder` nadal jest autokonfigurowany. Sprawdź, że
  kontekst rozwiązuje się jako `WebApplicationType.NONE` (brak nasłuchu portu).
  Test klienta `ClickupWorkspaceClientTest` używa `MockRestServiceServer`
  (w `spring-test`) — przeżywa bez webmvc.
- **Bootstrap FX↔Spring (Faza 3):** uruchamiamy z classpath. Osobna klasa
  `Launcher` z `main()` (NIE rozszerza `Application`) woła
  `Application.launch(App.class, args)` — to obejście błędu „Missing JavaFX runtime
  components" przy starcie z classpath. `App extends Application`: w `init()`
  zbuduj kontekst przez `new SpringApplicationBuilder(ClickupSimplifierApplication.class, UiConfig.class).web(WebApplicationType.NONE).headless(false).run(args)`;
  w `start(Stage)` pobierz z kontekstu bean korzeniowego widoku i pokaż scenę; w
  `stop()` zamknij kontekst i `Platform.exit()`.
- **Connectivity poza wątkiem FX (Faza 3):** `ConnectivityService.checkConnectivity()`
  robi I/O sieciowe — wywołaj je w `javafx.concurrent.Task` na tle i zaktualizuj
  label w `Platform.runLater`/`succeeded`, żeby nie blokować wątku JavaFX. Stan
  początkowy labela: „sprawdzanie…".
- **Headless TestFX w CI (Faza 3):** ustaw właściwości systemowe Monocle
  (`testfx.robot=glass`, `testfx.headless=true`, `glass.platform=Monocle`,
  `monocle.platform=Headless`, `prism.order=sw`) przez konfigurację surefire dla
  modułu `ui`; zależności `org.testfx:testfx-core`, `org.testfx:testfx-junit5`,
  `org.testfx:openjfx-monocle`.

## Phase 1: Reaktor Maven + przeniesienie `core` (bez zmiany zachowania)

### Overview

Wprowadzamy parent pom agregujący `core` + `ui`, przenosimy `server/` → `core/`
nienaruszone (webmvc i kontrolery zostają), usuwamy scaffoldy flutter/web i
tworzymy pusty, budowalny moduł `ui`. Cel: wszystkie istniejące testy zielone pod
nowym układem.

### Changes Required:

#### 1. Parent pom (reaktor)

**File**: `pom.xml` (nowy, korzeń repo)

**Intent**: Spiąć moduły `core` i `ui` w jeden reaktor; być źródłem zarządzania
wersjami (przez `spring-boot-starter-parent`) dla obu modułów.

**Contract**: `<packaging>pom</packaging>`; `<parent>` = `spring-boot-starter-parent:4.1.0`;
`<modules>core, ui</modules>`; `<properties><java.version>21</java.version></properties>`;
`groupId=com.example`, `artifactId=clickup-simplifier-parent`, `version=0.0.1-SNAPSHOT`.

#### 2. Moduł `core` (z `server/`)

**File**: `core/` (przeniesione `server/src`, `server/src/main/resources`, migracje), `core/pom.xml`

**Intent**: `core` to dotychczasowy `server/` 1:1; jedyna zmiana w tej fazie to
`<parent>` wskazujący na lokalny parent zamiast bezpośrednio na
`spring-boot-starter-parent`, oraz `artifactId=clickup-simplifier-core`.

**Contract**: zachowane wszystkie dotychczasowe zależności (w tym `-webmvc`,
`-webmvc-test`, data-jdbc, Flyway `spring-boot-flyway` + `flyway-core` +
`flyway-database-postgresql`, Testcontainers `testcontainers-*`). Pakiet bazowy
`com.example.clickupsimplifier` bez zmian. Migracje pozostają w
`core/src/main/resources/db/migration/`.

#### 3. Pusty moduł `ui`

**File**: `ui/pom.xml`, `ui/src/main/java/com/example/clickupsimplifier/ui/.gitkeep` (placeholder)

**Intent**: Zarejestrować budowalny moduł `ui` zależny od `core`, żeby reaktor był
kompletny; realne klasy JavaFX dochodzą w Fazie 3.

**Contract**: `<parent>` = lokalny parent; `artifactId=clickup-simplifier-ui`;
`<dependency>` na `clickup-simplifier-core`; zależności JavaFX (`javafx-controls:21`)
i `javafx-maven-plugin` przeniesione z `clients/desktop-java/pom.xml`. Bez
`module-info` (classpath).

#### 4. Kasacja porzuconych klientów i relokacja skryptów

**File**: usuń `clients/flutter/`, `clients/web/`, `clients/desktop-java/` (po przeniesieniu jego pom/scriptów do `ui`); zaktualizuj `server/build.sh`/`run.sh` → korzeń/`core`

**Intent**: Usunąć multi-client; przenieść build/run pod reaktor.

**Contract**: build script woła `mvn -f pom.xml` z korzenia (reaktor). Żaden plik
w repo (skrypty w `scripts/`, workflowy `.github/`, jeśli istnieją) nie wskazuje na
nieistniejący `server/` ani `clients/`.

### Success Criteria:

#### Automated Verification:

- Reaktor się skanuje: `JAVA_HOME="$JAVA_HOME21" "$MAVEN_HOME9/bin/mvn" -B -f pom.xml validate`
- Pełna kompilacja + testy zielone: `JAVA_HOME="$JAVA_HOME21" "$MAVEN_HOME9/bin/mvn" -B -f pom.xml test` (wszystkie istniejące testy `core` przechodzą)
- `ui` się buduje (pusty): moduł `clickup-simplifier-ui` w reaktorze kompiluje się bez błędu
- Brak martwych ścieżek: `grep -rn "server/" scripts/ .github/ 2>/dev/null` nie zwraca odwołań do starej lokalizacji (poza historią/archiwum)

#### Manual Verification:

- Drzewo repo pokazuje `pom.xml` + `core/` + `ui/`, brak `clients/`
- `core/` zawiera komplet kodu i migracji co dawne `server/`
- Wymaga Dockera dla testów Testcontainers (jak dotąd)

**Implementation Note**: Po przejściu automatycznej weryfikacji zatrzymaj się na
ręczne potwierdzenie przed Fazą 2. Checkboxy w `## Progress`.

---

## Phase 2: Usunięcie warstwy web + fasady aplikacyjne

### Overview

Usuwamy `-webmvc`/Tomcat, dodajemy jawny `spring-web` (RestClient) + rdzeniowy
`spring-boot-starter`. Kasujemy 3 kontrolery i ich `@WebMvcTest`, a ich mapowanie
domena→DTO przenosimy do fasad warstwy aplikacyjnej zwracających zachowane DTO
widoku. Kontekst startuje jako non-web.

### Changes Required:

#### 1. Wymiana zależności web w `core/pom.xml`

**File**: `core/pom.xml`

**Intent**: Zdjąć serwer HTTP, zachować klienta HTTP (RestClient) i rdzeń kontekstu.

**Contract**: usuń `spring-boot-starter-webmvc` i `spring-boot-starter-webmvc-test`;
dodaj `org.springframework.boot:spring-boot-starter` (rdzeń) oraz
`org.springframework:spring-web` (RestClient). Jackson 3 (`tools.jackson.*`)
pozostaje dostarczany tranzytywnie/jawnie — utrzymać zgodnie z lessons.md (NIE
dodawać `spring-boot-starter-json` na siłę; zweryfikować obecność Jacksona po
zmianie). Test RestClienta opiera się na `MockRestServiceServer` ze `spring-test`
(scope test).

#### 2. Kasacja kontrolerów i ich testów

**File**: usuń `clickup/SettingsController.java`, `sync/SyncController.java`, `sync/ListController.java` oraz `clickup/SettingsControllerTest.java`, `sync/SyncControllerTest.java`, `sync/ListControllerTest.java`

**Intent**: Usunąć powierzchnię REST; testy slice'owe `@WebMvcTest` przestają mieć
rację bytu (i zależność zniknęła).

**Contract**: po kasacji żaden plik nie importuje `org.springframework.web.bind.annotation.*`
ani `org.springframework.boot.webmvc.test.autoconfigure.*`.

#### 3. Warstwa aplikacyjna (fasady) — `core`

**File**: `clickup/ConnectivityViewService.java`, `sync/SyncViewService.java`, `sync/ListViewService.java` (nowe, w istniejących pakietach)

**Intent**: Przenieść mapowanie domena→DTO z kontrolerów do beanów warstwy
aplikacyjnej wołanych in-process; zachować DTO (`ConnectivityResponse`,
`SyncStatusResponse`, `ListResponse`) jako typy widoku oraz typy komend
(`SetTokenRequest`, `SyncEnabledRequest`) jako wejścia. Semantyka HTTP (409 przy
trwającym sync, 404 przy braku listy, walidacja tokenu) wraca jako wartości
zwracane, nie kody HTTP.

**Contract**:
- `ConnectivityViewService.current() : ConnectivityResponse` — logika z `SettingsController.checkConnectivity()` (`SettingsController.java:36`).
- `ConnectivityViewService.saveToken(SetTokenRequest) : void` (rzuca/zwraca błąd walidacji jak dziś `IllegalArgumentException`).
- `SyncViewService.triggerFullPull() : TriggerResult` (enum `ACCEPTED`/`ALREADY_RUNNING` zamiast 202/409) — logika z `SyncController.java:30`.
- `SyncViewService.status() : SyncStatusResponse` — logika z `SyncController.java:39`.
- `ListViewService.list() : List<ListResponse>` oraz `ListViewService.setSyncEnabled(String id, boolean) : boolean` (false = nie znaleziono) — logika z `ListController.java`.
- DTO pozostają w `clickup/dto/` i `sync/dto/` (typy warstwy aplikacji, bez adnotacji HTTP).

#### 4. Testy fasad (zastępują testy kontrolerów)

**File**: `clickup/ConnectivityViewServiceTest.java`, `sync/SyncViewServiceTest.java`, `sync/ListViewServiceTest.java`

**Intent**: Utrzymać pokrycie mapowania i przypadków brzegowych, które wcześniej
sprawdzały testy kontrolerów (walidacja tokenu, 409→ALREADY_RUNNING, 404→false,
kształt statusu), jako zwykłe testy jednostkowe (bez MockMvc).

**Contract**: testy oparte na Mockito/zwykłych asercjach; brak `@WebMvcTest`,
brak `MockMvc`.

### Success Criteria:

#### Automated Verification:

- Kompilacja + testy zielone: `JAVA_HOME="$JAVA_HOME21" "$MAVEN_HOME9/bin/mvn" -B -f pom.xml test`
- Brak zależności web: `grep -rn "spring-boot-starter-webmvc" core/pom.xml` nie zwraca nic
- Kontekst non-web: test ładowania kontekstu (np. `ClickupSimplifierApplicationTests`) przechodzi i nie podnosi serwera (brak nasłuchu portu / brak `ServletWebServerApplicationContext`)
- Brak importów web/`@WebMvcTest` w kodzie `core`

#### Manual Verification:

- Uruchomienie `core` jako aplikacji nie zajmuje portu HTTP (log bez „Tomcat started")
- Fasady zwracają te same kształty DTO co dawne endpointy (porównanie z F-01/S-01)

**Implementation Note**: Zatrzymaj się na ręczne potwierdzenie przed Fazą 3.

---

## Phase 3: Powłoka JavaFX na Springu + odczyt łączności + szkielet klawiatury + TestFX

### Overview

Moduł `ui` startuje kontekst Spring z classpath (Launcher + App), pobiera bean
korzeniowego widoku, woła `ConnectivityViewService` in-process na wątku tła i
pokazuje stan łączności. Dodajemy mały szkielet akceleratorów/fokusu i test TestFX
działający headless.

### Changes Required:

#### 1. Bootstrap classpath: Launcher + App

**File**: `ui/src/main/java/com/example/clickupsimplifier/ui/Launcher.java`, `.../ui/App.java`

**Intent**: Uruchomić JavaFX z classpath bez JPMS i spiąć cykl życia z kontekstem
Spring.

**Contract**: `Launcher.main` woła `Application.launch(App.class, args)`. `App extends Application`
buduje kontekst w `init()` (`SpringApplicationBuilder(ClickupSimplifierApplication.class, UiConfig.class).web(WebApplicationType.NONE).headless(false).run(args)`),
w `start(Stage)` pobiera `RootViewController`/root `Parent` z kontekstu i pokazuje
scenę, w `stop()` zamyka kontekst i woła `Platform.exit()`. Brak `module-info`.

#### 2. Konfiguracja kontekstu UI

**File**: `ui/src/main/java/com/example/clickupsimplifier/ui/UiConfig.java`

**Intent**: Włączyć skanowanie beanów `ui` (kontrolery widoków) obok beanów `core`.

**Contract**: `@Configuration @ComponentScan("com.example.clickupsimplifier.ui")`
(beany `core` wchodzą przez `ClickupSimplifierApplication`).

#### 3. Korzeniowy widok + odczyt łączności

**File**: `ui/src/main/java/com/example/clickupsimplifier/ui/RootView.java` (bean)

**Intent**: Pokazać stan łączności pobrany z `core` in-process, dowodząc kontraktu
ui→core; ustanowić nawigowalny klawiaturą layout.

**Contract**: bean `@Component` zależny od `ConnectivityViewService`; buduje
`Parent`/`Scene` z labelem stanu („sprawdzanie…" → wynik). Odczyt łączności w
`javafx.concurrent.Task` na tle; aktualizacja labela przez `setOnSucceeded`. Stan
mapowany z `ConnectivityResponse.status()` na czytelny komunikat (OK + nazwa
użytkownika / NOT_CONFIGURED / TOKEN_REJECTED / UNREACHABLE).

#### 4. Szkielet klawiatury

**File**: `ui/src/main/java/com/example/clickupsimplifier/ui/KeyboardScaffold.java`

**Intent**: Ustanowić jedno miejsce rejestrujące globalne akceleratory i
przewidywalny model fokusu — wzorzec, który rozwinie S-03.

**Contract**: rejestracja `scene.getAccelerators()` z 1–2 przykładami (np.
`Ctrl+R` = ponów sprawdzenie łączności, `F1`/`Esc` = no-op/placeholder); ustawienie
fokusu początkowego na deterministyczny węzeł. Bez konfigurowalnych map (poza zakresem).

#### 5. Dev-run + TestFX (pom `ui`)

**File**: `ui/pom.xml`

**Intent**: Umożliwić `mvn -pl ui javafx:run` i headless testy GUI.

**Contract**: `javafx-maven-plugin` z `mainClass=com.example.clickupsimplifier.ui.Launcher`;
zależności testowe `org.testfx:testfx-core`, `org.testfx:testfx-junit5`,
`org.testfx:openjfx-monocle`; surefire dla `ui` ustawia właściwości Monocle
(`testfx.headless=true`, `glass.platform=Monocle`, `monocle.platform=Headless`,
`prism.order=sw`, `testfx.robot=glass`).

#### 6. Test TestFX powłoki

**File**: `ui/src/test/java/com/example/clickupsimplifier/ui/RootViewTest.java`

**Intent**: Zweryfikować, że powłoka startuje, renderuje stan łączności i reaguje
na klawiaturę — headless.

**Contract**: test typu `ApplicationTest`; podstawia/atrapuje `ConnectivityViewService`
(np. zwraca NOT_CONFIGURED), startuje root, asercja że label pokazuje oczekiwany
komunikat; symulacja `Ctrl+R` wywołuje ponowny odczyt; asercja fokusu początkowego.

### Success Criteria:

#### Automated Verification:

- Pełny reaktor zielony headless: `JAVA_HOME="$JAVA_HOME21" "$MAVEN_HOME9/bin/mvn" -B -f pom.xml test` (w tym test TestFX modułu `ui`)
- Test TestFX nie wymaga wyświetlacza (przechodzi w trybie headless/Monocle)
- Kontekst `ui` ładuje się z `WebApplicationType.NONE` (brak nasłuchu portu w teście startu)

#### Manual Verification:

- `mvn -pl ui javafx:run` pokazuje okno z poprawnym stanem łączności (zależnie od skonfigurowanego tokenu: OK + użytkownik / NOT_CONFIGURED / TOKEN_REJECTED / UNREACHABLE)
- `Ctrl+R` ponawia sprawdzenie; `Tab`/`Shift+Tab` przewidywalnie przesuwa fokus; brak skoków fokusu
- Zamknięcie okna kończy proces (kontekst zamknięty, brak wiszących wątków)

**Implementation Note**: Po Fazie 3 i zielonej weryfikacji automatycznej zatrzymaj
się na ręczne potwierdzenie zakończenia zmiany.

---

## Testing Strategy

### Unit Tests:

- Fasady aplikacyjne (`*ViewServiceTest`): mapowanie domena→DTO, walidacja tokenu,
  `ALREADY_RUNNING` przy trwającym sync, „nie znaleziono" przy toggle listy, kształt statusu.
- Zachowane testy `core` (F-01/F-02/S-01) muszą pozostać zielone po restrukturyzacji
  i po usunięciu webmvc (poza skasowanymi testami kontrolerów).

### Integration Tests:

- Ładowanie kontekstu `core` jako non-web (brak serwletowego kontekstu/portu).
- TestFX (headless) powłoki `ui`: render stanu łączności + reakcja na klawiaturę.

### Manual Testing Steps:

1. `mvn -pl ui javafx:run` z nieskonfigurowanym tokenem → label „NOT_CONFIGURED".
2. Skonfiguruj poprawny token (plik ustawień F-01) → restart → label „OK" + nazwa użytkownika.
3. Token błędny → „TOKEN_REJECTED"; brak sieci → „UNREACHABLE".
4. `Ctrl+R` ponawia; `Tab`/`Shift+Tab` przesuwa fokus przewidywalnie.
5. Zamknij okno → proces kończy się czysto.

## Performance Considerations

Odczyt łączności robi I/O sieciowe — musi iść w `Task` na tle, nigdy na wątku FX
(inaczej zawieszenie UI przy starcie / przy `UNREACHABLE`). Poza tym powłoka jest
trywialna; brak budżetów wydajności w tej fazie (NFR ~100 ms dotyczy nawigacji po
lokalnej kopii — S-03).

## Migration Notes

- `server/` → `core/`: przeniesienie katalogu + zmiana `<parent>` w pom; pakiet
  bazowy bez zmian, więc importy w kodzie `core` się nie zmieniają.
- Refaktor dotyka kodu z zarchiwizowanych F-01/F-02/S-01, ale wszystkie zmiany
  jadą pod change-id `first-client-shell`; `context/archive/` nietknięte.
- DTO przechodzą z roli „ciało HTTP" do „typ widoku warstwy aplikacji" bez zmiany kształtu.

## References

- Roadmap: `context/foundation/roadmap.md` (F-03, rewizja 2026-06-25)
- Tech-stack: `context/foundation/tech-stack.md` (sekcja „Architecture revision")
- Lessons: `context/foundation/lessons.md` (Boot 4: Jackson 3, test-slice'y, Testcontainers 2.x, Flyway)
- Beany rdzenia: `server/.../clickup/ConnectivityService.java:22`, `.../sync/WorkspaceSyncService.java:78`, `.../settings/SettingsStore.java:40`
- Mapowanie do przeniesienia: `.../clickup/SettingsController.java:36`, `.../sync/SyncController.java:39`, `.../sync/ListController.java:27`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Reaktor Maven + przeniesienie core

#### Automated

- [x] 1.1 Reaktor się skanuje (`mvn -f pom.xml validate`)
- [x] 1.2 Pełna kompilacja + testy zielone (`mvn -f pom.xml test`)
- [x] 1.3 Moduł `ui` (pusty) kompiluje się w reaktorze
- [x] 1.4 Brak martwych ścieżek `server/` w `scripts/` i `.github/`

#### Manual

- [ ] 1.5 Drzewo repo: `pom.xml` + `core/` + `ui/`, brak `clients/`
- [ ] 1.6 `core/` zawiera komplet kodu i migracji co dawne `server/`

### Phase 2: Usunięcie warstwy web + fasady aplikacyjne

#### Automated

- [ ] 2.1 Kompilacja + testy zielone (`mvn -f pom.xml test`)
- [ ] 2.2 Brak `spring-boot-starter-webmvc` w `core/pom.xml`
- [ ] 2.3 Kontekst ładuje się jako non-web (brak nasłuchu portu)
- [ ] 2.4 Brak importów web / `@WebMvcTest` w kodzie `core`

#### Manual

- [ ] 2.5 Uruchomienie `core` nie zajmuje portu HTTP (log bez „Tomcat started")
- [ ] 2.6 Fasady zwracają te same kształty DTO co dawne endpointy

### Phase 3: Powłoka JavaFX + connectivity + szkielet klawiatury + TestFX

#### Automated

- [ ] 3.1 Pełny reaktor zielony headless, w tym TestFX `ui` (`mvn -f pom.xml test`)
- [ ] 3.2 Test TestFX przechodzi headless (Monocle, bez wyświetlacza)
- [ ] 3.3 Kontekst `ui` ładuje się z `WebApplicationType.NONE`

#### Manual

- [ ] 3.4 `mvn -pl ui javafx:run` pokazuje okno z poprawnym stanem łączności
- [ ] 3.5 `Ctrl+R` ponawia; `Tab`/`Shift+Tab` przewidywalny fokus, brak skoków
- [ ] 3.6 Zamknięcie okna kończy proces czysto
