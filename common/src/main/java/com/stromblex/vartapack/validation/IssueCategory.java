package com.stromblex.vartapack.validation;

/**
 * Category classification for issues. Helps group related issues
 * in the UI and report.
 */
public enum IssueCategory {
    ENVIRONMENT("Environment", "Java, RAM, OS-related issues"),
    LOADER("Loader", "Mod loader version or type issues"),
    MINECRAFT_VERSION("Minecraft Version", "Minecraft version mismatch"),
    REQUIRED_MOD("Required Mods", "Missing or wrong-version required mods"),
    BLOCKED_MOD("Blocked Mods", "Forbidden/incompatible mods detected"),
    RECOMMENDED_MOD("Recommended Mods", "Missing recommended mods"),
    EXTRA_MOD("Extra Mods", "Mods not in the modpack profile"),
    MOD_CONFLICT("Mod Conflicts", "Known conflicting mod combinations"),
    DUPLICATE_MOD("Duplicate Mods", "Multiple mods providing the same ID"),
    INTEGRITY("File Integrity", "File hash mismatches or missing files"),
    CRASH("Crash Analysis", "Issues detected from crash/log analysis"),
    RENDERING_CONFLICT("Rendering Conflict", "Conflicting rendering/shader mods"),
    CONFIGURATION("Configuration", "Config file issues"),
    INTERNAL("Internal", "VartaPack internal issues");

    private final String displayName;
    private final String description;

    IssueCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() { return displayName; }
    public String description() { return description; }
}
