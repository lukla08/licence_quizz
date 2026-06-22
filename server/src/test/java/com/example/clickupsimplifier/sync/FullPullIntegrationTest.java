package com.example.clickupsimplifier.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.clickupsimplifier.clickup.workspace.ClickupFolder;
import com.example.clickupsimplifier.clickup.workspace.ClickupList;
import com.example.clickupsimplifier.clickup.workspace.ClickupRef;
import com.example.clickupsimplifier.clickup.workspace.ClickupSpace;
import com.example.clickupsimplifier.clickup.workspace.ClickupTask;
import com.example.clickupsimplifier.clickup.workspace.ClickupTeam;
import com.example.clickupsimplifier.clickup.workspace.ClickupWorkspaceClient;
import com.example.clickupsimplifier.persistence.PostgresTestcontainersConfig;
import com.example.clickupsimplifier.persistence.SyncSet;
import com.example.clickupsimplifier.persistence.SyncSetRepository;
import com.example.clickupsimplifier.persistence.FolderRepository;
import com.example.clickupsimplifier.persistence.SpaceRepository;
import com.example.clickupsimplifier.persistence.TaskRepository;
import com.example.clickupsimplifier.persistence.WorkspaceListRepository;
import com.example.clickupsimplifier.settings.SettingsStore;
import com.example.clickupsimplifier.sync.SyncJobStatus.SyncState;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfig.class)
class FullPullIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired WorkspaceSyncService syncService;
    @Autowired SpaceRepository spaceRepo;
    @Autowired FolderRepository folderRepo;
    @Autowired WorkspaceListRepository listRepo;
    @Autowired TaskRepository taskRepo;
    @Autowired SyncSetRepository syncSetRepo;

    @MockitoBean ClickupWorkspaceClient workspaceClient;
    @MockitoBean SettingsStore settingsStore;

    private static final String TEAM_ID = "team1";
    private static final String SPACE_ID = "s1";
    private static final String FOLDER_ID = "f1";
    private static final String LIST_ID_1 = "l1";
    private static final String LIST_ID_2 = "l2";

    private final ClickupTeam team = new ClickupTeam(TEAM_ID, "Team");
    private final ClickupSpace space = new ClickupSpace(SPACE_ID, "Space");
    private final ClickupFolder folder = new ClickupFolder(FOLDER_ID, "Folder", new ClickupRef(SPACE_ID));
    private final ClickupList list1 = new ClickupList(LIST_ID_1, "List One",
            new ClickupRef(SPACE_ID), new ClickupList.FolderRef(FOLDER_ID, false));
    private final ClickupList list2 = new ClickupList(LIST_ID_2, "List Two",
            new ClickupRef(SPACE_ID), new ClickupList.FolderRef("hidden", true));

    @BeforeEach
    void setUp() {
        taskRepo.deleteAll();
        listRepo.deleteAll();
        folderRepo.deleteAll();
        spaceRepo.deleteAll();
        syncSetRepo.save(new SyncSet("dictionaries", null));
        syncSetRepo.save(new SyncSet("tasks", null));

        when(settingsStore.getToken()).thenReturn(Optional.of("tok"));
        when(workspaceClient.getTeams("tok")).thenReturn(List.of(team));
        when(workspaceClient.getSpaces("tok", TEAM_ID)).thenReturn(List.of(space));
        when(workspaceClient.getFolders("tok", SPACE_ID)).thenReturn(List.of(folder));
        when(workspaceClient.getListsByFolder("tok", FOLDER_ID)).thenReturn(List.of(list1));
        when(workspaceClient.getFolderlessLists("tok", SPACE_ID)).thenReturn(List.of(list2));
    }

    @Test
    void fullScenario_syncEnabled_controlsTaskSync() throws Exception {
        // Step 2: first pull — no sync_enabled lists
        mockMvc.perform(post("/api/sync/full-pull"))
                .andExpect(status().isAccepted());

        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> syncService.getStatus().state() == SyncState.COMPLETED);

        // Step 3: dictionaries present, task table empty
        assertThat(spaceRepo.findById(SPACE_ID)).isPresent();
        assertThat(folderRepo.findById(FOLDER_ID)).isPresent();
        assertThat(listRepo.findById(LIST_ID_1)).isPresent();
        assertThat(listRepo.findById(LIST_ID_2)).isPresent();
        assertThat(taskRepo.count()).isZero();

        // Step 4: mark list1 as sync_enabled
        mockMvc.perform(put("/api/lists/" + LIST_ID_1 + "/sync-enabled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": true}"))
                .andExpect(status().isNoContent());

        // Set up task responses for second pull
        ClickupTask parent = new ClickupTask("t1", "Parent Task", null, null, false, null, List.of());
        ClickupTask subtask = new ClickupTask("t2", "Sub Task", null, null, false, "t1", List.of());
        when(workspaceClient.getTasks(eq("tok"), eq(LIST_ID_1), any()))
                .thenReturn(List.of(parent, subtask));

        // Step 5: second pull
        mockMvc.perform(post("/api/sync/full-pull"))
                .andExpect(status().isAccepted());

        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> syncService.getStatus().state() == SyncState.COMPLETED);

        // Step 6: assertions
        assertThat(taskRepo.findById("t1")).isPresent();
        assertThat(taskRepo.findById("t2")).hasValueSatisfying(t -> {
            assertThat(t.parentId()).isEqualTo("t1");
            assertThat(t.listId()).isEqualTo(LIST_ID_1);
        });
        // list2 is not sync_enabled — no tasks for it
        assertThat(taskRepo.findAll()).extracting(t -> t.listId())
                .allMatch(id -> id.equals(LIST_ID_1));

        assertThat(syncSetRepo.findById("dictionaries"))
                .hasValueSatisfying(s -> assertThat(s.lastSyncedAt()).isNotNull());
        assertThat(syncSetRepo.findById("tasks"))
                .hasValueSatisfying(s -> assertThat(s.lastSyncedAt()).isNotNull());
    }
}
