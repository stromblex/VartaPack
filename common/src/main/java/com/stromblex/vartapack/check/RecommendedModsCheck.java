package com.stromblex.vartapack.check;

import com.stromblex.vartapack.config.ModRule;

import java.util.ArrayList;
import java.util.List;

public final class RecommendedModsCheck implements Check {
    @Override public String id() { return "recommendedMods"; }

    @Override
    public List<CheckResult> run(CheckContext ctx) {
        List<CheckResult> out = new ArrayList<>();
        Severity sev = ctx.config().recommendedModsSeverity();
        for (ModRule rule : ctx.profile().recommendedMods()) {
            if (rule.id().isBlank()) continue;
            if (!ctx.hasMod(rule.id())) {
                String base = "Recommended mod '" + rule.name() + "' (" + rule.id() + ") is not installed.";
                String msg = rule.reason().isBlank() ? base : base + " " + rule.reason();
                out.add(CheckResult.of(sev,
                        "recommended.missing." + rule.id(),
                        "Recommended mod missing: " + rule.name(),
                        msg));
            }
        }
        return out;
    }
}
