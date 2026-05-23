package com.stromblex.vartapack.check;

import com.stromblex.vartapack.api.ModInfo;
import com.stromblex.vartapack.config.ModRule;
import com.stromblex.vartapack.util.VersionUtil;

import java.util.ArrayList;
import java.util.List;

public final class RequiredModsCheck implements Check {
    @Override public String id() { return "requiredMods"; }

    @Override
    public List<CheckResult> run(CheckContext ctx) {
        List<CheckResult> out = new ArrayList<>();
        Severity sev = ctx.config().requiredModsSeverity();
        for (ModRule rule : ctx.profile().requiredMods()) {
            if (rule.id().isBlank()) continue;
            ModInfo installed = ctx.getMod(rule.id());
            if (installed == null) {
                out.add(CheckResult.of(sev,
                        "required.missing." + rule.id(),
                        "Missing required mod: " + rule.name(),
                        buildMessage("Required mod '" + rule.name() + "' (" + rule.id() + ") is not installed.",
                                rule.reason())));
                continue;
            }
            if (!rule.requiredVersion().isBlank()
                    && !VersionUtil.satisfies(installed.version(), rule.requiredVersion())) {
                out.add(CheckResult.of(sev,
                        "required.versionMismatch." + rule.id(),
                        "Wrong version of required mod: " + rule.name(),
                        "Installed " + installed.name() + " " + installed.version()
                                + ", required " + rule.requiredVersion() + "."));
            }
        }
        return out;
    }

    private static String buildMessage(String base, String reason) {
        return reason == null || reason.isBlank() ? base : base + " Reason: " + reason;
    }
}
