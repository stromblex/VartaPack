package com.stromblex.vartapack.config;

import com.stromblex.vartapack.check.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigManagerTest {
    @TempDir
    Path gameDir;

    @Test
    void migratesLegacyVartaConfigName() throws Exception {
        ConfigManager manager = new ConfigManager(gameDir);
        Files.createDirectories(manager.configDir());
        Files.writeString(manager.legacyVartaConfigPath(), """
                {
                  "schema": 1,
                  "enabled": true,
                  "showToastOnStartup": false,
                  "requiredModsSeverity": "CRITICAL"
                }
                """);

        VartaConfig config = manager.loadVartaConfig();

        assertTrue(Files.exists(manager.vartaConfigPath()));
        assertFalse(Files.exists(manager.legacyVartaConfigPath()));
        assertFalse(config.showToastOnStartup());
        assertEquals(Severity.CRITICAL, config.requiredModsSeverity());
    }

    @Test
    void savesAndLoadsPackProfile() {
        ConfigManager manager = new ConfigManager(gameDir);
        PackProfile profile = new PackProfile(
                1,
                "real-pack",
                "Real Pack",
                "2.0.0",
                "https://example.com/support",
                "https://example.com",
                List.of("1.21.1"),
                List.of("fabric"),
                21,
                4096,
                6144,
                List.of(new ModRule("sodium", "Sodium", ">=0.5.0", "renderer")),
                List.of(),
                List.of(new ModRule("optifine", "OptiFine", "", "incompatible")),
                List.of("fabric-api")
        );

        manager.savePackProfile(profile);
        PackProfile loaded = manager.loadPackProfile();

        assertEquals("real-pack", loaded.packId());
        assertEquals(List.of("1.21.1"), loaded.expectedMinecraftVersions());
        assertEquals("sodium", loaded.requiredMods().getFirst().id());
        assertEquals("optifine", loaded.blockedMods().getFirst().id());
    }
}