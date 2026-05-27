package com.stromblex.vartapack.integrity;

import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.validation.Issue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for {@link IntegrityChecker}: paths in integrity.json must not
 * be allowed to escape the game directory via {@code ..} or absolute paths.
 */
final class IntegrityCheckerPathTraversalTest {

    @TempDir
    Path gameDir;

    @Test
    void rejectsParentDirectoryTraversal() throws Exception {
        // Create a sensitive file outside gameDir
        Path outside = gameDir.resolveSibling("outside-secret.txt");
        Files.writeString(outside, "secret");

        IntegrityManifest manifest = new IntegrityManifest(1, List.of(
                new IntegrityManifest.FileEntry("../" + outside.getFileName(), "FILE", "",
                        true, Severity.ERROR, Severity.INFO, "Escaped", "", "")
        ));

        IntegrityChecker checker = new IntegrityChecker();
        List<Issue> issues = checker.check(manifest, gameDir);

        // Must NOT produce a "missing required file" or hash-check issue for a file outside gameDir.
        assertTrue(issues.stream().noneMatch(i -> i.id().startsWith("integrity.missing.")),
                "Path traversal must not let the checker reach files outside the game directory.");
        assertTrue(issues.stream().anyMatch(i -> i.id().startsWith("integrity.invalid_path.")),
                "Path traversal attempts should produce an invalid_path warning.");
    }

    @Test
    void rejectsAbsolutePath() {
        IntegrityManifest manifest = new IntegrityManifest(1, List.of(
                new IntegrityManifest.FileEntry("/etc/passwd", "FILE", "",
                        true, Severity.ERROR, Severity.INFO, "Absolute", "", "")
        ));

        IntegrityChecker checker = new IntegrityChecker();
        List<Issue> issues = checker.check(manifest, gameDir);

        assertTrue(issues.stream().noneMatch(i -> i.id().startsWith("integrity.missing.")),
                "Absolute paths must not be accepted.");
        assertTrue(issues.stream().anyMatch(i -> i.id().startsWith("integrity.invalid_path.")),
                "Absolute paths should produce an invalid_path warning.");
    }
}
