package com.stromblex.vartapack.check;

import java.util.List;

public final class MinecraftVersionCheck implements Check {
    @Override public String id() { return "minecraftVersion"; }

    @Override
    public List<CheckResult> run(CheckContext ctx) {
        List<String> expected = ctx.profile().expectedMinecraftVersions();
        if (expected.isEmpty()) return List.of();
        String actual = ctx.environment().minecraftVersion();
        if (actual == null || actual.isBlank()) {
            return List.of(CheckResult.of(Severity.WARNING,
                    "mc.unknown",
                    "Minecraft version unknown",
                    "Could not detect the running Minecraft version."));
        }
        for (String e : expected) {
            if (actual.equalsIgnoreCase(e)) return List.of();
        }
        return List.of(CheckResult.of(Severity.ERROR,
                "mc.mismatch",
                "Unexpected Minecraft version",
                "Running Minecraft " + actual + ", expected one of: " + String.join(", ", expected) + "."));
    }
}
