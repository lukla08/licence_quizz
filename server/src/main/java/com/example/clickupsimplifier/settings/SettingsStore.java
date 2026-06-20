package com.example.clickupsimplifier.settings;

import com.example.clickupsimplifier.config.ClickupProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Przechowuje osobisty token API ClickUp w pliku JSON na dysku (F-01).
 *
 * <p>Zapis jest atomowy (plik tymczasowy + move-replace), by awaria w trakcie
 * zapisu nie uszkodzila settings.json i nie zgubila tokenu. Bezpieczne
 * (szyfrowane) przechowywanie jest swiadomie odlozone na downstream
 * (PRD Access Control) - token lezy plaintext.
 */
@Component
public class SettingsStore {

    private final Path settingsFile;
    private final ObjectMapper objectMapper;

    public SettingsStore(ClickupProperties properties, ObjectMapper objectMapper) {
        this.settingsFile = Path.of(properties.settingsFile());
        this.objectMapper = objectMapper;
    }

    /**
     * Zapisuje token ClickUp. Token jest przycinany; pusty/bialy jest odrzucany
     * (plik nie powstaje).
     *
     * @throws IllegalArgumentException gdy token jest null, pusty lub same biale znaki
     */
    public void saveToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token ClickUp nie moze byc pusty");
        }
        writeAtomically(new Settings(token.trim()));
    }

    /**
     * @return zapisany token albo pusty Optional, gdy plik ustawien nie istnieje
     * lub nie zawiera tokenu
     */
    public Optional<String> getToken() {
        if (!Files.isRegularFile(settingsFile)) {
            return Optional.empty();
        }
        try {
            byte[] bytes = Files.readAllBytes(settingsFile);
            Settings settings = objectMapper.readValue(bytes, Settings.class);
            return Optional.ofNullable(settings.clickupToken()).filter(t -> !t.isBlank());
        } catch (IOException e) {
            throw new UncheckedIOException("Nie mozna odczytac pliku ustawien: " + settingsFile, e);
        }
    }

    private void writeAtomically(Settings settings) {
        Path target = settingsFile.toAbsolutePath();
        Path dir = target.getParent();
        try {
            Files.createDirectories(dir);
            byte[] json = objectMapper.writeValueAsBytes(settings);
            Path tmp = Files.createTempFile(dir, "settings", ".tmp");
            try {
                Files.write(tmp, json);
                try {
                    Files.move(tmp, target,
                            StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException atomicUnsupported) {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(tmp);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Nie mozna zapisac pliku ustawien: " + target, e);
        }
    }

    /**
     * Ksztalt pliku ustawien na dysku.
     */
    record Settings(String clickupToken) {
    }
}
