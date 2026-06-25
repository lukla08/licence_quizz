package com.example.clickupsimplifier.settings;

import com.example.clickupsimplifier.config.ClickupProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettingsStoreTest {

    private SettingsStore storeFor(Path settingsFile) {
        ClickupProperties props = new ClickupProperties(
                settingsFile.toString(),
                new ClickupProperties.Api("https://api.clickup.com/api/v2"));
        return new SettingsStore(props, JsonMapper.builder().build());
    }

    @Test
    void savesAndReadsBackToken(@TempDir Path tmp) {
        SettingsStore store = storeFor(tmp.resolve("settings.json"));

        store.saveToken("pk_test_123");

        assertThat(store.getToken()).contains("pk_test_123");
    }

    @Test
    void trimsTokenOnSave(@TempDir Path tmp) {
        SettingsStore store = storeFor(tmp.resolve("settings.json"));

        store.saveToken("  pk_test_123  ");

        assertThat(store.getToken()).contains("pk_test_123");
    }

    @Test
    void rejectsBlankTokenAndDoesNotCreateFile(@TempDir Path tmp) {
        Path file = tmp.resolve("settings.json");
        SettingsStore store = storeFor(file);

        assertThatThrownBy(() -> store.saveToken("   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.saveToken(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(Files.exists(file)).isFalse();
    }

    @Test
    void returnsEmptyWhenFileMissing(@TempDir Path tmp) {
        SettingsStore store = storeFor(tmp.resolve("settings.json"));

        assertThat(store.getToken()).isEmpty();
    }

    @Test
    void atomicWriteLeavesNoTempFileAndReplacesExisting(@TempDir Path tmp) throws IOException {
        SettingsStore store = storeFor(tmp.resolve("settings.json"));

        store.saveToken("pk_first");
        store.saveToken("pk_second");

        assertThat(store.getToken()).contains("pk_second");
        try (Stream<Path> entries = Files.list(tmp)) {
            List<String> names = entries.map(p -> p.getFileName().toString()).toList();
            assertThat(names).containsExactly("settings.json");
        }
    }
}
