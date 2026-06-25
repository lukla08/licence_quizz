package com.example.clickupsimplifier.persistence;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Efemeryczny PostgreSQL dla testów (F-02). {@code @ServiceConnection} wiąże kontener
 * jako DataSource aplikacji — testy z pełnym kontekstem dostają realny Postgres bez
 * lokalnej instalacji. Importować przez {@code @Import(PostgresTestcontainersConfig.class)}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }
}
