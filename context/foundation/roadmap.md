---
project: ClickUp Simplifier
version: 1
status: active
created: 2026-06-20
updated: 2026-06-20
prd_version: 1
main_goal: low-complexity
top_blocker: decisions
---

# Roadmap: ClickUp Simplifier

> Wyprowadzona z `context/foundation/prd.md` (v1) + auto-zbadanego baseline'u kodu.
> Edytuj w miejscu; archiwizuj, gdy zdezaktualizowana.
> Plasterki poniżej są w kolejności zależności. Tabela „W skrócie" jest indeksem.

## Vision recap

Jednoosobowe, lokalne narzędzie dla power-usera ClickUp, który męczy się z
przeładowanym i nieprzewidywalnym klientem online. Sedno: praca na *lokalnej
kopii* danych za szybkim, sterowanym z klawiatury interfejsem — wąski, spójny,
przewidywalny zestaw operacji bije pełną powierzchnię funkcji ClickUp.
Synchronizacja lokalnej kopii z ClickUp (w obie strony, z bezpiecznym zapisem
zwrotnym) jest wymaganą częścią rozwiązania, nie dodatkiem.

Architektura (z `tech-stack.md`): wspólny rdzeń w Spring/Java (lokalna kopia +
silnik sync + model domenowy) z wymiennymi klientami. Rdzeń jest client-agnostyczny.
Kolejność klientów **rozstrzygnięta (2026-06-20, OQ-1): JavaFX (desktop-java) pierwszy,
potem Flutter, potem web.** Logika domenowa zostaje w rdzeniu, by frontendy były wymienne.

## North star

**S-04: użytkownik tworzy milestone i przypisuje do niego zadania z klawiatury** —
to gwiazda przewodnia, czyli najmniejszy przepływ end-to-end, którego udane
dostarczenie dowodzi rdzenia hipotezy produktu (model milestone→task działa szybko
i przewidywalnie), ustawiony tak wcześnie, jak pozwalają zależności. Pełna
akceptacja US-01 domyka się dopiero z bezpiecznym zapisem zwrotnym (S-05).

## At a glance

| ID    | Change ID                          | Outcome (użytkownik …)                                              | Prerequisites | PRD refs                          | Status   |
| ----- | ---------------------------------- | ------------------------------------------------------------------- | ------------- | --------------------------------- | -------- |
| F-01  | clickup-token-and-connectivity     | (fundament) przechowuje token i ma uwierzytelnioną łączność z API   | —             | FR-001                            | ready    |
| F-02  | local-copy-persistence             | (fundament) ma minimalny lokalny magazyn na kopię workspace         | —             | FR-008, NFR                       | ready    |
| F-03  | first-client-shell                 | (fundament) klient JavaFX wpięty w rdzeń, baza nawigacji klawiaturą  | —             | FR-007, US-01                     | ready    |
| S-01  | full-workspace-pull                | pobiera całą przestrzeń ClickUp do lokalnej kopii (2 zestawy sync)  | F-01, F-02    | FR-002, FR-003                    | proposed |
| S-02  | incremental-sync-and-manual-trigger| utrzymuje kopię aktualną przyrostowo i wyzwala zestaw na żądanie    | S-01          | FR-004                            | proposed |
| S-03  | keyboard-milestone-task-nav        | nawiguje klawiaturą po milestone→task w wybranym kontekście         | S-01, F-03    | FR-005, FR-007, FR-008            | proposed |
| S-04  | create-milestone-and-assign        | tworzy milestone i przypisuje zadania z klawiatury                  | S-03          | FR-009, FR-010, US-01             | proposed |
| S-05  | reviewed-write-back                | przegląda kolejkę zmian, zatwierdza wybrane i wysyła do ClickUp     | S-04, F-03    | FR-014, US-01                     | proposed |
| S-06  | keyboard-task-editing              | zmienia status oraz edytuje tytuł/opis zadania z klawiatury         | S-05          | FR-012, FR-013, FR-014            | proposed |
| S-07  | sync-management-panel              | widzi status sync, wyzwala i ustawia częstotliwość per zestaw       | S-02, F-03    | FR-015..FR-019, US-02             | proposed |
| S-08  | milestone-release-note             | przegląda listę rozwiązanych zadań milestone'a (nice-to-have)       | S-04, S-06    | FR-011                            | proposed |
| S-09  | remember-last-context              | wraca do ostatniego kontekstu przy starcie (nice-to-have)          | S-03          | FR-006                            | proposed |

## Streams

Pomoc nawigacyjna — grupuje elementy dzielące łańcuch zależności. Kanoniczna
kolejność wciąż żyje w grafie zależności poniżej; ta tabela to proponowana
kolejność czytania równoległych torów.

| Stream | Theme                       | Chain                                          | Note                                                                          |
| ------ | --------------------------- | ---------------------------------------------- | ----------------------------------------------------------------------------- |
| A      | Rdzeń sync                  | `F-01` + `F-02` → `S-01` → `S-02`              | Client-agnostyczny; może ruszyć od razu, równolegle do decyzji o kliencie.    |
| B      | Doświadczenie klawiaturowe  | `F-03` → `S-03` → `S-04` → `S-05` → `S-06`     | Cała połowa UI; czeka na OQ-1 (który klient). Zawiera gwiazdę `S-04`.          |
| C      | Panel sync                  | `S-07`                                          | Dołącza do Stream A przy `S-02` i do Stream B przy `F-03`.                     |
| D      | Dodatki (nice-to-have)      | `S-08` / `S-09`                                | Parkowane za must-have; `S-08` dołącza przy `S-04`/`S-06`, `S-09` przy `S-03`. |

## Baseline

Co jest już w kodzie na dzień `2026-06-20` (auto-zbadane + potwierdzone przez użytkownika).
Fundamenty poniżej zakładają obecność tych rzeczy i ich nie re-scaffolderują.

- **Frontend:** partial — trzy szkielety klientów w `clients/` (web: Vite + React 19 + TS; flutter; desktop-java: JavaFX). Żaden bez kodu aplikacji; **wybór klienta odłożony**.
- **Backend / API:** partial — Spring Boot 4.1.0 (web-mvc) w `server/`, tylko klasa `ClickupSimplifierApplication` + test. Zero kontrolerów, domeny, integracji ClickUp.
- **Data:** absent — brak sterownika DB, ORM i migracji w `server/pom.xml`. Lokalna kopia nie ma warstwy trwałości.
- **Auth:** absent (zgodnie z projektem) — single-user, brak logowania; jedyny sekret to token API ClickUp, jego bezpieczne przechowywanie to sprawa downstream.
- **Deploy / infra:** partial — self-host, CI na GitHub Actions (per `tech-stack.md`), skrypty buildów Javy w `scripts/`. Brak zdalnego targetu (aplikacja lokalna).
- **Observability:** pominięte świadomie — aplikacja lokalna, jednoosobowa; widoczność błędów daje panel sync (FR-016/FR-017) + logi.

## Foundations

### F-01: Konfiguracja tokenu + łączność z API ClickUp

- **Outcome:** (fundament) aplikacja przechowuje osobisty token API ClickUp i potrafi wykonać uwierzytelnione wywołanie do API ClickUp.
- **Change ID:** clickup-token-and-connectivity
- **PRD refs:** FR-001, Access Control
- **Unlocks:** S-01 (pełny pull workspace) — bez uwierzytelnionej łączności nic nie da się pobrać.
- **Prerequisites:** —
- **Parallel with:** F-02, F-03
- **Blockers:** —
- **Unknowns:** Bezpieczne przechowywanie tokenu on-device — Owner: downstream. Block: no.
- **Risk:** Najmniejszy enabler dla każdej synchronizacji; ryzyko po stronie limitów/formatu API ClickUp. Sekwencjonowany pierwszy, bo bez niego rdzeń sync jest pusty.
- **Status:** ready

### F-02: Minimalna lokalna warstwa trwałości

- **Outcome:** (fundament) istnieje minimalny lokalny magazyn na kopię workspace (słowniki, milestone'y, zadania) odwzorowujący dwupoziomowy model milestone→task — tylko encje potrzebne pierwszemu pullowi.
- **Change ID:** local-copy-persistence
- **PRD refs:** FR-008, NFR (nawigacja ~100 ms), NFR (brak utraty lokalnej zmiany)
- **Unlocks:** S-01 (pull zapisuje do magazynu), S-03 (odczyt do nawigacji); redukuje niewiadomą „jak trzymana jest lokalna kopia".
- **Prerequisites:** —
- **Parallel with:** F-01, F-03
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Inwestycja „głęboko" — warstwa danych to serce produktu i bramka NFR (~100 ms na całym workspace). Trzymana minimalnie: nie budujemy całego schematu z góry, tylko kontrakt pod pierwszy pull; S-01 od razu go przez realny pull ćwiczy.
- **Status:** ready

### F-03: Wpięcie pierwszego klienta (JavaFX / desktop-java)

- **Outcome:** (fundament) klient JavaFX (`clients/desktop-java`) jest wpięty w rdzeń: działa kontrakt rdzeń↔klient (Java↔Java, kandydat na wywołanie in-process) i podstawowa, spójna nawigacja klawiaturą w pustej powłoce.
- **Change ID:** first-client-shell
- **PRD refs:** FR-007, US-01
- **Unlocks:** S-03, S-04 (gwiazda), S-05, S-06, S-07, S-08, S-09 — cała połowa UI.
- **Prerequisites:** —
- **Parallel with:** F-01, F-02
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Odblokowuje całą połowę UI łącznie z gwiazdą. Klient rozstrzygnięty (OQ-1): JavaFX pierwszy, potem Flutter, potem web. Szkielet już istnieje, więc to wpięcie + kontrakt, nie scaffolding od zera; ten sam język co rdzeń obniża koszt integracji.
- **Status:** ready

## Slices

### S-01: Pełny pull workspace do lokalnej kopii

- **Outcome:** użytkownik konfiguruje token i pobiera całą przestrzeń ClickUp do lokalnej kopii, zorganizowanej w dwa nazwane zestawy sync („Podstawowe słowniki" i „Zadania"); może zweryfikować, że dane są na miejscu.
- **Change ID:** full-workspace-pull
- **PRD refs:** FR-002, FR-003
- **Prerequisites:** F-01, F-02
- **Parallel with:** F-03
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Najryzykowniejsza integracja (zewnętrzne API, rozmiar całego workspace); sekwencjonowana wcześnie, bo cała reszta konsumuje lokalną kopię. Pełny pull jednorazowy, potem przyrost (S-02).
- **Implementation note (z review F-02):** Warstwa serwisowa sync MUSI owijać cały import workspace w `@Transactional`. Repozytoria F-02 mają `@Modifying` bez `@Transactional` (każdy upsert = osobna transakcja) — przy crash w połowie pull'a DB będzie w stanie częściowym. @Transactional należy do warstwy serwisowej, nie do repozytorium.
- **Implementation note (z review F-02):** FK w schemacie V1 nie mają `ON DELETE CASCADE`. Przy "replace all" sync usunięcie `space` bez kolejności leaf→root (task→list→folder→space) spowoduje FK violation. Plan S-01 musi zaplanować kolejność delecji lub dodać Flyway V2 z `CASCADE`.
- **Status:** proposed

### S-02: Synchronizacja przyrostowa + ręczne wyzwalanie

- **Outcome:** użytkownik utrzymuje lokalną kopię aktualną — zestaw „Zadania" dociąga zmiany przyrostowo, a użytkownik może wyzwolić dowolny zestaw na żądanie.
- **Change ID:** incremental-sync-and-manual-trigger
- **PRD refs:** FR-004 (oraz przyrostowa kadencja FR-003)
- **Prerequisites:** S-01
- **Parallel with:** F-03, S-03
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Utrzymuje świeżość kopii bez powtarzania pełnego pulla; ryzyko: bezpieczne scalanie przyrostu bez gubienia lokalnych edycji (guardrail). Client-agnostyczne — idzie równolegle do prac nad klientem.
- **Status:** proposed

### S-03: Nawigacja klawiaturą po milestone→task w wybranym kontekście

- **Outcome:** użytkownik wybiera kontekst (folder itp.) i porusza się wyłącznie klawiaturą po ściśle zagnieżdżonej strukturze milestone→task lokalnej kopii.
- **Change ID:** keyboard-milestone-task-nav
- **PRD refs:** FR-005, FR-007, FR-008, US-01
- **Prerequisites:** S-01, F-03
- **Parallel with:** S-02
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Pierwszy realny szybki, lekki widok; ryzyko: spójność klawiatury (NFR, rdzeń obietnicy produktu) — egzekwowana jako kryterium akceptacji, nie osobny fundament. Model widoku rozstrzygnięty (OQ-2): filtr zakończony na milestone → zadania tego milestone; zakończony na poziomie tasków kontekstu → wszystkie zadania kontekstu (w tym nieprzypisane). Czeka na klienta (F-03).
- **Status:** proposed

### S-04: Utworzenie milestone + przypisanie zadań z klawiatury

- **Outcome:** użytkownik tworzy milestone i przypisuje do niego zadania, w całości z klawiatury; zadania pokazują się ściśle zagnieżdżone pod swoim milestone.
- **Change ID:** create-milestone-and-assign
- **PRD refs:** FR-009, FR-010, US-01
- **Prerequisites:** S-03
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Gwiazda przewodnia — dowód centralnej reguły domenowej. Prezentacja nieprzypisanych rozstrzygnięta w OQ-2. Na tym etapie zmiany są lokalne; round-trip do ClickUp domyka S-05 (pełna akceptacja US-01).
- **Status:** proposed

### S-05: Bezpieczny zapis zwrotny do ClickUp z review

- **Outcome:** edycje gromadzą się lokalnie jako kolejka „oczekujących"; użytkownik otwiera panel review, widzi listę zmian (typ + obiekt + przed→po), zatwierdza wszystkie lub zaznaczony podzbiór (pomijając np. konflikt), po czym wybrane trafiają do ClickUp bez duplikatów i bez cichego auto-push.
- **Change ID:** reviewed-write-back
- **PRD refs:** FR-014, US-01, FR-009, FR-010
- **Prerequisites:** S-04, F-03
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Najryzykowniejsza część (dwukierunkowy zapis); guardrail: brak cichego nadpisania, bezpieczne konflikty. Mechanizm rozstrzygnięty (OQ-3): kolejka oczekujących + selektywny przegląd; konflikty wychodzą w panelu i można pominąć sporną zmianę, wysyłając czyste. Ustanawia kanał review, na którym jedzie S-06.
- **Status:** proposed

### S-06: Edycja zadań z klawiatury przez zapis zwrotny

- **Outcome:** użytkownik zmienia status zadania oraz edytuje tytuł/opis z klawiatury, a zmiany jadą do ClickUp przez ustanowiony mechanizm review.
- **Change ID:** keyboard-task-editing
- **PRD refs:** FR-012, FR-013, FR-014
- **Prerequisites:** S-05
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Najczęstsza codzienna operacja (status); jedzie bezpiecznym kanałem z S-05, więc ryzyko zapisu już rozbrojone. Powierzchnia edycji ma rosnąć w późniejszych wersjach (PRD).
- **Status:** proposed

### S-07: Panel zarządzania synchronizacją

- **Outcome:** użytkownik widzi per zestaw sync czas ostatniego sukcesu i ostatniego błędu (z opisem błędu), wyzwala dowolny zestaw natychmiast z panelu i ustawia jego automatyczną częstotliwość (preset lub własna wartość).
- **Change ID:** sync-management-panel
- **PRD refs:** FR-015, FR-016, FR-017, FR-018, FR-019, US-02
- **Prerequisites:** S-02, F-03
- **Parallel with:** S-03, S-04
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Powierzchnia zaufania do danych (świeżość + błędy). Presety i domyślne rozstrzygnięte (OQ-4): pełna lista + własna wartość; Zadania 5 min, Słowniki 24 h. Spójny, samodzielny ekran; czeka na klienta (F-03) i istniejący sync (S-02).
- **Status:** proposed

### S-08: Notatka wydania milestone'a (nice-to-have)

- **Outcome:** użytkownik przegląda listę rozwiązanych (ukończonych) zadań danego milestone'a lub zagregowaną w bieżącym kontekście.
- **Change ID:** milestone-release-note
- **PRD refs:** FR-011
- **Prerequisites:** S-04, S-06
- **Parallel with:** S-09
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Funkcja pochodna, składana z danych już posiadanych; nice-to-have, parkowana za ścieżką must-have (cel: niska złożoność).
- **Status:** proposed

### S-09: Zapamiętanie i przywrócenie kontekstu (nice-to-have)

- **Outcome:** aplikacja zapamiętuje ostatnio wybrany kontekst i przywraca go przy kolejnym uruchomieniu.
- **Change ID:** remember-last-context
- **PRD refs:** FR-006
- **Prerequisites:** S-03
- **Parallel with:** S-08
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Codzienna wygoda; nice-to-have, niskie ryzyko. Parkowana za must-have.
- **Status:** proposed

## Backlog Handoff

| Roadmap ID | Change ID                          | Suggested issue title                                | Ready for `/10x-plan` | Notes |
| ---------- | ---------------------------------- | --------------------------------------------------- | --------------------- | ----- |
| F-01       | clickup-token-and-connectivity     | Token ClickUp + uwierzytelniona łączność z API      | yes                   | Run `/10x-plan clickup-token-and-connectivity` |
| F-02       | local-copy-persistence             | Minimalna lokalna warstwa trwałości kopii workspace | yes                   | Run `/10x-plan local-copy-persistence` |
| F-03       | first-client-shell                 | Wpięcie klienta JavaFX (desktop-java) do rdzenia    | yes                   | OQ-1 rozstrzygnięte: JavaFX pierwszy. Run `/10x-plan first-client-shell` |
| S-01       | full-workspace-pull                | Pełny pull workspace do lokalnej kopii              | no                    | Czeka na F-01, F-02 |
| S-02       | incremental-sync-and-manual-trigger| Sync przyrostowy + ręczne wyzwalanie zestawu        | no                    | Czeka na S-01 |
| S-03       | keyboard-milestone-task-nav        | Nawigacja klawiaturą milestone→task w kontekście    | no                    | Czeka na S-01, F-03 |
| S-04       | create-milestone-and-assign        | Utworzenie milestone + przypisanie zadań (gwiazda)  | no                    | Czeka na S-03 |
| S-05       | reviewed-write-back                | Zapis zwrotny: kolejka oczekujących + selektywny review | no                | OQ-3 rozstrzygnięte; czeka jeszcze na S-04, F-03 |
| S-06       | keyboard-task-editing              | Edycja statusu/tytułu/opisu zadania z klawiatury    | no                    | Czeka na S-05 |
| S-07       | sync-management-panel              | Panel zarządzania synchronizacją                    | no                    | OQ-4 rozstrzygnięte; czeka na S-02, F-03 |
| S-08       | milestone-release-note             | Notatka wydania milestone'a                         | no                    | nice-to-have; czeka na S-04, S-06 |
| S-09       | remember-last-context              | Zapamiętanie ostatniego kontekstu                   | no                    | nice-to-have; czeka na S-03 |

## Open Roadmap Questions

1. ✅ **ROZSTRZYGNIĘTE (2026-06-20): Który klient idzie jako pierwszy?** → **JavaFX (desktop-java) pierwszy, potem Flutter, potem web.** Odblokowało F-03 (oraz przez nie ścieżkę UI: S-03..S-09).
2. ✅ **ROZSTRZYGNIĘTE (2026-06-20): Zadania nieprzypisane do milestone?** → Brak osobnego węzła „bez milestone". Prezentacja zależy od zakończenia filtra nawigacji (context-first): filtr zakończony na **milestone** → tylko zadania tego milestone; filtr zakończony na **poziomie tasków kontekstu** (bez wybranego milestone) → wszystkie zadania kontekstu razem (przypisane + nieprzypisane), więc nieprzypisane są tam widoczne. Żaden widok nie miesza milestone'ów i zadań (FR-008 OK). Dotyczyło S-03, S-04.
3. ✅ **ROZSTRZYGNIĘTE (2026-06-20): Mechanizm review zapisu zwrotnego?** → **Kolejka oczekujących zmian + selektywny przegląd** (model „staging": lokalne pending, panel z listą zmian, zatwierdź wszystko lub podzbiór, jeden push; konflikty pomijalne). Odblokowało S-05.
4. ✅ **ROZSTRZYGNIĘTE (2026-06-20): Presety częstotliwości auto-sync?** → Wspólna pełna lista presetów: Ręcznie · 5 min · 15 min · 30 min · 1 h · 4 h · 12 h · 24 h, plus własna wartość (FR-019). Domyślne per zestaw: **Zadania → 5 min** (NFR „świeżość w ciągu minut"), **Podstawowe słowniki → 24 h**. Dotyczyło S-07.
5. ✅ **ROZSTRZYGNIĘTE (2026-06-20): Czy zarządzanie sync potrzebuje własnej historyjki?** → Tak. Dopisano **US-02** (zarządzanie synchronizacją) do `prd.md` z Given/When/Then + kryteriami akceptacji (FR-015..FR-019); podpięta w PRD refs S-07.

## Parked

- **Multi-user, współdzielenie, role** — Why parked: PRD §Non-Goals; narzędzie jest jednoosobowe (może urosnąć później).
- **Odtwarzanie bogactwa funkcji ClickUp** (komentarze, załączniki, dashboardy, automatyzacje, Gantt, custom views) — Why parked: PRD §Non-Goals; wartością jest lekki, przewidywalny podzbiór, nie parytet.
- **Zarządzanie strukturą słowników z klienta** (tworzenie/edycja spaces/folders/lists) — Why parked: PRD §Non-Goals; słowniki są synchronizowane tylko do odczytu.
- **Własne zestawy sync użytkownika** — Why parked: PRD §Non-Goals; katalog zestawów jest stały dla MVP (dwa nazwane).
- **Tryb offline** — Why parked: PRD §Non-Goals; MVP wymaga żywego połączenia z ClickUp.
- **OAuth** — Why parked: PRD §Non-Goals; dla MVP tylko osobisty token API.

## Done

(Pusta przy pierwszej generacji. `/10x-archive` dopisuje tu wpis — i przełącza `Status` elementu na `done` — gdy zmiana o pasującym `Change ID` jest archiwizowana.)
