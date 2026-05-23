package com.stromblex.vartapack.check;

import java.util.List;

public final class JavaCheck implements Check {
    @Override public String id() { return "java"; }

    @Override
    public List<CheckResult> run(CheckContext ctx) {
        int minMajor = ctx.profile().minimumJavaMajor();
        int actual = ctx.environment().javaMajor();
        if (actual < 0) {
            return List.of(CheckResult.of(Severity.WARNING,
                    "java.unknown",
                    "Java version could not be determined",
                    "Could not parse Java version string: " + ctx.environment().javaVersion()));
        }
        if (minMajor > 0 && actual < minMajor) {
            return List.of(CheckResult.of(Severity.ERROR,
                    "java.tooOld",
                    "Java version is too old",
                    "Detected Java " + actual + " (" + ctx.environment().javaVersion()
                            + "). This modpack requires Java " + minMajor + " or newer."));
        }
        return List.of();
    }
}
