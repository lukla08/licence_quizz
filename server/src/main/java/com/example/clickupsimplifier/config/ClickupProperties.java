package com.example.clickupsimplifier.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Konfiguracja rdzenia ClickUp Simplifier (prefiks {@code clickup}).
 *
 * @param settingsFile sciezka do pliku ustawien na dysku (token, itp.)
 * @param api          ustawienia polaczenia z API ClickUp
 */
@ConfigurationProperties(prefix = "clickup")
public record ClickupProperties(String settingsFile, Api api) {

    /**
     * @param baseUrl bazowy URL API ClickUp (np. https://api.clickup.com/api/v2)
     */
    public record Api(String baseUrl) {
    }
}
