---
project: "LicenceQuizz"
version: 1
status: draft
created: 2026-06-01
updated: 2026-06-01
prd_version: 1
main_goal: low-complexity
top_blocker: capacity
---

# Roadmap: LicenceQuizz

> Derived from `context/foundation/prd.md` (v1) + auto-researched codebase baseline.
> Edit-in-place; archive when superseded.
> Slices below are listed in dependency order. The "At a glance" table is the index.

## Vision recap

Kandydaci na egzamin na prawo jazdy nie mają jednego narzędzia łączącego trzy
funkcje efektywnej nauki: przerwanie odtwarzania pytania filmowego, tagowanie
pytań własną skalą trudności oraz powtarzanie pytań wg trudności / liczby
błędów. Wyróżnikiem produktu (cechą, której usunięcie sprawia, że aplikacja
staje się zwykłym quizem) jest właśnie zebranie tej trójki w jednym miejscu —
żadna znana aplikacja tego nie ma. MVP jest web-only, dla małej, znanej grupy
(użytkownik + rodzina/znajomi), solo i po godzinach w oknie 3 tygodni.

## North star

**S-01: Użytkownik przechodzi pierwszą pełną sesję quizu na ręcznie wprowadzonych pytaniach** — to najmniejsza pełna pętla, która udowadnia rdzeń produktu i jednocześnie generuje dane (tagi trudności + historia błędów), bez których inteligentny dobór pytań nie ma na czym działać; zgodnie z celem `low-complexity` budujemy ją w minimalnym wariancie (tryb „wszystkie", pytania bez specjalnej obsługi przerwania wideo).

> „Gwiazda przewodnia" (north star) = najmniejszy kompletny, widoczny dla
> użytkownika przepływ, którego dostarczenie udowadnia główną hipotezę produktu;
> umieszczony tak wcześnie, jak pozwalają zależności, bo cała reszta ma sens
> tylko jeśli ten przepływ działa.

## At a glance

| ID   | Change ID                   | Outcome (użytkownik może …)                                                              | Prerequisites          | PRD refs                               | Status   |
| ---- | --------------------------- | ---------------------------------------------------------------------------------------- | ---------------------- | -------------------------------------- | -------- |
| F-01 | data-isolation-and-roles    | (foundation) izolacja RLS między kontami + flaga roli admin + minimalny schemat pytań/tagów/prób | —              | FR-001, FR-002, NFR (prywatność), Access Control | ready    |
| S-01 | first-playable-session      | skonfigurować i przejść pełną sesję: odpowiedź + tag trudności + natychmiastowy zapis + podsumowanie + nowa sesja | F-01   | US-01, FR-003 (tryb „wszystkie"), FR-005, FR-006, FR-007, FR-008 | proposed |
| S-02 | interruptible-video-playback | przerwać odtwarzanie wideo pytania w dowolnym momencie                                   | S-01, decyzja: źródło/format wideo | FR-004                     | blocked  |
| S-03 | adaptive-question-selection | dobrać pytania do sesji wg tagów trudności lub historii błędów                            | S-01                   | FR-003 (tryby: wg tagów / najbardziej kłopotliwe) | proposed |
| S-04 | admin-question-cms          | (admin) zarządzać bazą pytań wraz z wideo przez oddzielny panel CMS                       | F-01, decyzja: źródło/format wideo | FR-010                     | blocked  |

## Streams

Pomoc nawigacyjna — grupuje elementy dzielące łańcuch Prerequisites. Kanoniczna kolejność wynika z grafu zależności poniżej; ta tabela to proponowana kolejność czytania w równoległych torach.

| Stream | Theme            | Chain                                  | Note                                                                                   |
| ------ | ---------------- | -------------------------------------- | -------------------------------------------------------------------------------------- |
| A      | Pętla quizu      | `F-01` → `S-01` → `S-02` / `S-03`      | Rdzeń produktu; po `S-01` gałęzie `S-02` i `S-03` są równoległe (osobne przebiegi agenta). |
| B      | Panel admina     | `S-04`                                 | Dołącza do Stream A przy `F-01`; równoległy do `S-01`, ale wstrzymany decyzją o wideo.  |

## Baseline

Co jest już w kodzie na dzień `2026-06-01` (auto-research + potwierdzenie użytkownika).
Foundations poniżej zakładają obecność tych warstw i ich NIE odtwarzają.

- **Frontend:** present — Astro 6 SSR + React 19 islands, Tailwind 4, shadcn/ui; formularze auth gotowe (`src/components/auth/*`). Brak UI quizu/admina.
- **Backend / API:** present — trasy API SSR tylko dla auth (`src/pages/api/auth/{signin,signup,signout}.ts`). Brak endpointów quizu/admina.
- **Data:** absent — Supabase podpięte (`supabase/config.toml`), ale ZERO migracji i brak tabel domenowych (pytania, tagi, sesje, odpowiedzi). Tylko tabele auth zarządzane przez Supabase.
- **Auth:** partial — `@supabase/ssr` + `src/middleware.ts` rozwiązuje użytkownika, strony/endpointy auth działają. Brak flagi roli admin i izolacji RLS per-user (wymaga migracji).
- **Deploy / infra:** present — Cloudflare Workers, live pod `licence-quizz.licquizz.workers.dev`, CI auto-deploy aktywne.
- **Observability:** absent — `wrangler tail` dostępny operacyjnie; brak logowania / error trackingu w kodzie.

## Foundations

### F-01: Izolacja danych i role dostępu

- **Outcome:** (foundation) minimalny kontrakt kontroli dostępu i izolacji: flaga roli admin na koncie, polityki RLS izolujące dane per-użytkownik oraz najmniejszy schemat (pytania z generycznym odniesieniem do wideo, tagi trudności, próby/odpowiedzi) zasilony kilkoma ręcznie wprowadzonymi pytaniami.
- **Change ID:** data-isolation-and-roles
- **PRD refs:** FR-001, FR-002, Access Control, NFR (dane użytkownika nieudostępniane osobom trzecim), Guardrail (tagi i historia błędów izolowane między kontami)
- **Unlocks:** S-01 (sesja zapisuje izolowane tagi/odpowiedzi i czyta seedowane pytania), S-04 (bramka roli admin), ścieżka weryfikacji guardrailu „jedno konto nie widzi danych drugiego"
- **Prerequisites:** —
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Sekwencjonowane jako pierwsze, bo każdy slice zapisujący dane użytkownika zależy od izolacji RLS i guardrailu prywatności. Ryzyko: rozdęcie do „pełnej warstwy danych" — trzymamy minimum, które czyta/zapisuje pierwsza sesja (kolumna wideo generyczna, `nullable`, by nie czekać na decyzję o źródle wideo).
- **Status:** ready

## Slices

### S-01: Pierwsza grywalna sesja quizu (gwiazda przewodnia)

- **Outcome:** użytkownik może skonfigurować sesję (liczba pytań + kategoria, tryb „wszystkie"), przejść kolejne pytania odpowiadając i tagując trudność, z natychmiastowym zapisem każdej odpowiedzi, a po ostatnim pytaniu zobaczyć podsumowanie (liczba błędów) i uruchomić nową sesję.
- **Change ID:** first-playable-session
- **PRD refs:** US-01, FR-003 (tryb „wszystkie"), FR-005, FR-006, FR-007, FR-008, NFR (nawigacja z klawiatury), Guardrail (każda odpowiedź zapisywana natychmiast)
- **Prerequisites:** F-01
- **Parallel with:** S-04 (zależy tylko od F-01, nie od S-01)
- **Blockers:** —
- **Unknowns:**
  - Czy zapamiętywać ostatnią konfigurację sesji, by zmniejszyć tarcie ekranu konfiguracji? (Socrates do FR-003) — Owner: user. Block: no.
  - Jakie kategorie pytań przyjmuje seed i jak są definiowane? — Owner: user. Block: no.
- **Risk:** Spina produktu — po dostarczeniu generuje tagi + historię błędów, których potrzebuje S-03. Celowo wyłączamy z niej specjalną obsługę przerwania wideo (FR-004 → S-02), by nie zablokować rdzenia nierozstrzygniętą decyzją o źródle wideo; pytania mogą być na razie tekstowe/obrazkowe lub z wideo bez logiki przerwania.
- **Status:** proposed

### S-02: Przerwanie odtwarzania wideo

- **Outcome:** użytkownik może przerwać odtwarzanie wideo pytania w dowolnym momencie, bez czekania na koniec materiału.
- **Change ID:** interruptible-video-playback
- **PRD refs:** FR-004
- **Prerequisites:** S-01, decyzja: źródło/format wideo
- **Parallel with:** S-03 (oba zależą od S-01, żaden nie blokuje drugiego)
- **Blockers:** Decyzja o źródle/formacie wideo (hosting zewnętrzny vs przechowywanie w bazie) — własność: user
- **Unknowns:**
  - Jakie jest źródło i format wideo? Standardowy plik HTML5 (pause trywialny) czy osadzony odtwarzacz, np. YouTube (przerwanie przez API odtwarzacza)? — Owner: user. Block: yes.
- **Risk:** To pierwotna bolączka produktu (FR-004), ale sposób przerwania zależy wprost od formatu wideo; planowanie przed decyzją groziłoby przepisaniem warstwy odtwarzania. Wstrzymane do rozstrzygnięcia otwartego pytania PRD.
- **Status:** blocked

### S-03: Inteligentny dobór pytań

- **Outcome:** użytkownik może dobrać pytania do sesji wg nadanych wcześniej tagów trudności lub wg historii błędów (najbardziej kłopotliwe — największa liczba błędnych odpowiedzi).
- **Change ID:** adaptive-question-selection
- **PRD refs:** FR-003 (tryby: wg tagów trudności / najbardziej kłopotliwe), Business Logic
- **Prerequisites:** S-01
- **Parallel with:** S-02 (oba zależą od S-01, żaden nie blokuje drugiego)
- **Blockers:** —
- **Unknowns:**
  - Jak rozstrzygać remisy / brak danych historycznych dla pytania w trybie „najbardziej kłopotliwe"? — Owner: user. Block: no.
- **Risk:** Drugi rdzeniowy wyróżnik, ale ma wartość dopiero, gdy istnieje historia z S-01; dlatego sekwencjonowany po gwieździe przewodniej. Ryzyko niskie — żadna zewnętrzna zależność, logika doboru działa na danych z bazy.
- **Status:** proposed

### S-04: Panel CMS administratora

- **Outcome:** administrator (konto z flagą admin) może dodawać, edytować i usuwać pytania wraz z wideo przez oddzielny panel CMS.
- **Change ID:** admin-question-cms
- **PRD refs:** FR-010, Access Control (rola Admin)
- **Prerequisites:** F-01, decyzja: źródło/format wideo
- **Parallel with:** S-01 (oba zależą tylko od F-01)
- **Blockers:** Decyzja o źródle/formacie wideo — przesądza, jak admin podpina/przesyła wideo do pytania (własność: user)
- **Unknowns:**
  - Jak admin podpina wideo do pytania — link do hostingu zewnętrznego czy upload do bazy/storage? — Owner: user. Block: yes.
- **Risk:** Must-have (bez edycji pytań aplikacja jest nieużyteczna po fazie ręcznego seeda), ale część „wraz z wideo" zależy od tej samej nierozstrzygniętej decyzji co S-02. Sam CRUD metadanych + bramka roli admin byłby planowalny, lecz FR-010 wprost obejmuje wideo, więc slice pozostaje wstrzymany do decyzji.
- **Status:** blocked

## Backlog Handoff

| Roadmap ID | Change ID                    | Suggested issue title                                  | Ready for `/10x-plan` | Notes |
| ---------- | ---------------------------- | ------------------------------------------------------ | --------------------- | ----- |
| F-01       | data-isolation-and-roles     | Izolacja RLS per-user + rola admin + minimalny schemat | yes                   | Run `/10x-plan data-isolation-and-roles` — odblokowuje S-01 (gwiazda) |
| S-01       | first-playable-session       | Pierwsza grywalna sesja quizu (gwiazda przewodnia)     | no                    | Czeka aż F-01 będzie `done` |
| S-02       | interruptible-video-playback | Przerwanie odtwarzania wideo pytania                   | no                    | Wstrzymane: decyzja o źródle/formacie wideo |
| S-03       | adaptive-question-selection  | Inteligentny dobór pytań (wg tagów / wg błędów)        | no                    | Czeka aż S-01 będzie `done` |
| S-04       | admin-question-cms           | Panel CMS administratora (pytania + wideo)             | no                    | Wstrzymane: decyzja o źródle/formacie wideo |

## Open Roadmap Questions

1. **Jakie jest źródło i format wideo dla pytań egzaminacyjnych?** — Owner: user. Block: S-02, S-04 (oraz kształt kolumny wideo w F-01 — tam rozwiązane generyczną, nullable kolumną, więc F-01 nie jest blokowane). Najbardziej dźwigniowe pytanie roadmapy: jego rozstrzygnięcie odblokowuje dwa slice'y.

## Parked

- **Statystyki między sesjami (trend błędów w czasie, FR-009)** — Why parked: PRD §Non-Goals + Priority nice-to-have; wartość pojawia się dopiero po kilku sesjach, gdy istnieją realne dane.
- **Natywna aplikacja mobilna (iOS/Android)** — Why parked: PRD §Non-Goals; MVP wyłącznie web, mobile rozważane post-MVP.
- **Publiczna rejestracja / otwarta platforma** — Why parked: PRD §Non-Goals; konta tylko dla znanej grupy, role nadawane ręcznie przez dewelopera.
- **Pełna baza pytań egzaminacyjnych** — Why parked: PRD §Non-Goals; MVP zawiera wąski, ręcznie wprowadzony zestaw; pełna baza to rozbudowa post-MVP (przez S-04).

## Done

(Pusta przy pierwszej generacji. `/10x-archive` dopisuje wpis tutaj — i przełącza `Status` elementu na `done` — gdy archiwizowana zmiana ma `Change ID` zgodny z elementem roadmapy. NIE wypełniać ręcznie.)
