package com.stromblex.vartapack.validation;

/**
 * High-level status classification for a modpack instance.
 * Determined by the worst issue found during validation.
 */
public enum PackStatus {
    /** Instance matches the expected profile with no relevant issues. */
    CLEAN,
    /** Player added or changed something, but no dangerous issue was found. */
    MODIFIED,
    /** Instance may run, but support is limited due to important differences. */
    UNSUPPORTED,
    /** A critical issue exists; instance should not be considered safe or supported. */
    BROKEN;

    public String displayName() {
        return switch (this) {
            case CLEAN -> "Clean";
            case MODIFIED -> "Modified";
            case UNSUPPORTED -> "Unsupported";
            case BROKEN -> "Broken";
        };
    }

    public String description() {
        return switch (this) {
            case CLEAN -> "This instance matches the expected modpack profile.";
            case MODIFIED -> "This instance has been modified, but no critical issues were found.";
            case UNSUPPORTED -> "This instance differs significantly from the expected profile. Support may be limited.";
            case BROKEN -> "This instance has critical issues and should not be considered safe or supported.";
        };
    }
}
