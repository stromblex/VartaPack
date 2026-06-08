package com.stromblex.vartapack.doctor;

import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.validation.PackStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class DoctorRunnerTest {

    @TempDir
    Path instanceDir;

    @Test
    void emptyInstanceReportsWarnings() {
        DoctorOptions opts = DoctorOptions.defaults(instanceDir.toString());
        DoctorRunner runner = new DoctorRunner();
        DoctorRunner.DoctorResult result = runner.run(opts);

        // No mods dir or config dir -> warnings
        assertEquals(DoctorExitCode.WARNINGS, result.exitCode());
        assertFalse(result.issues().isEmpty());
    }

    @Test
    void validInstanceWithConfigReturnsClean() throws Exception {
        Files.createDirectories(instanceDir.resolve("mods"));
        Path configDir = instanceDir.resolve("config/vartapack");
        Files.createDirectories(configDir);
        Files.writeString(configDir.resolve("profile.json"), """
                {
                  "schema": 1,
                  "packId": "test-pack",
                  "packName": "Test Pack",
                  "profileVersion": "1.0.0",
                  "expectedMinecraftVersions": ["1.18.2"],
                  "expectedLoaders": ["fabric"],
                  "minimumJavaMajor": 17,
                  "minimumRamMb": 4096,
                  "recommendedRamMb": 6144,
                  "requiredMods": [],
                  "blockedMods": [],
                  "recommendedMods": [],
                  "allowedExtraMods": []
                }
                """);
        Files.writeString(configDir.resolve("vartapack.json"), """
                {
                  "schema": 1,
                  "enabled": true
                }
                """);

        DoctorOptions opts = DoctorOptions.defaults(instanceDir.toString());
        DoctorRunner runner = new DoctorRunner();
        DoctorRunner.DoctorResult result = runner.run(opts);

        assertEquals(DoctorExitCode.OK, result.exitCode());
        assertEquals(PackStatus.CLEAN, result.status());
    }

    @Test
    void invalidProfileJsonReportsError() throws Exception {
        Files.createDirectories(instanceDir.resolve("mods"));
        Path configDir = instanceDir.resolve("config/vartapack");
        Files.createDirectories(configDir);
        Files.writeString(configDir.resolve("profile.json"), "{ invalid json!!!");

        DoctorOptions opts = DoctorOptions.defaults(instanceDir.toString());
        DoctorRunner runner = new DoctorRunner();
        DoctorRunner.DoctorResult result = runner.run(opts);

        assertEquals(DoctorExitCode.ERRORS, result.exitCode());
        assertTrue(result.issues().stream()
                .anyMatch(i -> i.id().equals("doctor.invalid_profile")),
                "Doctor should report an invalid_profile issue for malformed JSON.");
    }

    @Test
    void nonexistentDirectoryReturnsErrors() {
        DoctorOptions opts = DoctorOptions.defaults("/tmp/nonexistent_vartapack_test_dir_xyz");
        DoctorRunner runner = new DoctorRunner();
        DoctorRunner.DoctorResult result = runner.run(opts);

        assertEquals(DoctorExitCode.ERRORS, result.exitCode());
        assertEquals(PackStatus.BROKEN, result.status());
    }

    @Test
    void integrityCheckInDoctor() throws Exception {
        Files.createDirectories(instanceDir.resolve("mods"));
        Path configDir = instanceDir.resolve("config/vartapack");
        Files.createDirectories(configDir);
        Files.writeString(configDir.resolve("profile.json"), """
                { "schema": 1, "packId": "test", "packName": "Test" }
                """);

        // Create a mod file
        Path modFile = instanceDir.resolve("mods/test.jar");
        Files.writeString(modFile, "test content");

        // Create integrity manifest with wrong hash
        Files.writeString(configDir.resolve("integrity.json"), """
                {
                  "schema": 1,
                  "files": [
                    {
                      "path": "mods/test.jar",
                      "type": "MOD",
                      "sha256": "0000000000000000000000000000000000000000000000000000000000000000",
                      "required": true,
                      "severityIfMissing": "ERROR",
                      "severityIfChanged": "WARNING"
                    }
                  ]
                }
                """);

        DoctorOptions opts = DoctorOptions.defaults(instanceDir.toString());
        DoctorRunner runner = new DoctorRunner();
        DoctorRunner.DoctorResult result = runner.run(opts);

        assertTrue(result.issues().stream()
                .anyMatch(i -> i.id().contains("integrity")));
    }
}
