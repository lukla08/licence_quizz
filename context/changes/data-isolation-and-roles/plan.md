# Izolacja danych i role dostępu (F-01) — Implementation Plan

## Overview

Ten plan tworzy fundament danych i kontroli dostępu dla LicenceQuizz. Po jego
realizacji baza ma minimalny, ale kompletny schemat domenowy (kategorie, pytania,
opcje odpowiedzi, tagi trudności per użytkownik, sesje i kolejkę pytań z
odpowiedziami), z **izolacją per-user wymuszoną w bazie przez RLS** oraz flagą
administratora czytaną przez polityki. Dochodzi seed (~12 pytań tekstowych) i
minimalne wpięcie w aplikację: wygenerowane typy DB i rola-świadome `App.Locals`.

To jest enabler — nie dostarcza widocznej dla użytkownika funkcji. Odblokowuje
S-01 (`first-playable-session`) i S-04 (`admin-question-cms`).

## Current State Analysis

- **Brak jakichkolwiek migracji** — `supabase/migrations/` nie istnieje; to pierwsza migracja projektu. `supabase/config.toml` obecny, CLI `supabase` w devDependencies.
- **Tylko klucz `anon`** w `astro.config.mjs` (`SUPABASE_KEY`, potwierdzone w `infrastructure.md` jako `anon public`). **Brak `service_role`.** Konsekwencja: klucz `anon` podlega RLS, więc sprawdzenie roli admina musi żyć w bazie (helper SECURITY DEFINER), a nie w server-side bypassie.
- **Signup** (`src/pages/api/auth/signup.ts`) używa `supabase.auth.signUp` → tworzy wiersz w `auth.users` + flow potwierdzenia e-mail. To naturalne miejsce na trigger tworzący wiersz `profiles`.
- **`src/lib/supabase.ts`** tworzy SSR-owy klient z cookies (sesja usera niesie JWT do PostgREST — paliwo dla `auth.uid()` w RLS).
- **`src/middleware.ts`** ustawia `context.locals.user`, ale nie zna roli. `PROTECTED_ROUTES = ["/dashboard"]`.
- **`src/env.d.ts`** — `App.Locals` ma tylko `user`. Brak `src/types.ts` (CLAUDE.md: tu trafiają współdzielone typy).

## Desired End State

- Migracja schematu + RLS zaaplikowana lokalnie (Docker) i wypchnięta na hostowany projekt (`supabase db push`).
- Każda tabela ma włączony RLS i komplet granularnych polityk per-operacja/per-rola (konwencja CLAUDE.md).
- Dane prywatne (`difficulty_tags`, `sessions`, `session_questions`) są fizycznie niedostępne między kontami — zweryfikowane testem dwóch użytkowników (user A pyta o dane usera B → 0 wierszy).
- Dane współdzielone (`categories`, `questions`, `answer_options`) są czytelne dla każdego zalogowanego, ale zapisywalne tylko przez admina (flaga `profiles.is_admin`).
- Seed ~12 pytań w 3 kategoriach dostępny; `video_ref` = NULL we wszystkich.
- Aplikacja ma wygenerowane typy DB w `src/types.ts` i `App.Locals.isAdmin` ustawiane w middleware.

### Key Discoveries:

- Brak `service_role` → admin rozwiązany flagą w bazie + helper `is_admin()` (`astro.config.mjs:17-22`).
- Trigger na `auth.users` to standardowy wzór Supabase na zasilenie `profiles` przy signup (`src/pages/api/auth/signup.ts:13`).
- `session_questions` nie ma własnego `user_id` — własność dziedziczy po `sessions` (polityka przez subquery `exists`).
- Konwencja migracji: `supabase/migrations/YYYYMMDDHHmmss_short_description.sql`, RLS obowiązkowy (CLAUDE.md).

## What We're NOT Doing

- **Logika pętli sesji / wznawiania** — F-01 dostarcza tabele; orkiestrację (kolejkowanie pytań, resume, podsumowanie) buduje S-01.
- **Tryby doboru pytań** (wg tagów / najbardziej kłopotliwe) — to S-03; F-01 tylko zapewnia, że dane historyczne da się odpytać.
- **Cokolwiek związanego z wideo** — `video_ref` jest NULL i nieużywany; format/źródło wideo to otwarte pytanie blokujące S-02/S-04.
- **Panel admina (UI)** — F-01 daje flagę roli i polityki zapisu; ekran CMS to S-04.
- **Service-role / operacje serwerowe omijające RLS** — celowo poza zakresem; cały dostęp przez klucz `anon` + sesję.
- **Zapamiętywanie ostatniej konfiguracji sesji** — pomysł z PRD (Socrates), nie-konieczne; ewentualnie później.

## Implementation Approach

Migracja-najpierw: jeden plik migracji tworzy cały schemat, helper, trigger i polityki,
testowany na lokalnej bazie (Docker) zanim dotknie hostowanego projektu. Seed jako
osobny, idempotentny artefakt. Wpięcie w aplikację minimalne (typy + rola w locals),
bez warstwy serwisowej dostępu do danych (to przyjdzie z S-01). Na końcu `db push`
na hostowany projekt i smoke na żywym środowisku.

## Critical Implementation Details

- **Rekurencja RLS przy sprawdzaniu admina** — helper `is_admin()` musi być `SECURITY DEFINER` z ustawionym `search_path`, by odczyt `profiles` w środku polityki nie wyzwalał ponownie RLS na `profiles`. Bez tego polityki zapisu `questions` wpadną w rekurencję lub odmowę.
- **Ownership `session_questions` jest pośrednia** — polityka używa `exists (select 1 from sessions s where s.id = session_id and s.user_id = auth.uid())` zarówno w `using`, jak i `with check`. Pominięcie `with check` przepuści wstawienie cudzego `session_id`.
- **Poprawność liczona przez join (decyzja Q6)** — `session_questions` NIE przechowuje `is_correct`; wynika z `selected_option_id → answer_options.is_correct`. Skutek do odnotowania: późniejsza edycja poprawnej opcji przez admina retroaktywnie zmienia statystyki błędów (S-03). Świadomy wybór, patrz Open Risks.
- **Trigger na `auth.users`** musi być idempotentny względem ponownego signup/confirm i odporny na brak uprawnień — `SECURITY DEFINER`, `on delete cascade` na FK `profiles.user_id`.

## Phase 1: Schemat + RLS (migracja)

### Overview

Jedna migracja tworząca komplet tabel, helper admina, trigger profilu i wszystkie
polityki RLS. Zaaplikowana i zweryfikowana na lokalnej bazie (Docker).

### Changes Required:

#### 1. Migracja schematu i polityk

**File**: `supabase/migrations/<ts>_init_data_isolation_and_roles.sql`

**Intent**: Założyć cały model danych F-01 z włączonym RLS i granularnymi politykami per-operacja/per-rola, helperem `is_admin()` oraz triggerem zasilającym `profiles` przy signup.

**Contract**: Tabele i klucze:
- `profiles(user_id uuid pk → auth.users on delete cascade, is_admin boolean not null default false, created_at timestamptz)`.
- `categories(id uuid pk, name text not null unique, created_at)`.
- `questions(id uuid pk, category_id uuid not null → categories, prompt text not null, video_ref text null, created_at)`.
- `answer_options(id uuid pk, question_id uuid not null → questions on delete cascade, position smallint not null, text text not null, is_correct boolean not null default false)`.
- `difficulty_tags(user_id → auth.users on delete cascade, question_id → questions on delete cascade, difficulty smallint not null check (difficulty between 1 and 3), updated_at, pk(user_id, question_id))`.
- `sessions(id uuid pk, user_id → auth.users on delete cascade, config jsonb not null, status text not null default 'in_progress', created_at, completed_at null)`.
- `session_questions(id uuid pk, session_id → sessions on delete cascade, question_id → questions, position smallint not null, selected_option_id uuid null → answer_options, answered_at timestamptz null)`.

Helper + trigger:
- `public.is_admin() returns boolean language sql security definer stable set search_path = public` → `coalesce((select is_admin from profiles where user_id = auth.uid()), false)`.
- `public.handle_new_user()` (SECURITY DEFINER) wstawia wiersz `profiles(user_id)`; trigger `after insert on auth.users`.

Polityki (wszystkie `to authenticated`; `enable row level security` na każdej tabeli):
- `profiles`: SELECT własny (`auth.uid() = user_id`); brak insert/update/delete dla userów (wiersz z triggera, `is_admin` nadaje dev SQL-em).
- `categories`, `questions`, `answer_options`: SELECT `using(true)`; INSERT/UPDATE/DELETE `with check`/`using` = `public.is_admin()`.
- `difficulty_tags`, `sessions`: `for all` z `using` i `with check` = `auth.uid() = user_id`.
- `session_questions`: `for all` z `using`/`with check` = `exists(select 1 from sessions s where s.id = session_id and s.user_id = auth.uid())`.

#### 2. Lokalna baza

**File**: (workflow, bez pliku) `npx supabase start`

**Intent**: Uruchomić lokalną bazę przez Docker i zaaplikować migrację, by testować RLS przed produkcją.

**Contract**: `supabase start` → `supabase db reset` (aplikuje migracje od zera). Brak zmian w `config.toml` oczekiwany.

### Success Criteria:

#### Automated Verification:

- Lokalna baza wstaje: `npx supabase start`
- Migracja aplikuje się czysto od zera: `npx supabase db reset`
- Lint/format bez błędów: `npm run lint`
- Skrypt izolacji RLS przechodzi: dwóch testowych userów; user A `select` na `difficulty_tags`/`sessions`/`session_questions` usera B zwraca 0 wierszy; `questions`/`categories`/`answer_options` widoczne dla obu; INSERT do `questions` jako nie-admin odrzucony, jako admin przyjęty.

#### Manual Verification:

- W Supabase Studio (lokalnie) widać wszystkie 7 tabel z włączonym RLS i kompletem polityk.
- Ręczne nadanie admina (`update profiles set is_admin = true …`) skutkuje możliwością zapisu `questions` z poziomu tego konta.
- Trigger: nowy signup tworzy wiersz w `profiles` z `is_admin = false`.

**Implementation Note**: Po przejściu automatycznej weryfikacji zatrzymaj się i poczekaj na ręczne potwierdzenie, zanim przejdziesz do Fazy 2.

---

## Phase 2: Seed

### Overview

Idempotentny seed: 3 kategorie + ~12 pytań tekstowych z opcjami odpowiedzi.
`video_ref` NULL we wszystkich.

### Changes Required:

#### 1. Plik seeda

**File**: `supabase/seed.sql`

**Intent**: Dostarczyć dość danych, by S-01 (pętla) i S-03 (tryby doboru) dało się sensownie przetestować, bez przesądzania decyzji o wideo.

**Contract**: Idempotentny SQL (`insert … on conflict do nothing` lub `where not exists`): 3 wiersze `categories`; ~12 `questions` (każde z `category_id`, `prompt`, `video_ref = null`); dla każdego pytania 3–4 `answer_options` z dokładnie jednym `is_correct = true`. Stałe UUID-y, by seed był powtarzalny. `supabase/config.toml` wskazuje `seed.sql` (domyślnie tak jest) — zweryfikować.

### Success Criteria:

#### Automated Verification:

- Seed aplikuje się przy resecie: `npx supabase db reset` (uruchamia `seed.sql`)
- Liczności zgodne: 3 kategorie, ~12 pytań, każde pytanie ma ≥3 opcje i dokładnie 1 poprawną (zapytanie kontrolne zwraca 0 pytań z liczbą poprawnych ≠ 1)
- Ponowny `db reset` nie duplikuje wierszy (idempotencja)

#### Manual Verification:

- Pytania rozłożone na 3 kategorie (nie wszystkie w jednej) — wystarczające do testu trybu „wg kategorii".
- Treść pytań sensowna (tematyka prawa jazdy), opcje rozróżnialne.

**Implementation Note**: Zatrzymaj się na ręczne potwierdzenie przed Fazą 3.

---

## Phase 3: Wpięcie w aplikację

### Overview

Wygenerowanie typów DB do `src/types.ts` i rozszerzenie `App.Locals` o `isAdmin`
ustawiane w middleware. Minimalny kontrakt dla S-01/S-04 — bez warstwy serwisowej.

### Changes Required:

#### 1. Wygenerowane typy DB

**File**: `src/types.ts`

**Intent**: Dać downstream slice'om typowany dostęp do schematu (konwencja CLAUDE.md: współdzielone typy w `src/types.ts`).

**Contract**: Wygenerować typy z lokalnej bazy: `npx supabase gen types typescript --local` → zapis do `src/types.ts` (eksport `Database` + aliasy encji, np. `Question`, `Category`, `Session`). Bez ręcznego dopisywania pól.

#### 2. Rola-świadome locals

**File**: `src/middleware.ts`, `src/env.d.ts`

**Intent**: Udostępnić flagę admina w `context.locals`, by S-04 mógł bramkować trasy/UI, a S-01 odróżnić użytkownika.

**Contract**: `src/env.d.ts` — dodać `isAdmin: boolean` do `App.Locals`. `src/middleware.ts` — po rozwiązaniu `user`, jeśli zalogowany, odczytać `profiles.is_admin` przez istniejący klient supabase i ustawić `context.locals.isAdmin` (domyślnie `false` dla niezalogowanych). Nie zmieniać `PROTECTED_ROUTES` (trasy admina dojdą w S-04).

### Success Criteria:

#### Automated Verification:

- Typy generują się bez błędów: `npx supabase gen types typescript --local`
- Type-check przechodzi: `npm run build` (Astro `astro:check`/build)
- Lint przechodzi: `npm run lint`

#### Manual Verification:

- Po zalogowaniu kontem admina `context.locals.isAdmin === true`; kontem zwykłym `false`; niezalogowany `false`.
- `src/types.ts` zawiera wszystkie 7 tabel.

**Implementation Note**: Zatrzymaj się na ręczne potwierdzenie przed Fazą 4.

---

## Phase 4: Wdrożenie na hostowany Supabase

### Overview

Podpięcie lokalnego repo migracji do hostowanego projektu i wypchnięcie schematu +
seeda, następnie smoke na żywym środowisku.

### Changes Required:

#### 1. Link + push

**File**: (workflow) `supabase link`, `supabase db push`

**Intent**: Przenieść zweryfikowany lokalnie schemat na produkcyjny projekt Supabase, którego używa wdrożona aplikacja.

**Contract**: `npx supabase link --project-ref <ref>` (jednorazowo), następnie `npx supabase db push` (aplikuje migracje na hostowany projekt). Seed na produkcję wykonać świadomie (push migracji nie uruchamia `seed.sql` automatycznie — zaaplikować seed ręcznie jako SQL lub pominąć na produkcji, jeśli pytania pójdą później przez S-04). Decyzja: seed produkcyjny **tak** (potrzebny do działania S-01 na żywo).

### Success Criteria:

#### Automated Verification:

- Push bez błędów: `npx supabase db push`
- Status migracji zsynchronizowany: `npx supabase migration list` (lokalne == zdalne)

#### Manual Verification:

- W hostowanym Studio widać 7 tabel z RLS i politykami oraz dane seeda.
- Smoke izolacji na żywo: zalogowanie dwoma kontami przez wdrożoną aplikację; konto A nie widzi danych konta B (po pojawieniu się jakichkolwiek prywatnych wierszy — np. ręcznie wstawiony tag).
- Odczyt pytań działa dla zwykłego konta; zapis `questions` tylko dla admina.

**Implementation Note**: To ostatnia faza — po ręcznym potwierdzeniu fundament jest gotowy; zarchiwizuj zmianę przez `/10x-archive`, co przełączy F-01 na `done` w roadmapie.

---

## Testing Strategy

### Unit Tests:

- Projekt nie ma jeszcze frameworka testowego; weryfikacja RLS realizowana skryptem SQL (dwóch userów) uruchamianym na lokalnej bazie — pełni rolę testu jednostkowego izolacji.
- Kluczowe przypadki: cross-user SELECT (0 wierszy), zapis `questions` przez nie-admina (odmowa) i admina (sukces), trigger `profiles` przy signup, `with check` blokujący cudze `session_id` w `session_questions`.

### Integration Tests:

- Pełny przepływ na lokalnej bazie: signup → trigger tworzy profil → nadanie admina → admin wstawia pytanie → drugi user je czyta, ale nie edytuje → user taguje trudność → drugi user nie widzi taga.

### Manual Testing Steps:

1. `supabase start` + `db reset`; otwórz Studio, potwierdź 7 tabel + RLS + polityki.
2. Utwórz dwóch userów; nadaj jednemu `is_admin`.
3. Jako admin wstaw `questions`/`answer_options`; jako user — odczyt OK, zapis odrzucony.
4. Jako user A wstaw `difficulty_tags`/`sessions`; jako user B potwierdź 0 wierszy cross-user.
5. Po `db push` powtórz smoke izolacji na hostowanym projekcie przez wdrożoną aplikację.

## Performance Considerations

Skala mała (PRD: users small, qps low). Brak budżetu wydajnościowego. Jedyna uwaga:
polityka `session_questions` używa subquery `exists` — przy małych danych bez znaczenia;
indeks `session_questions(session_id)` i `difficulty_tags(user_id)` warto dodać w migracji
dla porządku, ale to nie hotspot.

## Migration Notes

- Pierwsza migracja projektu — tworzy `supabase/migrations/`. Naming wg konwencji CLAUDE.md.
- Migracje na hostowany projekt przez `supabase db push` (nie przez CI; CI auto-deploy dotyczy kodu aplikacji, nie schematu DB).
- Rollback: schemat odwracalny ręcznie (drop w odwrotnej kolejności FK); na MVP akceptowalne, brak danych produkcyjnych poza seedem.

## References

- Roadmap: `context/foundation/roadmap.md` → F-01
- PRD: `context/foundation/prd.md` → FR-001, FR-002, Access Control, guardrail izolacji, Business Logic
- Stack: `context/foundation/tech-stack.md`; Infra: `context/foundation/infrastructure.md`
- Istniejący auth/middleware: `src/middleware.ts`, `src/lib/supabase.ts`, `src/pages/api/auth/signup.ts`
- Konwencje: `CLAUDE.md` (migracje, RLS, src/types.ts)

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Schemat + RLS (migracja)

#### Automated

- [x] 1.1 Lokalna baza wstaje (`npx supabase start`)
- [x] 1.2 Migracja aplikuje się czysto od zera (`npx supabase db reset`)
- [x] 1.3 Lint/format bez błędów (`npm run lint`)
- [x] 1.4 Skrypt izolacji RLS przechodzi (cross-user 0 wierszy; zapis questions tylko admin)

#### Manual

- [x] 1.5 Studio: 7 tabel z RLS i kompletem polityk
- [x] 1.6 Ręczne nadanie admina umożliwia zapis questions
- [x] 1.7 Nowy signup tworzy wiersz profiles (is_admin=false)

### Phase 2: Seed

#### Automated

- [ ] 2.1 Seed aplikuje się przy `db reset`
- [ ] 2.2 Liczności zgodne (3 kategorie, ~12 pytań, dokładnie 1 poprawna opcja na pytanie)
- [ ] 2.3 Ponowny `db reset` nie duplikuje wierszy (idempotencja)

#### Manual

- [ ] 2.4 Pytania rozłożone na 3 kategorie
- [ ] 2.5 Treść i opcje sensowne

### Phase 3: Wpięcie w aplikację

#### Automated

- [ ] 3.1 Typy generują się bez błędów (`supabase gen types typescript --local`)
- [ ] 3.2 Type-check/build przechodzi (`npm run build`)
- [ ] 3.3 Lint przechodzi (`npm run lint`)

#### Manual

- [ ] 3.4 `locals.isAdmin` poprawne dla admina/usera/niezalogowanego
- [ ] 3.5 `src/types.ts` zawiera wszystkie 7 tabel

### Phase 4: Wdrożenie na hostowany Supabase

#### Automated

- [ ] 4.1 Push bez błędów (`npx supabase db push`)
- [ ] 4.2 Status migracji zsynchronizowany (`supabase migration list`)

#### Manual

- [ ] 4.3 Hostowane Studio: 7 tabel + RLS + dane seeda
- [ ] 4.4 Smoke izolacji na żywo (konto A nie widzi danych konta B)
- [ ] 4.5 Odczyt pytań dla usera, zapis tylko dla admina
