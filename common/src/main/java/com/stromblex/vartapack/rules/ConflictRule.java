package com.stromblex.vartapack.rules;

import com.stromblex.vartapack.check.Severity;

/**
 * A conflict rule that detects when two specific mods are both installed.
 */
public record ConflictRule(
        String id,
        String modA,
        String modB,
        Severity severity,
        String reason,
        String fix,
        String versionRangeA,
        String versionRangeB
) {
    public ConflictRule {
        if (id == null) id = "";
        if (modA == null) modA = "";
        if (modB == null) modB = "";
        if (severity == null) severity = Severity.ERROR;
        if (reason == null) reason = "";
        if (fix == null) fix = "";
        if (versionRangeA == null) versionRangeA = "";
        if (versionRangeB == null) versionRangeB = "";
    }
}
