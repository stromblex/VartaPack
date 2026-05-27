package com.stromblex.vartapack.integrity;

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
 * Loads the integrity manifest from config/vartapack/integrity.json.
 */
public final class IntegrityLoader {
    private static final String INTEGRITY_FILE = "integrity.json";

    private IntegrityLoader() {}

    public static IntegrityManifest load(Path configDir) {
        Path path = configDir.resolve(INTEGRITY_FILE);
        if (!Files.exists(path)) {
            return IntegrityManifest.empty();
        }
        try {
            String text = Files.readString(path);
            JsonObject obj = JsonParser.parseString(text).getAsJsonObject();
            return parse(obj);
        } catch (Exception e) {
            VartaPack.LOGGER.warn("Failed to parse integrity.json: {}. Skipping integrity checks.", e.getMessage());
            return IntegrityManifest.empty();
        }
    }

    private static IntegrityManifest parse(JsonObject obj) {
        int schema = getInt(obj, "schema", 1);
        List<IntegrityManifest.FileEntry> files = new ArrayList<>();

        if (obj.has("files") && obj.get("files").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("files")) {
                if (!e.isJsonObject()) continue;
                JsonObject fo = e.getAsJsonObject();
                files.add(new IntegrityManifest.FileEntry(
                        getString(fo, "path", ""),
                        getString(fo, "type", "FILE"),
                        getString(fo, "sha256", ""),
                        getBool(fo, "required", false),
                        parseSeverity(getString(fo, "severityIfMissing", "WARNING")),
                        parseSeverity(getString(fo, "severityIfChanged", "INFO")),
                        getString(fo, "displayName", getString(fo, "path", "")),
                        getString(fo, "reason", ""),
                        getString(fo, "fix", "")
                ));
            }
        }

        return new IntegrityManifest(schema, files);
    }

    private static Severity parseSeverity(String s) {
        try { return Severity.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return Severity.INFO; }
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
