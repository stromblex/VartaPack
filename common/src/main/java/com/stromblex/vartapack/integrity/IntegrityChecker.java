package com.stromblex.vartapack.integrity;

import com.stromblex.vartapack.VartaPack;
import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.validation.Issue;
import com.stromblex.vartapack.validation.IssueCategory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks files against the integrity manifest.
 * Produces issues for missing or changed files.
 */
public final class IntegrityChecker {

    public List<Issue> check(IntegrityManifest manifest, Path gameDir) {
        List<Issue> issues = new ArrayList<>();
        Path normalizedBase = gameDir.toAbsolutePath().normalize();
        for (IntegrityManifest.FileEntry entry : manifest.files()) {
            if (entry.path().isBlank()) continue;
            Path filePath = resolveSafe(normalizedBase, entry.path());
            if (filePath == null) {
                issues.add(Issue.builder("integrity.invalid_path." + sanitizeId(entry.path()))
                        .title("Unsafe path in integrity manifest: " + entry.displayName())
                        .severity(Severity.WARNING)
                        .category(IssueCategory.INTEGRITY)
                        .message("Path escapes the instance directory: " + entry.path())
                        .fix("Use a path relative to the game directory (no '..' or absolute paths).")
                        .affectedFile(entry.path())
                        .blocksContinue(false)
                        .build());
                continue;
            }

            if (!Files.exists(filePath)) {
                if (entry.required()) {
                    issues.add(Issue.builder("integrity.missing." + sanitizeId(entry.path()))
                            .title("Missing file: " + entry.displayName())
                            .severity(entry.severityIfMissing())
                            .category(IssueCategory.INTEGRITY)
                            .message("Expected file not found: " + entry.path())
                            .detailedExplanation(entry.reason())
                            .fix(entry.fix().isBlank()
                                    ? "Restore the missing file or reinstall the modpack."
                                    : entry.fix())
                            .affectedFile(entry.path())
                            .blocksContinue(entry.severityIfMissing().ordinal() >= Severity.ERROR.ordinal())
                            .build());
                }
                continue;
            }

            if (!entry.sha256().isBlank()) {
                String actual = computeSha256(filePath);
                if (!actual.isEmpty() && !actual.equalsIgnoreCase(entry.sha256())) {
                    issues.add(Issue.builder("integrity.changed." + sanitizeId(entry.path()))
                            .title("File modified: " + entry.displayName())
                            .severity(entry.severityIfChanged())
                            .category(IssueCategory.INTEGRITY)
                            .message("File has been modified: " + entry.path())
                            .detailedExplanation(entry.reason().isBlank()
                                    ? "Expected hash does not match actual file content."
                                    : entry.reason())
                            .fix(entry.fix().isBlank()
                                    ? "Restore the original file or reinstall the modpack."
                                    : entry.fix())
                            .affectedFile(entry.path())
                            .blocksContinue(entry.severityIfChanged().ordinal() >= Severity.ERROR.ordinal())
                            .build());
                }
            }
        }
        return issues;
    }

    /**
     * Compute SHA-256 hash of a file. Returns empty string on error.
     */
    public static String computeSha256(Path file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    md.update(buffer, 0, read);
                }
            }
            byte[] hash = md.digest();
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            VartaPack.LOGGER.debug("Failed to compute hash for {}: {}", file, e.getMessage());
            return "";
        }
    }

    private static String sanitizeId(String path) {
        return path.replace('/', '.').replace('\\', '.').replace(' ', '_');
    }

    /**
     * Resolve {@code rel} against {@code base} and ensure the result stays inside {@code base}.
     * Rejects absolute paths, paths escaping via {@code ..}, and any I/O failures.
     *
     * @return the resolved path, or {@code null} if the path is unsafe.
     */
    private static Path resolveSafe(Path base, String rel) {
        try {
            Path candidate = base.resolve(rel).normalize();
            if (!candidate.startsWith(base)) return null;
            return candidate;
        } catch (Exception e) {
            return null;
        }
    }
}
