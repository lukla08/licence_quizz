package com.example.clickupsimplifier.sync;

import com.example.clickupsimplifier.clickup.workspace.ClickupList;
import com.example.clickupsimplifier.clickup.workspace.ClickupTask;
import com.example.clickupsimplifier.clickup.workspace.ClickupWorkspaceClient;
import com.example.clickupsimplifier.persistence.FolderRepository;
import com.example.clickupsimplifier.persistence.SpaceRepository;
import com.example.clickupsimplifier.persistence.SyncSetRepository;
import com.example.clickupsimplifier.persistence.TaskRepository;
import com.example.clickupsimplifier.persistence.WorkspaceList;
import com.example.clickupsimplifier.persistence.WorkspaceListRepository;
import com.example.clickupsimplifier.settings.SettingsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class WorkspaceSyncService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceSyncService.class);

    private final SettingsStore settingsStore;
    private final ClickupWorkspaceClient workspaceClient;
    private final SpaceRepository spaceRepo;
    private final FolderRepository folderRepo;
    private final WorkspaceListRepository listRepo;
    private final TaskRepository taskRepo;
    private final SyncSetRepository syncSetRepo;

    // Self-injection through proxy so @Transactional on syncDictionaries/syncTasks is honoured.
    @Lazy @Autowired
    private WorkspaceSyncService self;

    private final AtomicReference<SyncJobStatus> currentStatus =
            new AtomicReference<>(SyncJobStatus.idle());

    public WorkspaceSyncService(SettingsStore settingsStore,
                                ClickupWorkspaceClient workspaceClient,
                                SpaceRepository spaceRepo,
                                FolderRepository folderRepo,
                                WorkspaceListRepository listRepo,
                                TaskRepository taskRepo,
                                SyncSetRepository syncSetRepo) {
        this.settingsStore = settingsStore;
        this.workspaceClient = workspaceClient;
        this.spaceRepo = spaceRepo;
        this.folderRepo = folderRepo;
        this.listRepo = listRepo;
        this.taskRepo = taskRepo;
        this.syncSetRepo = syncSetRepo;
    }

    public SyncJobStatus getStatus() {
        return currentStatus.get();
    }

    @Async
    public void triggerPull(@Nullable Instant since) {
        Instant startedAt = Instant.now();
        currentStatus.set(SyncJobStatus.running(startedAt));
        try {
            String token = settingsStore.getToken()
                    .orElseThrow(() -> new IllegalStateException("No ClickUp token configured"));
            var teams = workspaceClient.getTeams(token);
            if (teams.isEmpty()) throw new IllegalStateException("ClickUp account has no teams");
            String teamId = teams.get(0).id();
            // Two independent transactions — dictionary commit persists even if task sync fails.
            self.syncDictionaries(token, teamId, since);
            self.syncTasks(token, teamId, since);
            log.info("Workspace sync completed (since={})", since);
            currentStatus.set(SyncJobStatus.completed(startedAt, Instant.now()));
        } catch (Exception e) {
            log.error("Workspace sync failed", e);
            currentStatus.set(SyncJobStatus.failed(e.getMessage(), startedAt, Instant.now()));
        }
    }

    @Transactional
    public void syncDictionaries(String token, String teamId, @Nullable Instant since) {
        record FolderEntry(String id, String spaceId, String name) {}

        var freshSpaces = workspaceClient.getSpaces(token, teamId);
        List<FolderEntry> freshFolders = new ArrayList<>();
        List<ClickupList> freshLists = new ArrayList<>();

        for (var space : freshSpaces) {
            var folders = workspaceClient.getFolders(token, space.id());
            for (var folder : folders) {
                freshFolders.add(new FolderEntry(folder.id(), space.id(), folder.name()));
                freshLists.addAll(workspaceClient.getListsByFolder(token, folder.id()));
            }
            freshLists.addAll(workspaceClient.getFolderlessLists(token, space.id()));
        }

        if (since == null) {
            Set<String> freshListIds = freshLists.stream().map(ClickupList::id).collect(Collectors.toSet());
            Set<String> freshFolderIds = freshFolders.stream().map(FolderEntry::id).collect(Collectors.toSet());
            Set<String> freshSpaceIds = freshSpaces.stream().map(s -> s.id()).collect(Collectors.toSet());

            // Leaf-first delete so CASCADE cleans children before parent rows are touched.
            if (freshListIds.isEmpty()) listRepo.deleteAll();
            else listRepo.deleteByIdNotIn(freshListIds);

            if (freshFolderIds.isEmpty()) folderRepo.deleteAll();
            else folderRepo.deleteByIdNotIn(freshFolderIds);

            if (freshSpaceIds.isEmpty()) spaceRepo.deleteAll();
            else spaceRepo.deleteByIdNotIn(freshSpaceIds);
        }

        // Upsert root→leaf to satisfy FK constraints.
        for (var space : freshSpaces) {
            spaceRepo.insertOrUpdate(space.id(), space.name());
        }
        for (var fe : freshFolders) {
            folderRepo.insertOrUpdate(fe.id(), fe.spaceId(), fe.name());
        }
        for (var list : freshLists) {
            listRepo.insertOrUpdate(list.id(), list.name(), list.spaceId(), list.folderId());
        }

        syncSetRepo.updateLastSyncedAt("dictionaries", Instant.now());
    }

    @Transactional
    public void syncTasks(String token, String teamId, @Nullable Instant since) {
        List<WorkspaceList> enabledLists = listRepo.findAllSyncEnabled();

        for (WorkspaceList list : enabledLists) {
            List<ClickupTask> allApiTasks = workspaceClient.getTasks(token, list.id(), since);

            List<ClickupTask> topLevel = allApiTasks.stream()
                    .filter(t -> t.parent() == null)
                    .toList();
            List<ClickupTask> subTasks = allApiTasks.stream()
                    .filter(t -> t.parent() != null)
                    .toList();

            if (since == null) {
                Set<String> freshIds = allApiTasks.stream()
                        .map(ClickupTask::id)
                        .collect(Collectors.toSet());
                if (freshIds.isEmpty()) {
                    taskRepo.deleteByListId(list.id());
                } else {
                    taskRepo.deleteStaleByListId(list.id(), freshIds);
                }
            }

            // Top-level tasks must be upserted before subtasks (FK parent_id).
            for (ClickupTask t : topLevel) {
                taskRepo.insertOrUpdate(t.id(), list.id(), t.name(), t.statusValue(),
                        t.description(), t.milestone(), null, null);
            }
            for (ClickupTask t : subTasks) {
                taskRepo.insertOrUpdate(t.id(), list.id(), t.name(), t.statusValue(),
                        t.description(), t.milestone(), null, t.parent());
            }
        }

        syncSetRepo.updateLastSyncedAt("tasks", Instant.now());
    }
}
