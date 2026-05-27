package com.stromblex.vartapack.rules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stromblex.vartapack.VartaPack;
import com.stromblex.vartapack.check.Severity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and parses rules.json from the VartaPack config directory.
 */
public final class RulesLoader {
    private static final String RULES_FILE = "rules.json";

    private RulesLoader() {}

    /**
     * Load rules from the config directory. Returns empty config if file does not exist.
     */
    public static RulesConfig load(Path configDir) {
        Path path = configDir.resolve(RULES_FILE);
        if (!Files.exists(path)) {
            return RulesConfig.empty();
        }
        try {
            String text = Files.readString(path);
            JsonObject obj = JsonParser.parseString(text).getAsJsonObject();
            return parseRulesConfig(obj);
        } catch (Exception e) {
            VartaPack.LOGGER.warn("Failed to parse rules.json: {}. Using empty rules.", e.getMessage());
            return RulesConfig.empty();
        }
    }

    private static RulesConfig parseRulesConfig(JsonObject obj) {
        int schema = getInt(obj, "schema", 1);
        String supportPolicy = getString(obj, "supportPolicyText", "");
        List<ModRule> rules = new ArrayList<>();
        List<ConflictRule> conflicts = new ArrayList<>();

        if (obj.has("rules") && obj.get("rules").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("rules")) {
                if (!e.isJsonObject()) continue;
                JsonObject ro = e.getAsJsonObject();
                String type = getString(ro, "type", "BLOCKED_MOD");
                if ("MOD_CONFLICT".equals(type)) {
                    conflicts.add(parseConflictRule(ro));
                } else {
                    rules.add(parseModRule(ro));
                }
            }
        }

        if (obj.has("conflicts") && obj.get("conflicts").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("conflicts")) {
                if (!e.isJsonObject()) continue;
                conflicts.add(parseConflictRule(e.getAsJsonObject()));
            }
        }

        return new RulesConfig(schema, rules, conflicts, supportPolicy);
    }

    private static ModRule parseModRule(JsonObject o) {
        return new ModRule(
                getString(o, "id", ""),
                parseRuleType(getString(o, "type", "BLOCKED_MOD")),
                getString(o, "modId", ""),
                getString(o, "displayName", getString(o, "modId", "")),
                parseSeverity(getString(o, "severity", "ERROR")),
                getString(o, "category", ""),
                getString(o, "reason", ""),
                getString(o, "fix", ""),
                getString(o, "versionRange", ""),
                getBool(o, "blockContinue", false)
        );
    }

    private static ConflictRule parseConflictRule(JsonObject o) {
        return new ConflictRule(
                getString(o, "id", ""),
                getString(o, "modA", ""),
                getString(o, "modB", ""),
                parseSeverity(getString(o, "severity", "ERROR")),
                getString(o, "reason", ""),
                getString(o, "fix", ""),
                getString(o, "versionRangeA", ""),
                getString(o, "versionRangeB", "")
        );
    }

    private static RuleType parseRuleType(String type) {
        try {
            return RuleType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RuleType.BLOCKED_MOD;
        }
    }

    private static Severity parseSeverity(String s) {
        try {
            return Severity.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Severity.ERROR;
        }
    }

    private static String getString(JsonObject o, String key, String def) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : def;
    }

    private static int getInt(JsonObject o, String key, int def) {
        try { return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsInt() : def; }
        catch (Exception e) { return def; }
    }

    private static boolean getBool(JsonObject o, String key, boolean def) {
        try { return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsBoolean() : def; }
        catch (Exception e) { return def; }
    }
}
