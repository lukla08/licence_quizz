package com.example.clickupsimplifier.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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
