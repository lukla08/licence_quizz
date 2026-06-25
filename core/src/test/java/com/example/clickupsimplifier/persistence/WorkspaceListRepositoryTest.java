package com.example.clickupsimplifier.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresTestcontainersConfig.class)
class WorkspaceListRepositoryTest {

    @Autowired SpaceRepository spaces;
    @Autowired WorkspaceListRepository lists;

    @BeforeEach
    void clean() {
        lists.deleteAll();
        spaces.deleteAll();
        spaces.insertOrUpdate("s1", "Space");
    }

    @Test
    void findAllSyncEnabledReturnsOnlyEnabledLists() {
        lists.insertOrUpdate("l1", "List A", "s1", null);
        lists.insertOrUpdate("l2", "List B", "s1", null);
        lists.updateSyncEnabled("l1", true);

        var enabled = lists.findAllSyncEnabled();

        assertThat(enabled).extracting(WorkspaceList::id).containsExactly("l1");
    }

    @Test
    void updateSyncEnabledChangesValue() {
        lists.insertOrUpdate("l1", "List", "s1", null);
        assertThat(lists.findById("l1")).hasValueSatisfying(l -> assertThat(l.syncEnabled()).isFalse());

        lists.updateSyncEnabled("l1", true);
        assertThat(lists.findById("l1")).hasValueSatisfying(l -> assertThat(l.syncEnabled()).isTrue());

        lists.updateSyncEnabled("l1", false);
        assertThat(lists.findById("l1")).hasValueSatisfying(l -> assertThat(l.syncEnabled()).isFalse());
    }

    @Test
    void upsertDoesNotResetSyncEnabled() {
        lists.insertOrUpdate("l1", "List", "s1", null);
        lists.updateSyncEnabled("l1", true);

        // Upsert with new name — sync_enabled must remain true
        lists.insertOrUpdate("l1", "List Renamed", "s1", null);

        assertThat(lists.findById("l1")).hasValueSatisfying(l -> {
            assertThat(l.name()).isEqualTo("List Renamed");
            assertThat(l.syncEnabled()).isTrue();
        });
    }

    @Test
    void updateSyncEnabledReturnsZeroForUnknownId() {
        int updated = lists.updateSyncEnabled("nonexistent", true);
        assertThat(updated).isZero();
    }
}
