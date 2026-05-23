package com.stromblex.vartapack.check;

import com.stromblex.vartapack.VartaPack;
import com.stromblex.vartapack.api.Platform;
import com.stromblex.vartapack.config.PackProfile;
import com.stromblex.vartapack.config.VartaConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CheckManager {

    private static final List<Check> CHECKS = List.of(
            new JavaCheck(),
            new RamCheck(),
            new MinecraftVersionCheck(),
            new LoaderCheck(),
            new RequiredModsCheck(),
            new BlockedModsCheck(),
            new RecommendedModsCheck(),
            new ExtraModsCheck()
    );

    public static int totalChecks() { return CHECKS.size(); }

    public List<CheckResult> runAll(Platform platform, VartaConfig config, PackProfile profile) {
        CheckContext ctx = CheckContext.create(platform, config, profile);
        List<CheckResult> results = new ArrayList<>();
        for (Check check : CHECKS) {
            try {
                List<CheckResult> r = check.run(ctx);
                if (r != null) results.addAll(r);
            } catch (Throwable t) {
                VartaPack.LOGGER.warn("Check '{}' failed with an exception. Continuing.", check.id(), t);
                results.add(new CheckResult(Severity.WARNING,
                        "check.failed." + check.id(),
                        "Internal check failure",
                        "Check '" + check.id() + "' failed: " + t.getClass().getSimpleName(),
                        String.valueOf(t.getMessage()),
                        false));
            }
        }
        // Sort: CRITICAL -> ERROR -> WARNING -> INFO (highest severity first)
        results.sort(Comparator.comparingInt((CheckResult r) -> r.severity().ordinal()).reversed());
        return List.copyOf(results);
    }
}
