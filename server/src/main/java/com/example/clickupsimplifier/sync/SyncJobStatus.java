package com.example.clickupsimplifier.sync;

import java.time.Instant;
import org.springframework.lang.Nullable;

public record SyncJobStatus(
        SyncState state,
        @Nullable String message,
        @Nullable Instant startedAt,
        @Nullable Instant completedAt
) {
    public enum SyncState { IDLE, RUNNING, COMPLETED, FAILED }

    public static SyncJobStatus idle() {
        return new SyncJobStatus(SyncState.IDLE, null, null, null);
    }

    public static SyncJobStatus running(Instant startedAt) {
        return new SyncJobStatus(SyncState.RUNNING, null, startedAt, null);
    }

    public static SyncJobStatus completed(Instant completedAt) {
        return new SyncJobStatus(SyncState.COMPLETED, null, null, completedAt);
    }

    public static SyncJobStatus failed(String message, Instant completedAt) {
        return new SyncJobStatus(SyncState.FAILED, message, null, completedAt);
    }
}
