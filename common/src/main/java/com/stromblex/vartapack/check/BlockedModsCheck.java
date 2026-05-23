package com.stromblex.vartapack.check;

import com.stromblex.vartapack.config.ModRule;

import java.util.ArrayList;
import java.util.List;

public final class BlockedModsCheck implements Check {
    @Override public String id() { return "blockedMods"; }

    @Override
    public List<CheckResult> run(CheckContext ctx) {
        List<CheckResult> out = new ArrayList<>();
        Severity sev = ctx.config().blockedModsSeverity();
        for (ModRule rule : ctx.profile().blockedMods()) {
            if (rule.id().isBlank()) continue;
            if (ctx.hasMod(rule.id())) {
                String base = "Mod '" + rule.name() + "' (" + rule.id() + ") is not allowed in this modpack.";
                String msg = rule.reason().isBlank() ? base : base + " Reason: " + rule.reason();
                out.add(CheckResult.of(sev,
                        "blocked.present." + rule.id(),
                        "Blocked mod installed: " + rule.name(),
                        msg));
            }
        }
        return out;
    }
}
