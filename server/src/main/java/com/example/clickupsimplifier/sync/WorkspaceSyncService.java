package com.example.clickupsimplifier.sync;

import com.example.clickupsimplifier.clickup.workspace.ClickupList;
import com.example.clickupsimplifier.clickup.workspace.ClickupSpace;
import com.example.clickupsimplifier.clickup.workspace.ClickupTask;
import com.example.clickupsimplifier.clickup.workspace.ClickupWorkspaceClient;
import com.example.clickupsimplifier.persistence.FolderRepository;
import com.example.clickupsimplifier.persistence.SpaceRepository;
import com.example.clickupsimplifier.persistence.SyncSetRepository;
import com.example.clickupsimplifier.persistence.TaskRepository;
import com.example.clickupsimplifier.persistence.WorkspaceList;
import com.example.clickupsimplifier.persistence.WorkspaceListRepository;
import com.example.clickupsimplifier.settings.SettingsStore;
import com.example.clickupsimplifier.sync.SyncJobStatus.SyncState;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    // Self-injection through proxy so @Transactional on write methods is honoured.
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

    /**
     * Atomically transitions from any non-RUNNING state to RUNNING.
     * Returns true if this caller claimed the running slot; false if already running.
     * Use in the controller to guard triggerPull — eliminates the check-then-act race.
     */
    public boolean tryClaimRunning() {
        SyncJobStatus current;
        do {
            current = currentStatus.get();
            if (current.state() == SyncState.RUNNING) return false;
        } while (!currentStatus.compareAndSet(current, SyncJobStatus.running(Instant.now())));
        return true;
    }

    @Async
    public void triggerPull(@Nullable Instant since) {
        // startedAt was already set atomically by tryClaimRunning(). Fall back to now()
        // only when triggerPull is called directly (e.g. from tests).
        SyncJobStatus existing = currentStatus.get();
        Instant startedAt;
        if (existing.state() == SyncState.RUNNING) {
            startedAt = existing.startedAt();
        } else {
            startedAt = Instant.now();
            currentStatus.set(SyncJobStatus.running(startedAt));
        }
        try {
            String token = settingsStore.getToken()
                    .orElseThrow(() -> new IllegalStateException("No ClickUp token configured"));
            var teams = workspaceClient.getTeams(token);
            if (teams.isEmpty()) throw new IllegalStateException("ClickUp account has no teams");
            String teamId = teams.get(0).id();

            // Fetch all dictionary data outside any transaction, then write atomically.
            // Dictionary commit persists even if subsequent task sync fails.
            DictionaryPayload dictPayload = fetchDictionaryData(token, teamId);
            self.writeDictionaries(dictPayload, since);

            // Enabled lists now committed; fetch tasks outside transaction, then write atomically.
            List<WorkspaceList> enabledLists = listRepo.findAllSyncEnabled();
            Map<String, List<ClickupTask>> tasksByList = fetchTaskData(token, enabledLists, since);
            self.writeTasks(tasksByList, since);

            log.info("Workspace sync completed (since={})", since);
            currentStatus.set(SyncJobStatus.completed(startedAt, Instant.now()));
        } catch (Exception e) {
            log.error("Workspace sync failed", e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            currentStatus.set(SyncJobStatus.failed(msg, startedAt, Instant.now()));
        }
    }

    private DictionaryPayload fetchDictionaryData(String token, String teamId) {
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
        return new DictionaryPayload(freshSpaces, freshFolders, freshLists);
    }

    private Map<String, List<ClickupTask>> fetchTaskData(
            String token, List<WorkspaceList> enabledLists, @Nullable Instant since) {
        Map<String, List<ClickupTask>> result = new LinkedHashMap<>();
        for (WorkspaceList list : enabledLists) {
            result.put(list.id(), workspaceClient.getTasks(token, list.id(), since));
        }
        return result;
    }

    @Transactional
    public void writeDictionaries(DictionaryPayload payload, @Nullable Instant since) {
        if (since == null) {
            Set<String> freshListIds = payload.lists().stream().map(ClickupList::id).collect(Collectors.toSet());
            Set<String> freshFolderIds = payload.folders().stream().map(FolderEntry::id).collect(Collectors.toSet());
            Set<String> freshSpaceIds = payload.spaces().stream().map(ClickupSpace::id).collect(Collectors.toSet());

            // Leaf-first delete so CASCADE cleans children before parent rows are touched.
            if (freshListIds.isEmpty()) listRepo.deleteAll();
            else listRepo.deleteByIdNotIn(freshListIds);

            if (freshFolderIds.isEmpty()) folderRepo.deleteAll();
            else folderRepo.deleteByIdNotIn(freshFolderIds);

            if (freshSpaceIds.isEmpty()) spaceRepo.deleteAll();
            else spaceRepo.deleteByIdNotIn(freshSpaceIds);
        }

        // Upsert root→leaf to satisfy FK constraints.
        for (var space : payload.spaces()) {
            spaceRepo.insertOrUpdate(space.id(), space.name());
        }
        for (var fe : payload.folders()) {
            folderRepo.insertOrUpdate(fe.id(), fe.spaceId(), fe.name());
        }
        for (var list : payload.lists()) {
            listRepo.insertOrUpdate(list.id(), list.name(), list.spaceId(), list.folderId());
        }
        syncSetRepo.updateLastSyncedAt("dictionaries", Instant.now());
    }

    @Transactional
    public void writeTasks(Map<String, List<ClickupTask>> tasksByList, @Nullable Instant since) {
        for (Map.Entry<String, List<ClickupTask>> entry : tasksByList.entrySet()) {
            String listId = entry.getKey();
            List<ClickupTask> allApiTasks = entry.getValue();

            List<ClickupTask> topLevel = allApiTasks.stream().filter(t -> t.parent() == null).toList();
            List<ClickupTask> subTasks = allApiTasks.stream().filter(t -> t.parent() != null).toList();

            if (since == null) {
                Set<String> freshIds = allApiTasks.stream().map(ClickupTask::id).collect(Collectors.toSet());
                if (freshIds.isEmpty()) taskRepo.deleteByListId(listId);
                else taskRepo.deleteStaleByListId(listId, freshIds);
            }

            // Top-level tasks must be upserted before subtasks (FK parent_id).
            for (ClickupTask t : topLevel) {
                taskRepo.insertOrUpdate(t.id(), listId, t.name(), t.statusValue(),
                        t.description(), t.milestone(), null, null);
            }
            for (ClickupTask t : subTasks) {
                taskRepo.insertOrUpdate(t.id(), listId, t.name(), t.statusValue(),
                        t.description(), t.milestone(), null, t.parent());
            }
        }
        syncSetRepo.updateLastSyncedAt("tasks", Instant.now());
    }

    // Package-private: used by WorkspaceSyncServiceTest in the same package.
    record DictionaryPayload(List<ClickupSpace> spaces, List<FolderEntry> folders, List<ClickupList> lists) {}
    record FolderEntry(String id, String spaceId, String name) {}
}
