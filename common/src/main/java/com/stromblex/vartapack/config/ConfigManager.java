package com.stromblex.vartapack.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stromblex.vartapack.VartaPack;
import com.stromblex.vartapack.check.Severity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and creates the local VartaPack config + pack profile JSON files.
 *
 * <p>Files live under {@code <gameDir>/config/vartapack/}:
 * <ul>
 *   <li>{@code vartapack.json} — mod behaviour</li>
 *   <li>{@code profile.json} — modpack-author-defined rules</li>
 * </ul>
 *
 * Corrupt files are renamed to {@code *.broken.<timestamp>} and replaced
 * with defaults; the game must not crash.
 */
public final class ConfigManager {
    private static final String CONFIG_FILE = "vartapack.json";
    private static final String LEGACY_CONFIG_FILE = "vartaconfig.json";
    private static final String PROFILE_FILE = "profile.json";

    private final Path configDir;

    public ConfigManager(Path gameDir) {
        this.configDir = gameDir.resolve("config").resolve("vartapack");
    }

    public Path configDir() { return configDir; }
    public Path vartaConfigPath() { return configDir.resolve(CONFIG_FILE); }
    public Path legacyVartaConfigPath() { return configDir.resolve(LEGACY_CONFIG_FILE); }
    public Path packProfilePath() { return configDir.resolve(PROFILE_FILE); }

    public VartaConfig loadVartaConfig() {
        ensureDir();
        Path path = vartaConfigPath();
        migrateLegacyConfigIfNeeded(path);
        if (!Files.exists(path)) {
            VartaConfig defaults = VartaConfig.defaults();
            writeVartaConfig(path, defaults);
            VartaPack.LOGGER.info("Created default VartaPack config at {}", path);
            return defaults;
        }
        try {
            String text = Files.readString(path);
            JsonObject obj = JsonParser.parseString(text).getAsJsonObject();
            return parseVartaConfig(obj);
        } catch (Exception e) {
            VartaPack.LOGGER.warn("Failed to read {}: {}. Backing up and regenerating defaults.",
                    path, e.getMessage());
            backupBroken(path);
            VartaConfig defaults = VartaConfig.defaults();
            writeVartaConfig(path, defaults);
            return defaults;
        }
    }

    public void saveVartaConfig(VartaConfig config) {
        ensureDir();
        writeVartaConfig(vartaConfigPath(), config);
    }

    public void savePackProfile(PackProfile profile) {
        ensureDir();
        writePackProfile(packProfilePath(), profile);
    }

    public PackProfile loadPackProfile() {
        ensureDir();
        Path path = packProfilePath();
        if (!Files.exists(path)) {
            PackProfile defaults = PackProfile.defaults();
            writePackProfile(path, defaults);
            VartaPack.LOGGER.info("Created default pack profile at {}", path);
            return defaults;
        }
        try {
            String text = Files.readString(path);
            JsonObject obj = JsonParser.parseString(text).getAsJsonObject();
            return parsePackProfile(obj);
        } catch (Exception e) {
            VartaPack.LOGGER.warn("Failed to read {}: {}. Backing up and regenerating defaults.",
                    path, e.getMessage());
            backupBroken(path);
            PackProfile defaults = PackProfile.defaults();
            writePackProfile(path, defaults);
            return defaults;
        }
    }


    private static VartaConfig parseVartaConfig(JsonObject o) {
        VartaConfig d = VartaConfig.defaults();
        return new VartaConfig(
                getInt(o, "schema", d.schema()),
                getBool(o, "enabled", d.enabled()),
                getBool(o, "showToastOnStartup", d.showToastOnStartup()),
                getBool(o, "showScreenOnCriticalIssues", d.showScreenOnCriticalIssues()),
                getBool(o, "allowContinueAnyway", d.allowContinueAnyway()),
                getBool(o, "includeInstalledModsInReport", d.includeInstalledModsInReport()),
                getBool(o, "includeExtraModsInReport", d.includeExtraModsInReport()),
                getBool(o, "redactUserHomePath", d.redactUserHomePath()),
                getBool(o, "redactUsername", d.redactUsername()),
                getBool(o, "strictMode", d.strictMode()),
                parseSeverity(getString(o, "extraModsSeverity", d.extraModsSeverity().name()), d.extraModsSeverity()),
                parseSeverity(getString(o, "requiredModsSeverity", d.requiredModsSeverity().name()), d.requiredModsSeverity()),
                parseSeverity(getString(o, "blockedModsSeverity", d.blockedModsSeverity().name()), d.blockedModsSeverity()),
                parseSeverity(getString(o, "recommendedModsSeverity", d.recommendedModsSeverity().name()), d.recommendedModsSeverity())
        );
    }

    private static PackProfile parsePackProfile(JsonObject o) {
        PackProfile d = PackProfile.defaults();
        return new PackProfile(
                getInt(o, "schema", d.schema()),
                getString(o, "packId", d.packId()),
                getString(o, "packName", d.packName()),
                getString(o, "profileVersion", d.profileVersion()),
                getString(o, "supportUrl", d.supportUrl()),
                getString(o, "homepageUrl", d.homepageUrl()),
                getStringList(o, "expectedMinecraftVersions"),
                getStringList(o, "expectedLoaders"),
                getInt(o, "minimumJavaMajor", d.minimumJavaMajor()),
                getLong(o, "minimumRamMb", d.minimumRamMb()),
                getLong(o, "recommendedRamMb", d.recommendedRamMb()),
                getModRuleList(o, "requiredMods"),
                getModRuleList(o, "recommendedMods"),
                getModRuleList(o, "blockedMods"),
                getStringList(o, "allowedExtraMods")
        );
    }

    private static Severity parseSeverity(String s, Severity fallback) {
        if (s == null) return fallback;
        try { return Severity.valueOf(s.trim().toUpperCase()); }
        catch (Exception e) { return fallback; }
    }

    private static String getString(JsonObject o, String key, String def) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : def;
    }

    private static int getInt(JsonObject o, String key, int def) {
        try { return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsInt() : def; }
        catch (Exception e) { return def; }
    }

    private static long getLong(JsonObject o, String key, long def) {
        try { return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsLong() : def; }
        catch (Exception e) { return def; }
    }

    private static boolean getBool(JsonObject o, String key, boolean def) {
        try { return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsBoolean() : def; }
        catch (Exception e) { return def; }
    }

    private static List<String> getStringList(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        if (o.has(key) && o.get(key).isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray(key)) {
                if (e.isJsonPrimitive()) out.add(e.getAsString());
            }
        }
        return out;
    }

    private static List<ModRule> getModRuleList(JsonObject o, String key) {
        List<ModRule> out = new ArrayList<>();
        if (!o.has(key) || !o.get(key).isJsonArray()) return out;
        for (JsonElement e : o.getAsJsonArray(key)) {
            if (!e.isJsonObject()) continue;
            JsonObject ro = e.getAsJsonObject();
            out.add(new ModRule(
                    getString(ro, "id", ""),
                    getString(ro, "name", ""),
                    getString(ro, "requiredVersion", ""),
                    getString(ro, "reason", "")
            ));
        }
        return out;
    }


    private void writeVartaConfig(Path path, VartaConfig c) {
        JsonObject o = new JsonObject();
        o.addProperty("schema", c.schema());
        o.addProperty("enabled", c.enabled());
        o.addProperty("showToastOnStartup", c.showToastOnStartup());
        o.addProperty("showScreenOnCriticalIssues", c.showScreenOnCriticalIssues());
        o.addProperty("allowContinueAnyway", c.allowContinueAnyway());
        o.addProperty("includeInstalledModsInReport", c.includeInstalledModsInReport());
        o.addProperty("includeExtraModsInReport", c.includeExtraModsInReport());
        o.addProperty("redactUserHomePath", c.redactUserHomePath());
        o.addProperty("redactUsername", c.redactUsername());
        o.addProperty("strictMode", c.strictMode());
        o.addProperty("extraModsSeverity", c.extraModsSeverity().name());
        o.addProperty("requiredModsSeverity", c.requiredModsSeverity().name());
        o.addProperty("blockedModsSeverity", c.blockedModsSeverity().name());
        o.addProperty("recommendedModsSeverity", c.recommendedModsSeverity().name());
        writeJson(path, o);
    }

    private void writePackProfile(Path path, PackProfile p) {
        JsonObject o = new JsonObject();
        o.addProperty("schema", p.schema());
        o.addProperty("packId", p.packId());
        o.addProperty("packName", p.packName());
        o.addProperty("profileVersion", p.profileVersion());
        o.addProperty("supportUrl", p.supportUrl());
        o.addProperty("homepageUrl", p.homepageUrl());
        o.add("expectedMinecraftVersions", toStringArray(p.expectedMinecraftVersions()));
        o.add("expectedLoaders", toStringArray(p.expectedLoaders()));
        o.addProperty("minimumJavaMajor", p.minimumJavaMajor());
        o.addProperty("minimumRamMb", p.minimumRamMb());
        o.addProperty("recommendedRamMb", p.recommendedRamMb());
        o.add("requiredMods", toRulesArray(p.requiredMods(), true));
        o.add("recommendedMods", toRulesArray(p.recommendedMods(), false));
        o.add("blockedMods", toRulesArray(p.blockedMods(), false));
        o.add("allowedExtraMods", toStringArray(p.allowedExtraMods()));
        writeJson(path, o);
    }

    private static JsonArray toStringArray(List<String> list) {
        JsonArray a = new JsonArray();
        for (String s : list) a.add(s);
        return a;
    }

    private static JsonArray toRulesArray(List<ModRule> list, boolean includeVersionField) {
        JsonArray a = new JsonArray();
        for (ModRule r : list) {
            JsonObject o = new JsonObject();
            o.addProperty("id", r.id());
            o.addProperty("name", r.name());
            if (includeVersionField) o.addProperty("requiredVersion", r.requiredVersion());
            o.addProperty("reason", r.reason());
            a.add(o);
        }
        return a;
    }

    private void writeJson(Path path, JsonObject o) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, JsonUtil.GSON.toJson(o));
        } catch (IOException e) {
            VartaPack.LOGGER.error("Failed to write config file {}", path, e);
        }
    }

    private void ensureDir() {
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            VartaPack.LOGGER.error("Failed to create config dir {}", configDir, e);
        }
    }

    private void migrateLegacyConfigIfNeeded(Path target) {
        Path legacy = legacyVartaConfigPath();
        if (Files.exists(target) || !Files.exists(legacy)) return;
        try {
            Files.move(legacy, target, StandardCopyOption.REPLACE_EXISTING);
            VartaPack.LOGGER.info("Migrated legacy VartaPack config {} to {}", legacy, target);
        } catch (IOException e) {
            VartaPack.LOGGER.warn("Could not migrate legacy VartaPack config {} to {}: {}",
                    legacy, target, e.getMessage());
        }
    }

    private static void backupBroken(Path path) {
        try {
            Path backup = path.resolveSibling(path.getFileName() + ".broken." + Instant.now().toEpochMilli());
            Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
            VartaPack.LOGGER.warn("Backed up broken config to {}", backup);
        } catch (IOException ex) {
            VartaPack.LOGGER.error("Could not back up broken config {}", path, ex);
        }
    }
}
