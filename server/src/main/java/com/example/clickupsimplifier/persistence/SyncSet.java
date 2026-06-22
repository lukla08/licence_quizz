package com.example.clickupsimplifier.persistence;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.Nullable;

@Table("sync_set")
public record SyncSet(
        @Id String name,
        @Nullable Instant lastSyncedAt
) {
}
