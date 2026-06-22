package com.example.clickupsimplifier.sync;

import com.example.clickupsimplifier.persistence.SyncSet;
import com.example.clickupsimplifier.persistence.SyncSetRepository;
import com.example.clickupsimplifier.sync.SyncJobStatus.SyncState;
import com.example.clickupsimplifier.sync.dto.SyncStatusResponse;
import com.example.clickupsimplifier.sync.dto.SyncStatusResponse.SyncSetStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/sync")
class SyncController {

    private final WorkspaceSyncService syncService;
    private final SyncSetRepository syncSetRepo;

    SyncController(WorkspaceSyncService syncService, SyncSetRepository syncSetRepo) {
        this.syncService = syncService;
        this.syncSetRepo = syncSetRepo;
    }

    @PostMapping("/full-pull")
    ResponseEntity<Void> triggerFullPull() {
        if (syncService.getStatus().state() == SyncState.RUNNING) {
            return ResponseEntity.status(409).build();
        }
        syncService.triggerPull(null);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/status")
    SyncStatusResponse getStatus() {
        SyncJobStatus status = syncService.getStatus();
        Map<String, SyncSetStatus> syncSets = StreamSupport
                .stream(syncSetRepo.findAll().spliterator(), false)
                .collect(Collectors.toMap(SyncSet::name, s -> new SyncSetStatus(s.lastSyncedAt())));
        return new SyncStatusResponse(
                status.state().name(),
                status.message(),
                status.startedAt(),
                status.completedAt(),
                syncSets
        );
    }
}
