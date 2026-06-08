package com.stromblex.vartapack.integrity;

import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.validation.Issue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class IntegrityCheckerTest {

    @TempDir
    Path gameDir;

    @Test
    void matchingHashProducesNoIssue() throws Exception {
        Path modFile = gameDir.resolve("mods/sodium.jar");
        Files.createDirectories(modFile.getParent());
        Files.writeString(modFile, "fake jar content");

        String hash = IntegrityChecker.computeSha256(modFile);

        IntegrityManifest manifest = new IntegrityManifest(1, List.of(
                new IntegrityManifest.FileEntry("mods/sodium.jar", "MOD", hash,
                        true, Severity.ERROR, Severity.WARNING, "Sodium", "", "")
        ));

        IntegrityChecker checker = new IntegrityChecker();
        List<Issue> issues = checker.check(manifest, gameDir);

        assertTrue(issues.isEmpty());
    }

    @Test
    void mismatchedHashProducesIssue() throws Exception {
        Path modFile = gameDir.resolve("mods/sodium.jar");
        Files.createDirectories(modFile.getParent());
        Files.writeString(modFile, "modified content");

        IntegrityManifest manifest = new IntegrityManifest(1, List.of(
                new IntegrityManifest.FileEntry("mods/sodium.jar", "MOD",
                        "0000000000000000000000000000000000000000000000000000000000000000",
                        true, Severity.ERROR, Severity.WARNING, "Sodium",
                        "File was modified.", "Reinstall the modpack.")
        ));

        IntegrityChecker checker = new IntegrityChecker();
        List<Issue> issues = checker.check(manifest, gameDir);

        assertEquals(1, issues.size());
        assertEquals(Severity.WARNING, issues.get(0).severity());
        assertTrue(issues.get(0).title().contains("modified"));
    }

    @Test
    void missingRequiredFileProducesIssue() {
        IntegrityManifest manifest = new IntegrityManifest(1, List.of(
                new IntegrityManifest.FileEntry("mods/important.jar", "MOD", "",
                        true, Severity.ERROR, Severity.INFO, "Important Mod",
                        "Required for the pack.", "Reinstall.")
        ));

        IntegrityChecker checker = new IntegrityChecker();
        List<Issue> issues = checker.check(manifest, gameDir);

        assertEquals(1, issues.size());
        assertEquals(Severity.ERROR, issues.get(0).severity());
        assertTrue(issues.get(0).title().contains("Missing"));
    }

    @Test
    void missingOptionalFileProducesNoIssue() {
        IntegrityManifest manifest = new IntegrityManifest(1, List.of(
                new IntegrityManifest.FileEntry("config/optional.json", "CONFIG", "",
                        false, Severity.WARNING, Severity.INFO, "Optional Config", "", "")
        ));

        IntegrityChecker checker = new IntegrityChecker();
        List<Issue> issues = checker.check(manifest, gameDir);

        assertTrue(issues.isEmpty());
    }

    @Test
    void sha256ComputationWorks() throws Exception {
        Path file = gameDir.resolve("test.txt");
        Files.writeString(file, "hello");
        String hash = IntegrityChecker.computeSha256(file);
        // SHA-256 of "hello" is well-known
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", hash);
    }
}
