-- Weryfikacja izolacji RLS dla F-01 (data-isolation-and-roles).
-- Uruchom na LOKALNEJ bazie (po `npx supabase start` + `db reset`):
--   psql "$(npx supabase status -o env | grep '^DB_URL' | cut -d= -f2- | tr -d '\"')" -f supabase/tests/rls_isolation.sql
-- Skrypt jest samozawierający: zakłada dane testowe, asercje, i ROLLBACK na końcu
-- (nie zostawia śladów). Każda asercja podnosi wyjątek przy niepowodzeniu.
--
-- Sprawdza:
--   * user A widzi wyłącznie własne tagi/sesje/odpowiedzi
--   * admin B NIE widzi prywatnych danych A (izolacja silniejsza niż rola)
--   * każdy zalogowany czyta dane współdzielone (questions/categories)
--   * nie-admin NIE może pisać do questions; admin może

begin;

-- ---- setup (superuser — omija RLS) ----
insert into auth.users (instance_id, id, aud, role, email, created_at, updated_at)
values
  ('00000000-0000-0000-0000-000000000000', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'authenticated', 'authenticated', 'user_a@test.local', now(), now()),
  ('00000000-0000-0000-0000-000000000000', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'authenticated', 'authenticated', 'admin_b@test.local', now(), now());

-- trigger utworzył wiersze profiles; promuj B na admina
update public.profiles set is_admin = true
where user_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb';

insert into public.categories (id, name)
values ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'Test');
insert into public.questions (id, category_id, prompt)
values ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 'Pytanie testowe?');

do $$
declare
  uid_a   uuid := 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
  uid_b   uuid := 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb';
  qid     uuid := 'dddddddd-dddd-dddd-dddd-dddddddddddd';
  cid     uuid := 'cccccccc-cccc-cccc-cccc-cccccccccccc';
  sess_id uuid := '11111111-1111-1111-1111-111111111111';
  n       int;
  blocked boolean;
begin
  -- ===== A (zwykły user) zapisuje dane prywatne =====
  execute 'set local role authenticated';
  perform set_config('request.jwt.claims', json_build_object('sub', uid_a::text, 'role', 'authenticated')::text, true);

  insert into public.difficulty_tags (user_id, question_id, difficulty) values (uid_a, qid, 2);
  insert into public.sessions (id, user_id, config) values (sess_id, uid_a, '{}'::jsonb);
  insert into public.session_questions (session_id, question_id, position) values (sess_id, qid, 1);

  select count(*) into n from public.difficulty_tags;
  assert n = 1, 'A powinien widzieć 1 własny tag, widzi ' || n;
  reset role;

  -- ===== B (admin) NIE może widzieć prywatnych danych A =====
  execute 'set local role authenticated';
  perform set_config('request.jwt.claims', json_build_object('sub', uid_b::text, 'role', 'authenticated')::text, true);

  select count(*) into n from public.difficulty_tags;
  assert n = 0, 'B nie może widzieć tagów A, widzi ' || n;
  select count(*) into n from public.sessions;
  assert n = 0, 'B nie może widzieć sesji A, widzi ' || n;
  select count(*) into n from public.session_questions;
  assert n = 0, 'B nie może widzieć odpowiedzi A, widzi ' || n;

  -- dane współdzielone widoczne dla zalogowanego
  select count(*) into n from public.questions;
  assert n >= 1, 'B musi czytać questions, widzi ' || n;
  select count(*) into n from public.categories;
  assert n >= 1, 'B musi czytać categories, widzi ' || n;
  reset role;

  -- ===== A (nie-admin) NIE może pisać do questions =====
  execute 'set local role authenticated';
  perform set_config('request.jwt.claims', json_build_object('sub', uid_a::text, 'role', 'authenticated')::text, true);
  blocked := false;
  begin
    insert into public.questions (category_id, prompt) values (cid, 'proba-nie-admina');
  exception when others then
    blocked := true;  -- oczekiwana odmowa RLS
  end;
  assert blocked, 'nie-admin NIE może wstawić questions';
  reset role;

  -- ===== B (admin) MOŻE pisać do questions =====
  execute 'set local role authenticated';
  perform set_config('request.jwt.claims', json_build_object('sub', uid_b::text, 'role', 'authenticated')::text, true);
  insert into public.questions (category_id, prompt) values (cid, 'dodane-przez-admina');
  select count(*) into n from public.questions where prompt = 'dodane-przez-admina';
  assert n = 1, 'wstawienie przez admina powinno się udać';
  reset role;

  raise notice 'RLS isolation: WSZYSTKIE ASERCJE PRZESZŁY';
end $$;

rollback;
