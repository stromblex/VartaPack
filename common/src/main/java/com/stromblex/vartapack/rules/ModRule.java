package com.stromblex.vartapack.rules;

import com.stromblex.vartapack.check.Severity;

/**
 * A single rule entry from rules.json. Supports multiple rule types
 * for blocked mods, soft-blocked mods, required mods, etc.
 */
public record ModRule(
        String id,
        RuleType type,
        String modId,
        String displayName,
        Severity severity,
        String category,
        String reason,
        String fix,
        String versionRange,
        boolean blockContinue
) {
    public ModRule {
        if (id == null) id = "";
        if (type == null) type = RuleType.BLOCKED_MOD;
        if (modId == null) modId = "";
        if (displayName == null) displayName = modId;
        if (severity == null) severity = Severity.ERROR;
        if (category == null) category = "";
        if (reason == null) reason = "";
        if (fix == null) fix = "";
        if (versionRange == null) versionRange = "";
    }
}
