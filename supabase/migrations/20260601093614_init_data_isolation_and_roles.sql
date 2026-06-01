-- F-01: Izolacja danych i role dostępu
-- Tworzy minimalny schemat domenowy LicenceQuizz z izolacją per-user (RLS)
-- i flagą administratora czytaną z bazy (brak service_role; admin musi żyć w DB).
--
-- Wzorce bezpieczeństwa:
--   * każda polityka ma TO authenticated + predykat własności (nie sam TO)
--   * auth.uid() owinięte w (select auth.uid()) -> cache per-statement
--   * UPDATE ma USING i WITH CHECK; UPDATE wymaga też polityki SELECT
--   * helper admina jako SECURITY DEFINER w NIEEKSPONOWANYM schemacie `private`
--     (funkcje SECURITY DEFINER w public są wywoływalne przez wszystkie role)

-- ============================================================
-- Schemat pomocniczy (nieeksponowany w Data API)
-- ============================================================
create schema if not exists private;

-- ============================================================
-- Tabele
-- ============================================================

-- Profil konta: nośnik flagi administratora (1:1 z auth.users)
create table public.profiles (
  user_id    uuid primary key references auth.users (id) on delete cascade,
  is_admin   boolean not null default false,
  created_at timestamptz not null default now()
);

-- Kategorie pytań (współdzielone; pisze tylko admin)
create table public.categories (
  id         uuid primary key default gen_random_uuid(),
  name       text not null unique,
  created_at timestamptz not null default now()
);

-- Pytania (współdzielone; pisze tylko admin). video_ref nullable i nieużywany w F-01.
create table public.questions (
  id          uuid primary key default gen_random_uuid(),
  category_id uuid not null references public.categories (id),
  prompt      text not null,
  video_ref   text,
  created_at  timestamptz not null default now()
);

-- Warianty odpowiedzi (współdzielone; pisze tylko admin)
create table public.answer_options (
  id          uuid primary key default gen_random_uuid(),
  question_id uuid not null references public.questions (id) on delete cascade,
  position    smallint not null,
  text        text not null,
  is_correct  boolean not null default false
);

-- Tagi trudności nadawane przez użytkownika (prywatne per user)
create table public.difficulty_tags (
  user_id     uuid not null references auth.users (id) on delete cascade,
  question_id uuid not null references public.questions (id) on delete cascade,
  difficulty  smallint not null check (difficulty between 1 and 3),
  updated_at  timestamptz not null default now(),
  primary key (user_id, question_id)
);

-- Sesje quizowe (prywatne per user)
create table public.sessions (
  id           uuid primary key default gen_random_uuid(),
  user_id      uuid not null references auth.users (id) on delete cascade,
  config       jsonb not null,
  status       text not null default 'in_progress' check (status in ('in_progress', 'completed')),
  created_at   timestamptz not null default now(),
  completed_at timestamptz
);

-- Kolejka pytań + udzielone odpowiedzi w sesji (prywatne przez właściciela sesji).
-- Bez is_correct: poprawność liczona przez join do answer_options (decyzja Q6).
create table public.session_questions (
  id                 uuid primary key default gen_random_uuid(),
  session_id         uuid not null references public.sessions (id) on delete cascade,
  question_id        uuid not null references public.questions (id),
  position           smallint not null,
  selected_option_id uuid references public.answer_options (id),
  answered_at        timestamptz
);

-- ============================================================
-- Indeksy (FK / kolumny używane w politykach RLS)
-- ============================================================
create index idx_questions_category on public.questions (category_id);
create index idx_answer_options_question on public.answer_options (question_id);
create index idx_difficulty_tags_user on public.difficulty_tags (user_id);
create index idx_sessions_user on public.sessions (user_id);
create index idx_session_questions_session on public.session_questions (session_id);

-- ============================================================
-- Helper admina: SECURITY DEFINER w schemacie private
-- ============================================================
-- Czyta wyłącznie flagę dla bieżącego auth.uid(); brak parametrów -> brak
-- powierzchni do wycieku cudzych danych. SECURITY DEFINER, by odczyt profiles
-- wewnątrz polityk nie wyzwalał RLS na profiles (brak rekurencji).
create function private.is_admin()
returns boolean
language sql
security definer
stable
set search_path = ''
as $$
  select coalesce(
    (select p.is_admin from public.profiles p where p.user_id = (select auth.uid())),
    false
  );
$$;

-- Domyślnie EXECUTE jest grantowane PUBLIC; cofamy i nadajemy tylko authenticated.
revoke execute on function private.is_admin() from public;
grant usage on schema private to authenticated;
grant execute on function private.is_admin() to authenticated;

-- ============================================================
-- Trigger: nowy auth.users -> wiersz profiles
-- ============================================================
create function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (user_id) values (new.id)
  on conflict (user_id) do nothing;
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ============================================================
-- RLS: włączenie na każdej tabeli
-- ============================================================
alter table public.profiles enable row level security;
alter table public.categories enable row level security;
alter table public.questions enable row level security;
alter table public.answer_options enable row level security;
alter table public.difficulty_tags enable row level security;
alter table public.sessions enable row level security;
alter table public.session_questions enable row level security;

-- ============================================================
-- Polityki: profiles (tylko własny odczyt; zapis przez trigger / dev SQL)
-- ============================================================
create policy "profiles_select_own" on public.profiles
  for select to authenticated
  using ((select auth.uid()) = user_id);

-- ============================================================
-- Polityki: categories (odczyt dla zalogowanych; zapis tylko admin)
-- ============================================================
create policy "categories_select_authenticated" on public.categories
  for select to authenticated
  using (true);

create policy "categories_insert_admin" on public.categories
  for insert to authenticated
  with check ((select private.is_admin()));

create policy "categories_update_admin" on public.categories
  for update to authenticated
  using ((select private.is_admin()))
  with check ((select private.is_admin()));

create policy "categories_delete_admin" on public.categories
  for delete to authenticated
  using ((select private.is_admin()));

-- ============================================================
-- Polityki: questions (odczyt dla zalogowanych; zapis tylko admin)
-- ============================================================
create policy "questions_select_authenticated" on public.questions
  for select to authenticated
  using (true);

create policy "questions_insert_admin" on public.questions
  for insert to authenticated
  with check ((select private.is_admin()));

create policy "questions_update_admin" on public.questions
  for update to authenticated
  using ((select private.is_admin()))
  with check ((select private.is_admin()));

create policy "questions_delete_admin" on public.questions
  for delete to authenticated
  using ((select private.is_admin()));

-- ============================================================
-- Polityki: answer_options (odczyt dla zalogowanych; zapis tylko admin)
-- ============================================================
create policy "answer_options_select_authenticated" on public.answer_options
  for select to authenticated
  using (true);

create policy "answer_options_insert_admin" on public.answer_options
  for insert to authenticated
  with check ((select private.is_admin()));

create policy "answer_options_update_admin" on public.answer_options
  for update to authenticated
  using ((select private.is_admin()))
  with check ((select private.is_admin()));

create policy "answer_options_delete_admin" on public.answer_options
  for delete to authenticated
  using ((select private.is_admin()));

-- ============================================================
-- Polityki: difficulty_tags (prywatne per user)
-- ============================================================
create policy "difficulty_tags_select_own" on public.difficulty_tags
  for select to authenticated
  using ((select auth.uid()) = user_id);

create policy "difficulty_tags_insert_own" on public.difficulty_tags
  for insert to authenticated
  with check ((select auth.uid()) = user_id);

create policy "difficulty_tags_update_own" on public.difficulty_tags
  for update to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

create policy "difficulty_tags_delete_own" on public.difficulty_tags
  for delete to authenticated
  using ((select auth.uid()) = user_id);

-- ============================================================
-- Polityki: sessions (prywatne per user)
-- ============================================================
create policy "sessions_select_own" on public.sessions
  for select to authenticated
  using ((select auth.uid()) = user_id);

create policy "sessions_insert_own" on public.sessions
  for insert to authenticated
  with check ((select auth.uid()) = user_id);

create policy "sessions_update_own" on public.sessions
  for update to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

create policy "sessions_delete_own" on public.sessions
  for delete to authenticated
  using ((select auth.uid()) = user_id);

-- ============================================================
-- Polityki: session_questions (własność pośrednia przez sesję)
-- ============================================================
create policy "session_questions_select_own" on public.session_questions
  for select to authenticated
  using (exists (
    select 1 from public.sessions s
    where s.id = session_id and s.user_id = (select auth.uid())
  ));

create policy "session_questions_insert_own" on public.session_questions
  for insert to authenticated
  with check (exists (
    select 1 from public.sessions s
    where s.id = session_id and s.user_id = (select auth.uid())
  ));

create policy "session_questions_update_own" on public.session_questions
  for update to authenticated
  using (exists (
    select 1 from public.sessions s
    where s.id = session_id and s.user_id = (select auth.uid())
  ))
  with check (exists (
    select 1 from public.sessions s
    where s.id = session_id and s.user_id = (select auth.uid())
  ));

create policy "session_questions_delete_own" on public.session_questions
  for delete to authenticated
  using (exists (
    select 1 from public.sessions s
    where s.id = session_id and s.user_id = (select auth.uid())
  ));
