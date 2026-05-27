package com.stromblex.vartapack.integrity;

import com.stromblex.vartapack.check.Severity;

import java.util.List;

/**
 * Parsed integrity manifest (integrity.json). Defines expected file hashes
 * for modpack integrity verification.
 */
public record IntegrityManifest(
        int schema,
        List<FileEntry> files
) {
    public IntegrityManifest {
        if (files == null) files = List.of();
    }

    public static IntegrityManifest empty() {
        return new IntegrityManifest(1, List.of());
    }

    /**
     * A single file entry in the integrity manifest.
     */
    public record FileEntry(
            String path,
            String type,
            String sha256,
            boolean required,
            Severity severityIfMissing,
            Severity severityIfChanged,
            String displayName,
            String reason,
            String fix
    ) {
        public FileEntry {
            if (path == null) path = "";
            if (type == null) type = "FILE";
            if (sha256 == null) sha256 = "";
            if (severityIfMissing == null) severityIfMissing = Severity.WARNING;
            if (severityIfChanged == null) severityIfChanged = Severity.INFO;
            if (displayName == null) displayName = path;
            if (reason == null) reason = "";
            if (fix == null) fix = "";
        }
    }
}
