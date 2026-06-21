-- F-02 / Faza 2: lokalna kopia workspace ClickUp (Space -> Folder -> List -> Task).
-- Klucze to natywne id ClickUp (text). Milestone modelowany na encji Task:
-- is_milestone (flaga) + milestone_id (self-ref na task-milestone; NULL = "no milestone").

CREATE TABLE space (
    id   text PRIMARY KEY,
    name text NOT NULL
);

CREATE TABLE folder (
    id       text PRIMARY KEY,
    space_id text NOT NULL REFERENCES space (id),
    name     text NOT NULL
);

-- Listy mogą wisieć bezpośrednio pod Space ("folderless") -> folder_id nullowalne,
-- ale space_id zawsze NOT NULL (lista zawsze należy do Space).
CREATE TABLE list (
    id        text PRIMARY KEY,
    name      text NOT NULL,
    space_id  text NOT NULL REFERENCES space (id),
    folder_id text NULL REFERENCES folder (id)
);

CREATE TABLE task (
    id           text PRIMARY KEY,
    list_id      text NOT NULL REFERENCES list (id),
    name         text NOT NULL,
    status       text NULL,
    description  text NULL,
    is_milestone boolean NOT NULL DEFAULT false,
    milestone_id text NULL REFERENCES task (id)
);

-- Indeksy na kolumnach FK pod ścieżki nawigacji (NFR ~100 ms na całym workspace).
CREATE INDEX idx_folder_space_id ON folder (space_id);
CREATE INDEX idx_list_space_id ON list (space_id);
CREATE INDEX idx_list_folder_id ON list (folder_id);
CREATE INDEX idx_task_list_id ON task (list_id);
CREATE INDEX idx_task_milestone_id ON task (milestone_id);
