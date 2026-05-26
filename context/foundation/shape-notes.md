---
project: "LicenceQuizz"
context_type: greenfield
created: 2026-05-25
updated: 2026-05-26
version: 1
status: draft
product_type: other
target_scale:
  users: small
  qps: low
  data_volume: small
timeline_budget:
  mvp_weeks: 7
  hard_deadline: null
  after_hours_only: true
checkpoint:
  current_phase: 8
  phases_completed: [1, 2, 3, 4, 5, 6, 7]
  gray_areas_resolved:
    - topic: "pain category"
      decision: "workflow friction (forced video playback) + missing capability (difficulty tagging) + learning quality (no error-based repetition)"
    - topic: "insight"
      decision: "no single app combines: interrupt video playback + difficulty tagging + smart repetition based on difficulty/errors"
    - topic: "primary persona"
      decision: "user + friends/family — small known group, not a public app"
    - topic: "access control"
      decision: "email + password login; each user has separate tags and progress; data in cloud (multi-device access)"
    - topic: "mvp timeline"
      decision: "3 weeks after-hours; user confirmed confidence"
    - topic: "client platform"
      decision: "multi-platform comparative project; 5 independent clients: Flutter (web+desktop+mobile), Android+JetPack Compose, Android without JC, JavaFX, Java Swing; developed in parallel from scratch; any working variant = MVP"
    - topic: "timeline revision"
      decision: "7+ weeks; parallel development of all 5 clients; user consciously accepted sustained effort cost on 2026-05-26"
    - topic: "keyboard support scope"
      decision: "Flutter (web + desktop builds), JavaFX, Java Swing; touch-first (one thumb) for Android variants"
    - topic: "offline mode"
      decision: "deferred to Open Questions — no decision yet"
  frs_drafted: 10
  quality_check_status: accepted
---

## Vision & Problem Statement

Kandydaci na egzamin na prawo jazdy nie mają jednego narzędzia, które łączy trzy kluczowe funkcje efektywnej nauki: możliwość przerwania odtwarzania pytania filmowego, tagowanie pytań własną skalą trudności oraz powtarzanie pytań na podstawie trudności lub liczby błędów. Oficjalne aplikacje wymuszają pełne odtworzenie wideo przed odpowiedzią; dostępne alternatywy nie oferują wszystkich tych funkcji w jednym miejscu.

Insight: oczekiwane funkcjonalności nie są wyszukane technicznie — żadna znana aplikacja nie zebrała ich razem. Szybki research nie pozwolił znaleźć jednego rozwiązania zawierającego: przerwanie odtwarzania + tagowanie stopniem trudności + powtarzanie wg trudności/błędów.

## User & Persona

**Podstawowa persona:** Osoba przygotowująca się do egzaminu na prawo jazdy w Polsce. Zna już format egzaminu, chce efektywnie powtarzać materiał bez marnowania czasu na obowiązkowe odtwarzanie wideo.

**Zakres:** Użytkownik oraz znajomi/rodzina — mała, znana grupa. Nie jest to publiczna aplikacja.

## Success Criteria

### Primary
- Użytkownik może zalogować się, skonfigurować sesję (liczba pytań + kategoria + sposób doboru), przejść przez quiz (przerywając wideo, tagując trudność, odpowiadając) i zobaczyć podsumowanie z opcją uruchomienia kolejnej sesji.

### Secondary
- Widok statystyk między sesjami pokazujący trend błędów w czasie.

### Guardrails
- Tagi i historia błędów jednego użytkownika są zawsze odizolowane od danych innych kont.
- Każda odpowiedź jest zapisywana natychmiast — awaria lub odświeżenie nie kasuje wyniku trwającej sesji.
- Klawiatura: tagowanie trudności i udzielanie odpowiedzi dostępne z klawiatury w wariantach Flutter (web i desktop), JavaFX, Java Swing. Pełna nawigacja po aplikacji nie jest wymagana.
- Dotyk: wszystkie interakcje w wariantach Android dostępne jednym kciukiem — brak precyzyjnych tapnięć małych obszarów.
- Tryb offline: decyzja odroczona — patrz Open Questions.

## Access Control

Login e-mail + hasło — jeden system auth dla obu aplikacji. Dwie role:
- **User** — dostęp do aplikacji quizowej; widzi wyłącznie własne tagi i historię błędów.
- **Admin** — ta sama metoda logowania co user, ale z flagą admin w bazie; dodatkowo dostęp do oddzielnego panelu CMS (zarządzanie pytaniami). Rola nadawana ręcznie przez dewelopera.

Dane użytkownika (tagi, historia błędów) są zawsze odizolowane między kontami.

## Functional Requirements

### Authentication

- FR-001: Użytkownik może założyć konto podając e-mail i hasło. Priority: must-have
  > Socrates: Kontrargument: backend auth opóźnia dostarczenie wartości o ~tydzień pracy. Rozstrzygnięcie: zachowano — synchronizacja danych między urządzeniami wymaga kont; koszt jest akceptowany świadomie.

- FR-002: Użytkownik może zalogować się do swojego konta. Priority: must-have

### Quiz session setup

- FR-003: Użytkownik może skonfigurować sesję quizową wybierając liczbę pytań, kategorię oraz sposób doboru pytań (wszystkie / wg tagów trudności nadanych w poprzednich sesjach / najbardziej kłopotliwe — największa historyczna liczba błędów). Priority: must-have
  > Socrates: Kontrargument: ekran konfiguracji przed każdą sesją to tarcie — większość użytkowników zawsze wybierze to samo. Rozstrzygnięcie: zachowano — różne tryby doboru to główna wartość produktu; warto rozważyć zapamiętywanie ostatniej konfiguracji.

### Quiz playback

- FR-004: Użytkownik może przerwać odtwarzanie wideo pytania bez oczekiwania na jego koniec. Priority: must-have
  > Socrates: Kontrargument: hosting wideo (zewnętrzny lub w DB) to nietrywialny koszt. Rozstrzygnięcie: zachowano — to pierwotna ból-kość opisana w seed idea; wideo będzie hostowane zewnętrznie lub w bazie.

- FR-005: Użytkownik może otagować pytanie własnym stopniem trudności podczas quizu — tag klasyfikuje pytanie na potrzeby przyszłych sesji i nie wpływa na dobór pytań w bieżącej sesji. Priority: must-have
  > Socrates: Brak kontrargumentu — tagowanie to kluczowa wartość różniąca produkt.

- FR-006: Użytkownik może udzielić odpowiedzi na pytanie wybierając jedną z dostępnych opcji. Priority: must-have
  > Socrates: Brak kontrargumentu — rdzeń quizu.

### Results & statistics

- FR-007: Użytkownik widzi krótkie podsumowanie sesji po jej zakończeniu (liczba błędnych odpowiedzi). Priority: must-have
  > Socrates: Kontrargument: sam wynik bez kontekstu historycznego ma ograniczoną wartość. Rozstrzygnięcie: uproszczono zakres — podsumowanie pokazuje tylko liczbę błędnych odpowiedzi; porównanie historyczne trafia do FR-009 (nice-to-have).

- FR-008: Użytkownik może uruchomić nową sesję bezpośrednio z ekranu podsumowania. Priority: must-have

- FR-009: Użytkownik może przeglądać statystyki między sesjami pokazujące trend błędów w czasie. Priority: nice-to-have
  > Socrates: Kontrargument: statystyki mają wartość dopiero po kilku sesjach — w MVP danych jeszcze nie będzie. Rozstrzygnięcie: potwierdzone jako nice-to-have; odkładamy na post-MVP.

### Question management (admin)

- FR-010: Administrator może zarządzać bazą pytań przez oddzielny panel CMS (dodawanie, edycja, usuwanie pytań wraz z wideo). Priority: must-have
  > Socrates: Brak kontrargumentu — bez możliwości edycji pytań aplikacja jest nieużyteczna po fazie MVP z ręcznie wpisanymi danymi.

## Business Logic

Aplikacja dobiera pytania na podstawie historii trudności i błędów użytkownika.

Wejścia reguły (jako dane użytkownika, nie komponenty systemu): tagi trudności nadane przez użytkownika per pytanie oraz liczba błędnych odpowiedzi na to pytanie w poprzednich sesjach. Wyjście: zestaw pytań do sesji zgodny z wybranym trybem doboru (wszystkie / wg tagów / najbardziej kłopotliwe). Użytkownik napotyka regułę w momencie konfiguracji sesji — wybór trybu decyduje o tym, które pytania trafią do kolejki.

## Non-Functional Requirements

- Dane użytkownika (historia błędów, tagi trudności) nie są udostępniane osobom trzecim.
- Klawiatura: tagowanie trudności i udzielanie odpowiedzi dostępne z klawiatury w wariantach Flutter (web i desktop), JavaFX, Java Swing.
- Dotyk: interakcje w wariantach Android dostępne jednym kciukiem bez precyzyjnych tapnięć.

## Non-Goals

- Brak jednolitej bazy kodu między wariantami klientów — każda implementacja jest celowo oddzielna (cel porównawczy).
- Brak dedykowanego klienta webowego (np. React/Vue/Angular) — Flutter web jako build target Fluttera nie jest tą samą kategorią i nie wyklucza tego non-goal.
- Brak publicznej rejestracji — konta tworzone tylko dla znanych użytkowników; aplikacja nie jest otwartą platformą dla wszystkich kandydatów.
- Brak pełnej bazy pytań egzaminacyjnych — MVP zawiera wąski, ręcznie wprowadzony zestaw pytań; pełna baza to rozbudowa post-MVP.
- Brak statystyk między sesjami (FR-009) — potwierdzone jako nice-to-have; odkładamy do momentu, gdy będą realne dane.

## User Stories

### US-01: Konfiguracja i przejście przez sesję quizową

- **Given** zalogowany użytkownik na ekranie startu
- **When** wybiera liczbę pytań, kategorię i sposób doboru, a następnie uruchamia quiz
- **Then** kolejno widzi pytania (z możliwością przerwania wideo), taguje trudność, odpowiada, a po ostatnim pytaniu trafia na ekran podsumowania z opcją nowej sesji

#### Acceptance Criteria
- Przerwanie wideo jest możliwe w dowolnym momencie odtwarzania
- Tag trudności można zmienić przed przejściem do następnego pytania; nie zmienia kolejki pytań w bieżącej sesji
- Podsumowanie pokazuje liczbę poprawnych/błędnych odpowiedzi z tej sesji
- Klawiatura: tagowanie trudności i udzielanie odpowiedzi dostępne z klawiatury w wariantach Flutter (web i desktop), JavaFX, Java Swing
- Dotyk: wszystkie interakcje w wariantach Android dostępne jednym kciukiem

## Timeline acknowledgment

Acknowledged on 2026-05-26: 7+ tygodni MVP (równoległy start wszystkich 5 wariantów klientów) wymaga długotrwałego zaangażowania; użytkownik świadomie akceptuje ten koszt. MVP = dowolny działający wariant, który dotrze pierwszy. Na późniejszym etapie możliwa rezygnacja z niektórych wariantów.

## Open Questions

- **Tryb offline**: czy warianty Android powinny działać bez połączenia sieciowego? Decyzja odroczona. Implikacja: wymaga cache'owania pytań i wideo na urządzeniu, co podnosi koszt MVP.

## Forward: tech-stack

Planowane warianty klientów (porównanie implementacji — nie należy do PRD):

| # | Klient | Platforma | Keyboard support |
|---|--------|-----------|-----------------|
| 1 | Flutter | Web + Desktop + Mobile | tak (web i desktop) |
| 2 | Android + JetPack Compose | Mobile | dotyk |
| 3 | Android bez JetPack Compose | Mobile | dotyk |
| 4 | JavaFX | Desktop | tak |
| 5 | Java Swing | Desktop | tak |

Warianty rozwijane niezależnie od zera; możliwa rezygnacja z niektórych na późniejszym etapie.

### Decyzje stackowe (ustalone 2026-05-26)

**Backend (wspólny):** Spring Boot (Java, Maven), deploy: Render, CI: GitHub Actions (auto-deploy on merge), katalog `licence-quizz-api`. Hand-off dla bootstrappera: `context/foundation/tech-stack.md`.

**Wspólne dla wszystkich 5 klientów:**
- Transport: REST + JSON po HTTPS do backendu.
- Auth: backend wydaje token przy logowaniu; klient przechowuje go (bezpieczny storage zależny od platformy) i dołącza w nagłówku `Authorization`.
- Izolacja danych egzekwowana po stronie backendu (user widzi tylko swoje tagi/historię).

Zasada porównawcza: warstwa rozmowy z backendem ma być jak najbardziej identyczna w obrębie pary, żeby różnica izolowała toolkit UI, a nie sieć/JSON.

| # | Klient | Język / build | UI | State / architektura | Sieć + JSON | Inne | Rejestr / scaffold |
|---|--------|---------------|----|----------------------|-------------|------|--------------------|
| 1 | Flutter | Dart / Flutter CLI (pub) | Flutter widgets | Riverpod | dio + json_serializable/freezed | go_router; flutter_secure_storage; klawiatura via Focus/Shortcuts/Actions (web+desktop) | karta `flutter`, `verified` → możliwy hand-off |
| 2 | Android + Jetpack Compose | Kotlin / Gradle | Jetpack Compose | MVVM: ViewModel + StateFlow | Retrofit + OkHttp + Moshi (KSP), Coroutines/Flow | DI: Hilt; token: DataStore/EncryptedSharedPreferences; touch-first | brak karty → ręcznie |
| 3 | Android bez Jetpack Compose | Kotlin / Gradle | XML layouts + View Binding | MVVM: ViewModel + LiveData | **identyczne jak #2** (Retrofit + OkHttp + Moshi + Hilt + Coroutines) | touch-first; różnica vs #2 = tylko warstwa UI | brak karty → ręcznie |
| 4 | JavaFX | Java / Maven | FXML + kontrolery | MVVM na Property/bindingach | java.net.http.HttpClient + Jackson | javafx-maven-plugin, pakowanie jpackage; klawiatura: akceleratory / setOnKeyPressed | brak karty → ręcznie |
| 5 | Java Swing | Java / Maven | programatyczny Swing (np. MigLayout) | MVC + SwingWorker (sieć poza EDT) | **identyczne jak #4** (HttpClient + Jackson) | pakowanie jpackage; klawiatura: InputMap/ActionMap, mnemoniki; różnica vs #4 = tylko toolkit UI | brak karty → ręcznie |

**Pary porównawcze (wspólna warstwa danych, różnica w UI):**
- Android #2 vs #3 → wspólny stack danych (Retrofit/Moshi/Hilt/Coroutines), różnica = Compose vs klasyczne Views.
- Desktop JVM #4 vs #5 → wspólny stack danych (HttpClient/Jackson), różnica = JavaFX vs Swing.

**Otwarte / do rewizji później:** Moshi vs kotlinx.serialization na Androidzie (wstępnie Moshi); tryb offline (patrz Open Questions w PRD); hosting wideo (zewnętrznie vs w bazie).
