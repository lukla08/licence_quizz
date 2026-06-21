-- S-01 / Phase 1: subtaski (parent_id na task) + CASCADE FK + tabela stanu sync.
--
-- Przebudowujemy istniejące FK bez CASCADE na FK z CASCADE/SET NULL,
-- żeby syncDictionaries mogło usuwać stale space/folder/list bez ręcznej
-- kolejności delete (delete space → CASCADE do folder → list → task).

-- 1. parent_id na task (nowa kolumna dla subtasków ClickUp)
ALTER TABLE task ADD COLUMN parent_id text NULL;
ALTER TABLE task
    ADD CONSTRAINT task_parent_id_fkey FOREIGN KEY (parent_id)
        REFERENCES task (id) ON DELETE SET NULL;
CREATE INDEX idx_task_parent_id ON task (parent_id);

-- 2. task.list_id: NOT NULL FK bez CASCADE → CASCADE
ALTER TABLE task DROP CONSTRAINT task_list_id_fkey;
ALTER TABLE task
    ADD CONSTRAINT task_list_id_fkey FOREIGN KEY (list_id)
        REFERENCES list (id) ON DELETE CASCADE;

-- 3. task.milestone_id: FK bez CASCADE → SET NULL
ALTER TABLE task DROP CONSTRAINT task_milestone_id_fkey;
ALTER TABLE task
    ADD CONSTRAINT task_milestone_id_fkey FOREIGN KEY (milestone_id)
        REFERENCES task (id) ON DELETE SET NULL;

-- 4. list.folder_id: FK bez CASCADE → CASCADE
ALTER TABLE list DROP CONSTRAINT list_folder_id_fkey;
ALTER TABLE list
    ADD CONSTRAINT list_folder_id_fkey FOREIGN KEY (folder_id)
        REFERENCES folder (id) ON DELETE CASCADE;

-- 5. list.space_id: FK bez CASCADE → CASCADE
ALTER TABLE list DROP CONSTRAINT list_space_id_fkey;
ALTER TABLE list
    ADD CONSTRAINT list_space_id_fkey FOREIGN KEY (space_id)
        REFERENCES space (id) ON DELETE CASCADE;

-- 6. folder.space_id: FK bez CASCADE → CASCADE
ALTER TABLE folder DROP CONSTRAINT folder_space_id_fkey;
ALTER TABLE folder
    ADD CONSTRAINT folder_space_id_fkey FOREIGN KEY (space_id)
        REFERENCES space (id) ON DELETE CASCADE;

-- 7. Tabela stanu sync: per-zestaw timestamp ostatniego sukcesu
CREATE TABLE sync_set (
    name           text PRIMARY KEY,
    last_synced_at timestamptz NULL
);

INSERT INTO sync_set (name) VALUES ('dictionaries'), ('tasks');
