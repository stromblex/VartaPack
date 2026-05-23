package com.stromblex.vartapack.check;

import java.util.List;
import java.util.Locale;

public final class LoaderCheck implements Check {
    @Override public String id() { return "loader"; }

    @Override
    public List<CheckResult> run(CheckContext ctx) {
        List<String> expected = ctx.profile().expectedLoaders();
        if (expected.isEmpty()) return List.of();
        String actual = ctx.environment().loaderName();
        if (actual == null || actual.isBlank()) {
            return List.of(CheckResult.of(Severity.WARNING,
                    "loader.unknown",
                    "Mod loader unknown",
                    "Could not detect the active mod loader."));
        }
        String actualLower = actual.toLowerCase(Locale.ROOT);
        for (String e : expected) {
            if (e != null && actualLower.equals(e.toLowerCase(Locale.ROOT))) return List.of();
        }
        return List.of(CheckResult.of(Severity.ERROR,
                "loader.mismatch",
                "Unexpected mod loader",
                "Running on " + actual + ", expected one of: " + String.join(", ", expected) + "."));
    }
}
