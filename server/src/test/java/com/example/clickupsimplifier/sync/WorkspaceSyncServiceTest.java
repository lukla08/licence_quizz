package com.example.clickupsimplifier.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.clickupsimplifier.clickup.workspace.ClickupFolder;
import com.example.clickupsimplifier.clickup.workspace.ClickupList;
import com.example.clickupsimplifier.clickup.workspace.ClickupRef;
import com.example.clickupsimplifier.clickup.workspace.ClickupSpace;
import com.example.clickupsimplifier.clickup.workspace.ClickupTask;
import com.example.clickupsimplifier.clickup.workspace.ClickupTeam;
import com.example.clickupsimplifier.clickup.workspace.ClickupWorkspaceClient;
import com.example.clickupsimplifier.persistence.FolderRepository;
import com.example.clickupsimplifier.persistence.PostgresTestcontainersConfig;
import com.example.clickupsimplifier.persistence.SpaceRepository;
import com.example.clickupsimplifier.persistence.SyncSet;
import com.example.clickupsimplifier.persistence.SyncSetRepository;
import com.example.clickupsimplifier.persistence.TaskRepository;
import com.example.clickupsimplifier.persistence.WorkspaceListRepository;
import com.example.clickupsimplifier.settings.SettingsStore;
import com.example.clickupsimplifier.sync.SyncJobStatus.SyncState;
import com.example.clickupsimplifier.sync.WorkspaceSyncService.DictionaryPayload;
import com.example.clickupsimplifier.sync.WorkspaceSyncService.FolderEntry;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import(PostgresTestcontainersConfig.class)
class WorkspaceSyncServiceTest {

    @Autowired WorkspaceSyncService service;
    @Autowired SpaceRepository spaces;
    @Autowired FolderRepository folders;
    @Autowired WorkspaceListRepository lists;
    @Autowired TaskRepository tasks;
    @Autowired SyncSetRepository syncSets;

    @MockitoBean ClickupWorkspaceClient workspaceClient;
    @MockitoBean SettingsStore settingsStore;

    private static final String TEAM_ID = "team1";
    private static final String SPACE_ID = "space1";
    private static final String FOLDER_ID = "folder1";
    private static final String LIST_ID_1 = "list1";
    private static final String LIST_ID_2 = "list2";

    private final ClickupTeam team = new ClickupTeam(TEAM_ID, "Team");
    private final ClickupSpace space = new ClickupSpace(SPACE_ID, "Space");
    private final ClickupFolder folder = new ClickupFolder(FOLDER_ID, "Folder", new ClickupRef(SPACE_ID));
    private final ClickupList list1 = new ClickupList(LIST_ID_1, "List 1",
            new ClickupRef(SPACE_ID), new ClickupList.FolderRef(FOLDER_ID, false));
    private final ClickupList list2 = new ClickupList(LIST_ID_2, "List 2",
            new ClickupRef(SPACE_ID), new ClickupList.FolderRef("hidden", true));

    private DictionaryPayload minimalPayload() {
        return new DictionaryPayload(
                List.of(space),
                List.of(new FolderEntry(FOLDER_ID, SPACE_ID, "Folder")),
                List.of(list1, list2)
        );
    }

    @BeforeEach
    void clean() {
        tasks.deleteAll();
        lists.deleteAll();
        folders.deleteAll();
        spaces.deleteAll();
        syncSets.save(new SyncSet("dictionaries", null));
        syncSets.save(new SyncSet("tasks", null));

        when(settingsStore.getToken()).thenReturn(java.util.Optional.of("tok"));
        when(workspaceClient.getTeams("tok")).thenReturn(List.of(team));
        when(workspaceClient.getSpaces("tok", TEAM_ID)).thenReturn(List.of(space));
        when(workspaceClient.getFolders("tok", SPACE_ID)).thenReturn(List.of(folder));
        when(workspaceClient.getListsByFolder("tok", FOLDER_ID)).thenReturn(List.of(list1));
        when(workspaceClient.getFolderlessLists("tok", SPACE_ID)).thenReturn(List.of(list2));
    }

    // ---- 3.4 Replace-all -----------------------------------------------

    @Test
    void fullPull_upsertsAllDictionaries() {
        spaces.insertOrUpdate("stale-space", "Stale");

        service.writeDictionaries(minimalPayload(), null);

        assertThat(spaces.findById(SPACE_ID)).isPresent();
        assertThat(spaces.findById("stale-space")).isEmpty();
        assertThat(folders.findById(FOLDER_ID)).isPresent();
        assertThat(lists.findById(LIST_ID_1)).isPresent();
        assertThat(lists.findById(LIST_ID_2)).isPresent();
        assertThat(syncSets.findById("dictionaries"))
                .hasValueSatisfying(s -> assertThat(s.lastSyncedAt()).isNotNull());
    }

    // ---- 3.5 Incremental (since != null) --------------------------------

    @Test
    void incrementalPull_noDeleteCalled() {
        spaces.insertOrUpdate("extra-space", "Extra");

        service.writeDictionaries(minimalPayload(), Instant.now());

        // Stale entity remains — no delete in incremental mode
        assertThat(spaces.findById("extra-space")).isPresent();
    }

    // ---- 3.6 Error → FAILED status + rollback ---------------------------

    @Test
    void errorInGetSpaces_statusFailedAndNoTimestampUpdate() {
        when(workspaceClient.getSpaces(any(), any()))
                .thenThrow(new RuntimeException("API down"));

        service.triggerPull(null);

        await().atMost(2, TimeUnit.SECONDS)
                .until(() -> service.getStatus().state() == SyncState.FAILED);

        assertThat(service.getStatus().state()).isEqualTo(SyncState.FAILED);
        assertThat(service.getStatus().message()).isEqualTo("API down");
        assertThat(syncSets.findById("dictionaries"))
                .hasValueSatisfying(s -> assertThat(s.lastSyncedAt()).isNull());
    }

    // ---- 3.7 Subtasks: list_id inherited, parent upserted first ----------

    @Test
    void subtasksInheritListIdAndParentIsUpsertedFirst() {
        spaces.insertOrUpdate(SPACE_ID, "Space");
        folders.insertOrUpdate(FOLDER_ID, SPACE_ID, "Folder");
        lists.insertOrUpdate(LIST_ID_1, "List 1", SPACE_ID, FOLDER_ID);

        ClickupTask parent = new ClickupTask("t1", "Parent", null, null, false, null);
        ClickupTask subtask = new ClickupTask("t2", "Sub", null, null, false, "t1");

        service.writeTasks(Map.of(LIST_ID_1, List.of(parent, subtask)), null);

        assertThat(tasks.findById("t1")).hasValueSatisfying(t -> {
            assertThat(t.listId()).isEqualTo(LIST_ID_1);
            assertThat(t.parentId()).isNull();
        });
        assertThat(tasks.findById("t2")).hasValueSatisfying(t -> {
            assertThat(t.listId()).isEqualTo(LIST_ID_1);
            assertThat(t.parentId()).isEqualTo("t1");
        });
    }

    // ---- 3.8 sync_enabled filtering: disabled list tasks are not touched ----

    @Test
    void writeTasks_onlyProcessesListsPassedIn() {
        spaces.insertOrUpdate(SPACE_ID, "Space");
        folders.insertOrUpdate(FOLDER_ID, SPACE_ID, "Folder");
        lists.insertOrUpdate(LIST_ID_1, "List 1", SPACE_ID, FOLDER_ID);
        lists.insertOrUpdate(LIST_ID_2, "List 2", SPACE_ID, null);

        // Pre-seed a task on list2 (disabled list — not included in the map)
        tasks.insertOrUpdate("existing-t", LIST_ID_2, "Existing", null, null, false, null, null);

        ClickupTask t = new ClickupTask("t1", "Task", null, null, false, null);

        // Only list1 is passed in (simulating that fetchTaskData only fetched enabled lists)
        service.writeTasks(Map.of(LIST_ID_1, List.of(t)), null);

        // list1 tasks written
        assertThat(tasks.findById("t1")).isPresent();
        // list2 task untouched (list2 was not in the map)
        assertThat(tasks.findById("existing-t")).isPresent();
    }
}
