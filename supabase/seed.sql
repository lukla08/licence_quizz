-- Seed F-01: 3 kategorie + 12 pytań tekstowych + warianty odpowiedzi.
-- video_ref pozostaje NULL (decyzja o źródle wideo odłożona do S-02/S-04).
-- Idempotentny: stałe UUID-y + ON CONFLICT / WHERE NOT EXISTS, więc bezpieczny
-- przy ponownym uruchomieniu na tej samej bazie (db reset i tak odtwarza od zera).

-- ============================================================
-- Kategorie
-- ============================================================
insert into public.categories (id, name) values
  ('c0000000-0000-0000-0000-000000000001', 'Przepisy ruchu drogowego'),
  ('c0000000-0000-0000-0000-000000000002', 'Znaki drogowe'),
  ('c0000000-0000-0000-0000-000000000003', 'Pierwsza pomoc i technika jazdy')
on conflict (id) do nothing;

-- ============================================================
-- Pytania (po 4 na kategorię)
-- ============================================================
insert into public.questions (id, category_id, prompt) values
  -- Przepisy ruchu drogowego
  ('a0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'Na skrzyżowaniu równorzędnym pierwszeństwo ma pojazd nadjeżdżający:'),
  ('a0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', 'Dozwolona prędkość samochodu osobowego w terenie zabudowanym wynosi:'),
  ('a0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000001', 'Podwójna ciągła linia na środku jezdni oznacza, że:'),
  ('a0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000001', 'Światła mijania w pojeździe należy włączać:'),
  -- Znaki drogowe
  ('a0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000002', 'Znak B-20 „STOP" nakazuje kierującemu:'),
  ('a0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000002', 'Trójkątny znak z czerwoną obwódką i wykrzyknikiem to znak:'),
  ('a0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000002', 'Okrągłe znaki na niebieskim tle to znaki:'),
  ('a0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000002', 'Okrągłe znaki z czerwoną obwódką to przede wszystkim znaki:'),
  -- Pierwsza pomoc i technika jazdy
  ('a0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000003', 'U osoby nieprzytomnej w pierwszej kolejności sprawdzasz:'),
  ('a0000000-0000-0000-0000-00000000000a', 'c0000000-0000-0000-0000-000000000003', 'Numer alarmowy działający w całej Unii Europejskiej to:'),
  ('a0000000-0000-0000-0000-00000000000b', 'c0000000-0000-0000-0000-000000000003', 'Prawidłowa częstotliwość uciśnięć klatki piersiowej podczas RKO to:'),
  ('a0000000-0000-0000-0000-00000000000c', 'c0000000-0000-0000-0000-000000000003', 'W razie wpadnięcia pojazdu w poślizg należy przede wszystkim:')
on conflict (id) do nothing;

-- ============================================================
-- Warianty odpowiedzi (dokładnie jeden poprawny na pytanie)
-- Idempotencja kluczowana na (question_id, position).
-- ============================================================
insert into public.answer_options (question_id, position, text, is_correct)
select v.qid, v.pos, v.txt, v.correct
from (values
  -- Q1: pierwszeństwo na równorzędnym
  ('a0000000-0000-0000-0000-000000000001'::uuid, 1, 'z lewej strony', false),
  ('a0000000-0000-0000-0000-000000000001'::uuid, 2, 'z prawej strony', true),
  ('a0000000-0000-0000-0000-000000000001'::uuid, 3, 'jadący na wprost', false),
  -- Q2: prędkość w terenie zabudowanym
  ('a0000000-0000-0000-0000-000000000002'::uuid, 1, '40 km/h', false),
  ('a0000000-0000-0000-0000-000000000002'::uuid, 2, '50 km/h', true),
  ('a0000000-0000-0000-0000-000000000002'::uuid, 3, '60 km/h', false),
  ('a0000000-0000-0000-0000-000000000002'::uuid, 4, '70 km/h', false),
  -- Q3: podwójna ciągła
  ('a0000000-0000-0000-0000-000000000003'::uuid, 1, 'można ją przejeżdżać przy wyprzedzaniu', false),
  ('a0000000-0000-0000-0000-000000000003'::uuid, 2, 'obowiązuje tylko nocą', false),
  ('a0000000-0000-0000-0000-000000000003'::uuid, 3, 'nie wolno na nią najeżdżać ani jej przekraczać', true),
  -- Q4: światła mijania
  ('a0000000-0000-0000-0000-000000000004'::uuid, 1, 'tylko po zmroku', false),
  ('a0000000-0000-0000-0000-000000000004'::uuid, 2, 'tylko podczas opadów', false),
  ('a0000000-0000-0000-0000-000000000004'::uuid, 3, 'przez całą dobę', true),
  -- Q5: znak STOP
  ('a0000000-0000-0000-0000-000000000005'::uuid, 1, 'zwolnić i ustąpić pierwszeństwa bez zatrzymania', false),
  ('a0000000-0000-0000-0000-000000000005'::uuid, 2, 'bezwzględnie zatrzymać pojazd i ustąpić pierwszeństwa', true),
  ('a0000000-0000-0000-0000-000000000005'::uuid, 3, 'zatrzymać się tylko, gdy nadjeżdża inny pojazd', false),
  -- Q6: znak ostrzegawczy
  ('a0000000-0000-0000-0000-000000000006'::uuid, 1, 'ostrzegawczy', true),
  ('a0000000-0000-0000-0000-000000000006'::uuid, 2, 'zakazu', false),
  ('a0000000-0000-0000-0000-000000000006'::uuid, 3, 'informacyjny', false),
  -- Q7: znaki niebieskie okrągłe
  ('a0000000-0000-0000-0000-000000000007'::uuid, 1, 'zakazu', false),
  ('a0000000-0000-0000-0000-000000000007'::uuid, 2, 'nakazu', true),
  ('a0000000-0000-0000-0000-000000000007'::uuid, 3, 'ostrzegawcze', false),
  -- Q8: znaki z czerwoną obwódką
  ('a0000000-0000-0000-0000-000000000008'::uuid, 1, 'nakazu', false),
  ('a0000000-0000-0000-0000-000000000008'::uuid, 2, 'informacyjne', false),
  ('a0000000-0000-0000-0000-000000000008'::uuid, 3, 'zakazu', true),
  -- Q9: nieprzytomny — co najpierw
  ('a0000000-0000-0000-0000-000000000009'::uuid, 1, 'oddech i drożność dróg oddechowych', true),
  ('a0000000-0000-0000-0000-000000000009'::uuid, 2, 'dokumenty poszkodowanego', false),
  ('a0000000-0000-0000-0000-000000000009'::uuid, 3, 'temperaturę ciała', false),
  -- Q10: numer alarmowy
  ('a0000000-0000-0000-0000-00000000000a'::uuid, 1, '997', false),
  ('a0000000-0000-0000-0000-00000000000a'::uuid, 2, '112', true),
  ('a0000000-0000-0000-0000-00000000000a'::uuid, 3, '999', false),
  -- Q11: częstotliwość RKO
  ('a0000000-0000-0000-0000-00000000000b'::uuid, 1, '60–80 na minutę', false),
  ('a0000000-0000-0000-0000-00000000000b'::uuid, 2, '100–120 na minutę', true),
  ('a0000000-0000-0000-0000-00000000000b'::uuid, 3, '140–160 na minutę', false),
  -- Q12: poślizg
  ('a0000000-0000-0000-0000-00000000000c'::uuid, 1, 'gwałtownie hamować', false),
  ('a0000000-0000-0000-0000-00000000000c'::uuid, 2, 'zredukować gaz i kontrować kierownicą', true),
  ('a0000000-0000-0000-0000-00000000000c'::uuid, 3, 'wcisnąć sprzęgło i puścić kierownicę', false)
) as v(qid, pos, txt, correct)
where not exists (
  select 1 from public.answer_options ao
  where ao.question_id = v.qid and ao.position = v.pos
);
