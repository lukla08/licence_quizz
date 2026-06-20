package com.example.clickupsimplifier.clickup;

import com.example.clickupsimplifier.config.ClickupProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Konfiguracja klienta HTTP do API ClickUp (F-01).
 *
 * <p>Token NIE jest wpinany na stale - leci per-wywolanie ({@link ClickupClient}),
 * bo moze sie zmienic w ustawieniach.
 */
@Configuration
public class ClickupClientConfig {

    @Bean
    RestClient clickupRestClient(ClickupProperties properties) {
        return RestClient.builder().baseUrl(properties.api().baseUrl()).build();
    }
}
