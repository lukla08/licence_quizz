package com.example.clickupsimplifier.sync.dto;

import org.springframework.lang.Nullable;
import java.time.Instant;
import java.util.Map;

public record SyncStatusResponse(
        String state,
        @Nullable String message,
        @Nullable Instant startedAt,
        @Nullable Instant completedAt,
        Map<String, SyncSetStatus> syncSets
) {
    public record SyncSetStatus(@Nullable Instant lastSyncedAt) {}
}
