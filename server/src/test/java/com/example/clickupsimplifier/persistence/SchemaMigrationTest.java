package com.example.clickupsimplifier.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Dowodzi, że Flyway aplikuje V1 czysto na Postgresie z Testcontainers i że schemat
 * zgadza się z kontraktem planu (F-02, Faza 2): cztery tabele lokalnej kopii plus
 * kluczowe kolumny/ograniczenia milestone'a.
 */
@SpringBootTest
@Import(PostgresTestcontainersConfig.class)
class SchemaMigrationTest {

    @Autowired
    JdbcClient jdbcClient;

    @BeforeEach
    void cleanTestData() {
        // CASCADE usunie folder/list/task pod space s1
        jdbcClient.sql("DELETE FROM space WHERE id = 's1'").update();
    }

    @Test
    void createsFourWorkspaceTables() {
        List<String> tables = jdbcClient
                .sql("SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'public' ORDER BY table_name")
                .query(String.class)
                .list();

        assertThat(tables).contains("space", "folder", "list", "task");
    }

    @Test
    void taskCarriesMilestoneColumns() {
        List<String> columns = columnsOf("task");

        assertThat(columns).contains("id", "list_id", "name", "status",
                "description", "is_milestone", "milestone_id");
    }

    @Test
    void folderlessListAllowsNullFolderButRequiresSpace() {
        assertThat(isNullable("list", "folder_id")).isTrue();
        assertThat(isNullable("list", "space_id")).isFalse();
    }

    @Test
    void isMilestoneIsNotNull() {
        assertThat(isNullable("task", "is_milestone")).isFalse();
    }

    // --- V2 assertions ---

    @Test
    void taskCarriesParentIdColumn() {
        assertThat(columnsOf("task")).contains("parent_id");
        assertThat(isNullable("task", "parent_id")).isTrue();
    }

    @Test
    void syncSetTableExistsWithTwoRows() {
        assertThat(columnsOf("sync_set")).contains("name", "last_synced_at");

        List<String> names = jdbcClient
                .sql("SELECT name FROM sync_set ORDER BY name")
                .query(String.class)
                .list();
        assertThat(names).containsExactly("dictionaries", "tasks");
    }

    @Test
    void deleteListCascadesToTasks() {
        // Seed minimal hierarchy
        jdbcClient.sql("INSERT INTO space VALUES ('s1','Space')").update();
        jdbcClient.sql("INSERT INTO folder VALUES ('f1','s1','Folder')").update();
        jdbcClient.sql("INSERT INTO list (id, name, space_id, folder_id) VALUES ('l1','List','s1','f1')").update();
        jdbcClient.sql("INSERT INTO task (id,list_id,name,is_milestone) VALUES ('t1','l1','Task',false)").update();

        jdbcClient.sql("DELETE FROM list WHERE id = 'l1'").update();

        long taskCount = jdbcClient.sql("SELECT count(*) FROM task WHERE id = 't1'")
                .query(Long.class).single();
        assertThat(taskCount).isZero();
    }

    // --- V3 assertions ---

    @Test
    void listCarriesSyncEnabledColumn() {
        assertThat(columnsOf("list")).contains("sync_enabled");
        assertThat(isNullable("list", "sync_enabled")).isFalse();
    }

    @Test
    void syncEnabledDefaultIsFalse() {
        jdbcClient.sql("INSERT INTO space VALUES ('s1','Space')").update();
        jdbcClient.sql("INSERT INTO list (id, name, space_id) VALUES ('l1','List','s1')").update();

        Boolean enabled = jdbcClient
                .sql("SELECT sync_enabled FROM list WHERE id = 'l1'")
                .query(Boolean.class)
                .single();
        assertThat(enabled).isFalse();
    }

    private List<String> columnsOf(String table) {
        return jdbcClient
                .sql("SELECT column_name FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = :t")
                .param("t", table)
                .query(String.class)
                .list();
    }

    private boolean isNullable(String table, String column) {
        String nullable = jdbcClient
                .sql("SELECT is_nullable FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = :t AND column_name = :c")
                .param("t", table)
                .param("c", column)
                .query(String.class)
                .single();
        return "YES".equals(nullable);
    }
}
