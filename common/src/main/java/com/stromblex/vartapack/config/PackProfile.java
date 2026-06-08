package com.stromblex.vartapack.config;

import java.util.Collections;
import java.util.List;

public record PackProfile(
        int schema,
        String packId,
        String packName,
        String profileVersion,
        String supportUrl,
        String homepageUrl,
        List<String> expectedMinecraftVersions,
        List<String> expectedLoaders,
        int minimumJavaMajor,
        long minimumRamMb,
        long recommendedRamMb,
        List<ModRule> requiredMods,
        List<ModRule> recommendedMods,
        List<ModRule> blockedMods,
        List<String> allowedExtraMods
) {
    public PackProfile {
        if (packId == null) packId = "";
        if (packName == null) packName = "";
        if (profileVersion == null) profileVersion = "";
        if (supportUrl == null) supportUrl = "";
        if (homepageUrl == null) homepageUrl = "";
        expectedMinecraftVersions = expectedMinecraftVersions == null
                ? List.of() : List.copyOf(expectedMinecraftVersions);
        expectedLoaders = expectedLoaders == null
                ? List.of() : List.copyOf(expectedLoaders);
        requiredMods = requiredMods == null ? List.of() : List.copyOf(requiredMods);
        recommendedMods = recommendedMods == null ? List.of() : List.copyOf(recommendedMods);
        blockedMods = blockedMods == null ? List.of() : List.copyOf(blockedMods);
        allowedExtraMods = allowedExtraMods == null ? List.of() : List.copyOf(allowedExtraMods);
    }

    public static PackProfile defaults() {
        return new PackProfile(
                1,
                "example-pack",
                "Example Pack",
                "1.0.0",
                "",
                "",
                List.of("1.18.2"),
                List.of("fabric", "forge"),
                17,
                4096,
                6144,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    /**
     * Returns an unconfigured profile used when no profile.json exists.
     * Empty lists and zero thresholds mean no checks are performed.
     */
    public static PackProfile empty() {
        return new PackProfile(
                1,
                "",
                "",
                "",
                "",
                "",
                List.of(),
                List.of(),
                0,
                0,
                0,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
}
