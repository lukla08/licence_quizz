package com.example.clickupsimplifier.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Testy integracyjne repozytoriów na efemerycznym Postgresie (Testcontainers).
 * Pokrywa: zapis-odczyt, idempotentny upsert, milestone null + self-ref, relacje FK,
 * zapytania nawigacyjne (F-02, Faza 3).
 */
@SpringBootTest
@Import(PostgresTestcontainersConfig.class)
class RepositoryIntegrationTest {

    @Autowired SpaceRepository spaces;
    @Autowired FolderRepository folders;
    @Autowired WorkspaceListRepository lists;
    @Autowired TaskRepository tasks;

    @BeforeEach
    void clean() {
        tasks.deleteAll();
        lists.deleteAll();
        folders.deleteAll();
        spaces.deleteAll();
    }

    // --- 3.2 Zapis → odczyt ---

    @Test
    void saveAndReadSpace() {
        spaces.upsert("s1", "My Space");
        assertThat(spaces.findById("s1")).hasValueSatisfying(s -> {
            assertThat(s.id()).isEqualTo("s1");
            assertThat(s.name()).isEqualTo("My Space");
        });
    }

    @Test
    void saveAndReadFolder() {
        spaces.upsert("s1", "Space");
        folders.upsert("f1", "s1", "My Folder");
        assertThat(folders.findById("f1")).hasValueSatisfying(f -> {
            assertThat(f.spaceId()).isEqualTo("s1");
            assertThat(f.name()).isEqualTo("My Folder");
        });
    }

    @Test
    void saveAndReadList() {
        spaces.upsert("s1", "Space");
        folders.upsert("f1", "s1", "Folder");
        lists.upsert("l1", "My List", "s1", "f1");
        assertThat(lists.findById("l1")).hasValueSatisfying(l -> {
            assertThat(l.spaceId()).isEqualTo("s1");
            assertThat(l.folderId()).isEqualTo("f1");
        });
    }

    @Test
    void saveAndReadTask() {
        seedHierarchy();
        tasks.upsert("t1", "l1", "My Task", "open", "desc", false, null);
        assertThat(tasks.findById("t1")).hasValueSatisfying(t -> {
            assertThat(t.listId()).isEqualTo("l1");
            assertThat(t.status()).isEqualTo("open");
            assertThat(t.isMilestone()).isFalse();
            assertThat(t.milestoneId()).isNull();
        });
    }

    // --- 3.3 Idempotencja upsertu ---

    @Test
    void upsertSpaceIsIdempotent() {
        spaces.upsert("s1", "Old Name");
        spaces.upsert("s1", "New Name");
        assertThat(spaces.count()).isEqualTo(1);
        assertThat(spaces.findById("s1")).hasValueSatisfying(s ->
                assertThat(s.name()).isEqualTo("New Name"));
    }

    @Test
    void upsertTaskIsIdempotent() {
        seedHierarchy();
        tasks.upsert("t1", "l1", "Task", "open", null, false, null);
        tasks.upsert("t1", "l1", "Task Updated", "done", "added desc", false, null);
        assertThat(tasks.count()).isEqualTo(1);
        assertThat(tasks.findById("t1")).hasValueSatisfying(t -> {
            assertThat(t.name()).isEqualTo("Task Updated");
            assertThat(t.status()).isEqualTo("done");
            assertThat(t.description()).isEqualTo("added desc");
        });
    }

    // --- 3.4 Milestone: null ("no milestone") i self-ref ---

    @Test
    void taskWithNoMilestone() {
        seedHierarchy();
        tasks.upsert("t1", "l1", "Plain Task", null, null, false, null);
        assertThat(tasks.findById("t1")).hasValueSatisfying(t -> {
            assertThat(t.isMilestone()).isFalse();
            assertThat(t.milestoneId()).isNull();
        });
    }

    @Test
    void taskWithMilestoneSelfRef() {
        seedHierarchy();
        // milestone task
        tasks.upsert("m1", "l1", "Milestone", null, null, true, null);
        // regular task under milestone
        tasks.upsert("t1", "l1", "Sub Task", null, null, false, "m1");

        assertThat(tasks.findById("m1")).hasValueSatisfying(m -> assertThat(m.isMilestone()).isTrue());
        assertThat(tasks.findById("t1")).hasValueSatisfying(t -> {
            assertThat(t.isMilestone()).isFalse();
            assertThat(t.milestoneId()).isEqualTo("m1");
        });
    }

    // --- 3.5 Relacje FK + zapytania nawigacyjne ---

    @Test
    void findFoldersBySpace() {
        spaces.upsert("s1", "Space");
        folders.upsert("f1", "s1", "Folder A");
        folders.upsert("f2", "s1", "Folder B");
        List<Folder> result = folders.findBySpaceId("s1");
        assertThat(result).extracting(Folder::id).containsExactlyInAnyOrder("f1", "f2");
    }

    @Test
    void findTasksByList() {
        seedHierarchy();
        tasks.upsert("t1", "l1", "Task A", null, null, false, null);
        tasks.upsert("t2", "l1", "Task B", null, null, false, null);
        List<Task> result = tasks.findByListId("l1");
        assertThat(result).extracting(Task::id).containsExactlyInAnyOrder("t1", "t2");
    }

    @Test
    void findMilestonesByList() {
        seedHierarchy();
        tasks.upsert("m1", "l1", "Milestone", null, null, true, null);
        tasks.upsert("t1", "l1", "Task", null, null, false, null);
        List<Task> milestones = tasks.findByListIdAndIsMilestoneTrue("l1");
        assertThat(milestones).extracting(Task::id).containsExactly("m1");
    }

    @Test
    void findTasksUnderMilestone() {
        seedHierarchy();
        tasks.upsert("m1", "l1", "Milestone", null, null, true, null);
        tasks.upsert("t1", "l1", "Task A", null, null, false, "m1");
        tasks.upsert("t2", "l1", "Task B", null, null, false, "m1");
        List<Task> result = tasks.findByMilestoneId("m1");
        assertThat(result).extracting(Task::id).containsExactlyInAnyOrder("t1", "t2");
    }

    @Test
    void folderlessListHasNullFolderId() {
        spaces.upsert("s1", "Space");
        lists.upsert("l1", "Folderless", "s1", null);
        assertThat(lists.findById("l1")).hasValueSatisfying(l ->
                assertThat(l.folderId()).isNull());
    }

    // --- 3.6 Trwałość w obrębie kontenera ---

    @Test
    void dataPersistedAcrossRepositoryQueries() {
        seedHierarchy();
        tasks.upsert("t1", "l1", "Persisted Task", "open", null, false, null);
        // ponowne odpytanie inną metodą — dane muszą być
        assertThat(tasks.findByListId("l1")).hasSize(1);
        assertThat(tasks.findById("t1")).isPresent();
    }

    // --- helpers ---

    private void seedHierarchy() {
        spaces.upsert("s1", "Space");
        folders.upsert("f1", "s1", "Folder");
        lists.upsert("l1", "List", "s1", "f1");
    }
}
