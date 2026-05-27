package com.stromblex.vartapack.rules;

/**
 * Types of rules that can appear in rules.json.
 */
public enum RuleType {
    /** Mod must not be present. Hard block. */
    BLOCKED_MOD,
    /** Mod should not be present but is not fatal. */
    SOFT_BLOCKED_MOD,
    /** Known conflict between specific mods. */
    MOD_CONFLICT,
    /** Mod is required to be installed. */
    REQUIRED_MOD,
    /** Mod is recommended but optional. */
    RECOMMENDED_MOD,
    /** Mod is allowed as an extra (no warning). */
    ALLOWED_EXTRA_MOD,
    /** A suspicious mod category (e.g. cheat mods). */
    SUSPICIOUS_MOD,
    /** Expected environment constraint. */
    ENVIRONMENT_RULE,
    /** File integrity rule. */
    FILE_RULE
}
