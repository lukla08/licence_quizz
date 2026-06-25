package com.example.clickupsimplifier.persistence;

import java.time.Instant;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface SyncSetRepository extends CrudRepository<SyncSet, String> {

    @Modifying
    @Query("UPDATE sync_set SET last_synced_at = :ts WHERE name = :name")
    void updateLastSyncedAt(String name, Instant ts);
}
