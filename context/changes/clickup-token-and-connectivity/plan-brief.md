# ClickUp Token + Uwierzytelniona Łączność (F-01) — Plan Brief

> Full plan: `context/changes/clickup-token-and-connectivity/plan.md`

## What & Why

Rdzeń Spring potrzebuje sposobu na zapisanie osobistego tokenu API ClickUp i
potwierdzenie, że token działa. To fundament F-01 — najmniejszy enabler, bez którego
silnik synchronizacji (S-01, pełny pull) nie ma jak się uwierzytelnić.

## Starting Point

`server/` to goły szkielet Spring Boot 4.1.0 (Java 21, starter `web-mvc`): tylko klasa
`Application` i domyślny test. Brak klienta HTTP, konfiguracji, kontrolerów i — co istotne —
brak warstwy danych (F-02 jeszcze nie istnieje), więc token nie może lądować w DB.

## Desired End State

Można zapisać token (przeżywa restart, plik ustawień na dysku) i wywołać sprawdzenie
łączności, które zwraca albo tożsamość użytkownika ClickUp (id + username), albo strukturalny
błąd rozróżniający: brak tokenu / token odrzucony (401) / ClickUp nieosiągalny. Dostępne jako
serwis (in-process dla JavaFX) i przez REST (dla web/flutter).

## Key Decisions Made

| Decision                    | Choice                                    | Why (1 sentence)                                                        | Source |
| --------------------------- | ----------------------------------------- | ---------------------------------------------------------------------- | ------ |
| Przechowywanie tokenu       | Plik ustawień JSON na dysku               | Przeżywa restart, zero zależności od DB; szyfrowanie odłożone (PRD)     | Plan   |
| Powierzchnia                | Serwis + cienki kontroler REST            | Web-mvc już jest; używalne przez każdego klienta, JavaFX woła in-process | Plan   |
| Test łączności              | `GET /api/v2/user` → tożsamość            | Kanoniczny „kim jestem", potwierdza token i daje czytelny feedback      | Plan   |
| Model błędów                | Strukturalny wynik z rozróżnieniem klas   | Panel sync (S-07) i tak będzie pokazywał opisy — ustalamy kształt raz    | Plan   |
| Walidacja tokenu            | Lekka (niepusty); 401 z ClickUp = prawda  | Proste i odporne na zmiany formatu tokenu po stronie ClickUp            | Plan   |
| Testy                       | `MockRestServiceServer` (200/401/timeout) | Deterministyczne, bez realnych wywołań i sekretów                       | Plan   |
| Konfiguracja                | URL + ścieżka pliku jako properties        | Testy wskazują mock-URL i temp-dir; łatwy override per środowisko        | Plan   |

## Scope

**In scope:** zapis/odczyt tokenu (plik JSON), klient `RestClient` do ClickUp, `GET /user`,
strukturalny wynik łączności, cienki kontroler REST, testy z mockiem HTTP.

**Out of scope:** szyfrowane przechowywanie tokenu, pobieranie workspace'ów/danych (S-01),
OAuth, UI/ekran ustawień (F-03), warstwa danych/DB (F-02), ponawianie/limity API.

## Architecture / Approach

`SettingsStore` (plik JSON) trzyma token. `ClickupClient` (na `RestClient` z bazowym URL)
woła `GET /user` z nagłówkiem `Authorization: <token>` (bez `Bearer`). `ConnectivityService`
spina jedno z drugim i zwraca `ConnectivityResult` (status OK/NOT_CONFIGURED/TOKEN_REJECTED/
UNREACHABLE + opcjonalna tożsamość + opis). Cienki `SettingsController` wystawia to po HTTP.
URL i ścieżka pliku to konfigurowalne properties.

## Phases at a Glance

| Phase                              | What it delivers                                   | Key risk                                        |
| ---------------------------------- | -------------------------------------------------- | ----------------------------------------------- |
| 1. Magazyn ustawień (token)        | Trwały zapis/odczyt tokenu w pliku JSON            | Ścieżka/uprawnienia pliku w katalogu domowym    |
| 2. Klient ClickUp + łączność       | `GET /user` + strukturalny wynik z klasami błędów  | Poprawny nagłówek auth ClickUp (bez `Bearer`)   |
| 3. Powierzchnia REST + spięcie     | Endpointy `PUT` token / `GET` connectivity         | Mapowanie statusów na odpowiedź HTTP            |

**Prerequisites:** F-02 i F-03 NIE są wymagane (slice celowo client- i DB-agnostyczny). Toolchain JDK 21 + Maven 3.9 (z roadmapy).
**Estimated effort:** ~1 sesja, 3 niewielkie fazy.

## Open Risks & Assumptions

- Założenie: token osobisty ClickUp idzie w nagłówku `Authorization` bez `Bearer` —
  potwierdzane testem manualnym realnym tokenem (Faza 2).
- Token w pliku plaintext jest świadomie zaakceptowany dla MVP (PRD Access Control); wymiana
  na bezpieczne przechowywanie to przyszły slice.

## Success Criteria (Summary)

- Token zapisany przeżywa restart; sprawdzenie łączności realnym tokenem zwraca tożsamość.
- Błędny token daje czytelny `TOKEN_REJECTED`; brak sieci → `UNREACHABLE`; brak tokenu → `NOT_CONFIGURED`.
- `mvn test` zielony (ścieżki OK/401/brak-tokenu/sieć + testy kontrolera), bez realnych wywołań API.
