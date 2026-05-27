package com.stromblex.vartapack.rules;

import java.util.List;

/**
 * Parsed rules.json configuration. Contains advanced mod rules,
 * conflict definitions, and environment constraints.
 */
public record RulesConfig(
        int schema,
        List<ModRule> rules,
        List<ConflictRule> conflicts,
        String supportPolicyText
) {
    public RulesConfig {
        if (rules == null) rules = List.of();
        if (conflicts == null) conflicts = List.of();
        if (supportPolicyText == null) supportPolicyText = "";
    }

    public static RulesConfig empty() {
        return new RulesConfig(1, List.of(), List.of(), "");
    }
}
