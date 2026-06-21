package com.example.clickupsimplifier.persistence;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("sync_set")
public record SyncSet(
        @Id String name,
        Instant lastSyncedAt
) {
}
