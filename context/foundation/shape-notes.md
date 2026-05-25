---
project: "LicenceQuizz"
context_type: greenfield
created: 2026-05-25
updated: 2026-05-25
version: 1
status: draft
product_type: web-app
target_scale:
  users: small
  qps: low
  data_volume: small
timeline_budget:
  mvp_weeks: 3
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
      decision: "web app for MVP; mobile/desktop considered post-MVP"
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
- Tagowanie trudności i udzielanie odpowiedzi dostępne z klawiatury (pełna nawigacja po aplikacji nie jest wymagana).

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
- Aplikacja działa na współczesnych przeglądarkach desktop: Chrome, Firefox, Edge (dwie ostatnie wersje główne).
- Tagowanie trudności i udzielanie odpowiedzi dostępne z klawiatury.

## Non-Goals

- Brak natywnej aplikacji mobilnej (iOS/Android) — MVP to wyłącznie web; platforma mobilna rozważana po MVP.
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
- Tagowanie trudności i udzielanie odpowiedzi dostępne z klawiatury
