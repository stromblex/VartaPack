package com.stromblex.vartapack.check;

import java.util.List;

/**
 * Aggregates raw environment data into a single INFO entry that always
 * appears in reports, so authors see what the player runs even without issues.
 */
public final class EnvironmentCheck implements Check {
    @Override public String id() { return "environment"; }

    @Override
    public List<CheckResult> run(CheckContext ctx) {
        String details = "MC=" + ctx.environment().minecraftVersion()
                + ", loader=" + ctx.environment().loaderName()
                + " " + ctx.environment().loaderVersion()
                + ", java=" + ctx.environment().javaVersion()
                + ", ram=" + ctx.environment().maxMemoryMb() + " MB";
        return List.of(new CheckResult(
                Severity.INFO,
                "environment.summary",
                "Environment snapshot",
                details,
                "",
                false));
    }
}
