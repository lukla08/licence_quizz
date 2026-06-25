package com.example.clickupsimplifier.sync;

import com.example.clickupsimplifier.persistence.WorkspaceListRepository;
import com.example.clickupsimplifier.sync.dto.ListResponse;
import com.example.clickupsimplifier.sync.dto.SyncEnabledRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
class ListController {

    private final WorkspaceListRepository listRepo;

    ListController(WorkspaceListRepository listRepo) {
        this.listRepo = listRepo;
    }

    @GetMapping("/lists")
    List<ListResponse> getLists() {
        return listRepo.findAllOrderedForDisplay().stream()
                .map(l -> new ListResponse(l.id(), l.name(), l.syncEnabled(), l.folderId(), l.spaceId()))
                .toList();
    }

    @PutMapping("/lists/{id}/sync-enabled")
    ResponseEntity<Void> updateSyncEnabled(@PathVariable String id,
                                           @RequestBody SyncEnabledRequest request) {
        int updated = listRepo.updateSyncEnabled(id, request.enabled());
        if (updated == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
