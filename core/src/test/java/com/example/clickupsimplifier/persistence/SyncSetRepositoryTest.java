package com.example.clickupsimplifier.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresTestcontainersConfig.class)
class SyncSetRepositoryTest {

    @Autowired
    SyncSetRepository syncSets;

    @BeforeEach
    void resetTimestamps() {
        syncSets.save(new SyncSet("dictionaries", null));
        syncSets.save(new SyncSet("tasks", null));
    }

    @Test
    void twoRowsPreseededByMigration() {
        assertThat(syncSets.findById("dictionaries")).isPresent();
        assertThat(syncSets.findById("tasks")).isPresent();
    }

    @Test
    void lastSyncedAtIsNullInitially() {
        assertThat(syncSets.findById("dictionaries"))
                .hasValueSatisfying(s -> assertThat(s.lastSyncedAt()).isNull());
    }

    @Test
    void updateLastSyncedAt() {
        Instant ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        syncSets.updateLastSyncedAt("tasks", ts);

        assertThat(syncSets.findById("tasks"))
                .hasValueSatisfying(s -> assertThat(s.lastSyncedAt()).isEqualTo(ts));
    }
}
