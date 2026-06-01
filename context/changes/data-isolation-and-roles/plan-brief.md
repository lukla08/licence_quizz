# Izolacja danych i role dostępu (F-01) — Plan Brief

> Full plan: `context/changes/data-isolation-and-roles/plan.md`

## What & Why

Fundament danych i kontroli dostępu dla LicenceQuizz. Tworzy minimalny schemat
domenowy z izolacją per-user wymuszoną w bazie (RLS) i flagą administratora,
plus seed i minimalne wpięcie w aplikację. To enabler bez widocznej funkcji —
odblokowuje gwiazdę przewodnią S-01 (pierwsza grywalna sesja) oraz S-04 (panel CMS).

## Starting Point

Wdrożona aplikacja to dziś tylko szkielet startera + działający auth Supabase
(`@supabase/ssr`, middleware ustawia `locals.user`). **Zero migracji**, brak tabel
domenowych, brak roli admina, brak `src/types.ts`. Tylko klucz `anon` (brak service_role).

## Desired End State

Baza ma 7 tabel z włączonym RLS i kompletem granularnych polityk: dane prywatne
(tagi, sesje, odpowiedzi) fizycznie nieosiągalne między kontami; dane współdzielone
(kategorie, pytania, opcje) czytelne dla zalogowanych, zapisywalne tylko przez admina.
Seed ~12 pytań działa lokalnie i na hostowanym projekcie; aplikacja ma typy DB i
`locals.isAdmin`.

## Key Decisions Made

| Decision | Choice | Why (1 sentence) | Source |
| --- | --- | --- | --- |
| Opcje odpowiedzi | Osobna tabela `answer_options` | Typowane i czyste dla CMS (S-04) oraz zapytań o poprawność | Plan |
| Rola admina | `profiles.is_admin` + trigger + helper `is_admin()` | Brak service_role → sprawdzenie musi żyć w bazie/RLS | Plan |
| Skala trudności | 3-stopniowa (smallint 1–3) | Szybkie tagowanie z klawiatury; wystarcza dla S-03 | Plan |
| Kategoria | Osobna tabela `categories` | Admin dodaje kategorie bez migracji; spójność referencyjna | Plan |
| Stan sesji | `sessions` + `session_questions` (kolejka+odpowiedzi) | Pełna wznawialność (guardrail „nie kasuj trwającej sesji") | Plan |
| Poprawność | Liczona przez join (bez denormalizacji) | Jedno źródło prawdy; user świadomy skutku retroaktywnego | Plan |
| Seed | ~12 pytań tekstowych, `video_ref` NULL | Dość danych do testu, bez przesądzania decyzji o wideo | Plan |
| Migracje | Lokalnie (Docker) → `supabase db push` | Wersjonowanie w repo + lokalny test RLS przed produkcją | Plan |

## Scope

**In scope:** schemat (7 tabel), helper admina + trigger profilu, komplet polityk RLS,
seed, wygenerowane typy DB, `locals.isAdmin`, wdrożenie na hostowany projekt.

**Out of scope:** logika pętli/wznawiania sesji (S-01), tryby doboru (S-03), cokolwiek
z wideo (`video_ref` NULL), UI panelu admina (S-04), service_role/bypass RLS.

## Architecture / Approach

Migracja-najpierw: jeden plik tworzy cały schemat + RLS + helper + trigger, testowany
na lokalnej bazie (Docker) przed dotknięciem produkcji. Izolacja oparta o `auth.uid()`
z podpisanego JWT sesji (klucz `anon` podlega RLS); admin przez flagę w `profiles`
czytaną helperem `SECURITY DEFINER`. Dane prywatne filtrowane `auth.uid() = user_id`;
`session_questions` dziedziczy własność po sesji (subquery). Seed i wpięcie w app minimalne.

## Phases at a Glance

| Phase | What it delivers | Key risk |
| --- | --- | --- |
| 1. Schemat + RLS | Migracja: tabele, helper, trigger, polityki + weryfikacja izolacji | Rekurencja RLS w helperze admina jeśli nie SECURITY DEFINER |
| 2. Seed | 3 kategorie + ~12 pytań + opcje, idempotentnie | Niespójność „dokładnie 1 poprawna opcja" |
| 3. Wpięcie w app | Typy DB w `src/types.ts` + `locals.isAdmin` | Dodatkowe zapytanie o rolę w middleware |
| 4. Wdrożenie | `link` + `db push` + smoke na żywo | Seed produkcyjny nie idzie z push automatycznie |

**Prerequisites:** Docker (lokalny Supabase), dostęp do hostowanego projektu (`supabase link`).
**Estimated effort:** ~1–2 sesje przez 4 fazy.

## Open Risks & Assumptions

- **Q6 — poprawność przez join:** edycja poprawnej odpowiedzi przez admina retroaktywnie zmienia historię błędów (S-03). Świadomy wybór; gdyby przeszkadzał — przejście na denormalizację `is_correct` w `session_questions`.
- **Seed produkcyjny:** `db push` nie uruchamia `seed.sql`; seed na hostowany projekt aplikujemy świadomie (Faza 4), bo S-01 na żywo potrzebuje pytań.
- **Brak frameworka testowego:** izolację RLS weryfikuje skrypt SQL dwóch userów, nie suite testowy.
- **Decyzja o wideo wciąż otwarta** — nie blokuje F-01 (kolumna nullable), blokuje S-02/S-04.

## Success Criteria (Summary)

- Konto A nie widzi prywatnych danych konta B (zweryfikowane lokalnie i na żywo).
- Zwykłe konto czyta pytania, ale ich nie zapisze; konto admin zapisze.
- Seed ~12 pytań w 3 kategoriach dostępny; aplikacja typuje schemat i zna rolę usera.
