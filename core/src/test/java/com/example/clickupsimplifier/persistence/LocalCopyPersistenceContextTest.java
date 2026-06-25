package com.example.clickupsimplifier.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Dowodzi, że pełny kontekst aplikacji wstaje na Postgresie z Testcontainers —
 * datasource + Spring Data JDBC + Flyway autoconfig są poprawnie wpięte (F-02, Faza 1).
 */
@SpringBootTest
@Import(PostgresTestcontainersConfig.class)
class LocalCopyPersistenceContextTest {

    @Test
    void contextLoadsWithPostgres() {
    }
}
