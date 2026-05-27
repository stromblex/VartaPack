package com.stromblex.vartapack.fabric;

import com.stromblex.vartapack.api.ModInfo;
import com.stromblex.vartapack.api.Platform;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FabricPlatform implements Platform {

    private static final Logger LOGGER = LoggerFactory.getLogger("VartaPack");

    @Override public String getLoaderName() { return "Fabric"; }

    @Override
    public String getLoaderVersion() {
        return FabricLoader.getInstance().getModContainer("fabricloader")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    @Override
    public String getMinecraftVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    @Override public Path getGameDirectory() { return FabricLoader.getInstance().getGameDir(); }

    @Override
    public List<ModInfo> getInstalledMods() {
        List<ModInfo> out = new ArrayList<>();
        Set<String> knownIds = new HashSet<>();

        // First, collect all properly loaded Fabric mods
        for (ModContainer c : FabricLoader.getInstance().getAllMods()) {
            ModMetadata m = c.getMetadata();
            out.add(new ModInfo(m.getId(), m.getName(), m.getVersion().getFriendlyString(),
                    m.getDescription()));
            knownIds.add(m.getId().toLowerCase(Locale.ROOT));
        }

        // Then scan the mods folder for JARs that Fabric didn't load (e.g. OptiFine, Forge mods)
        Path modsDir = getGameDirectory().resolve("mods");
        if (Files.isDirectory(modsDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir, "*.jar")) {
                for (Path jarPath : stream) {
                    ModInfo detected = detectUnrecognizedMod(jarPath, knownIds);
                    if (detected != null) {
                        out.add(detected);
                        knownIds.add(detected.id().toLowerCase(Locale.ROOT));
                    }
                }
            } catch (IOException e) {
                LOGGER.debug("VartaPack: failed to scan mods folder: {}", e.getMessage());
            }
        }

        return out;
    }

    /**
     * Tries to identify a JAR in the mods folder that wasn't loaded by Fabric.
     * Uses filename patterns and internal package scanning to determine the mod ID.
     */
    private ModInfo detectUnrecognizedMod(Path jarPath, Set<String> knownIds) {
        String filename = jarPath.getFileName().toString().toLowerCase(Locale.ROOT);

        // Known mod patterns: filename -> (id, displayName)
        record ModPattern(Pattern pattern, String id, String name) {}
        List<ModPattern> patterns = List.of(
                new ModPattern(Pattern.compile("optifine|optifabric", Pattern.CASE_INSENSITIVE),
                        "optifine", "OptiFine"),
                new ModPattern(Pattern.compile("performant", Pattern.CASE_INSENSITIVE),
                        "performant", "Performant"),
                new ModPattern(Pattern.compile("rubidium", Pattern.CASE_INSENSITIVE),
                        "rubidium", "Rubidium"),
                new ModPattern(Pattern.compile("oculus", Pattern.CASE_INSENSITIVE),
                        "oculus", "Oculus"),
                new ModPattern(Pattern.compile("better.?fps", Pattern.CASE_INSENSITIVE),
                        "betterfps", "BetterFPS"),
                new ModPattern(Pattern.compile("forge", Pattern.CASE_INSENSITIVE),
                        null, null) // Skip forge JARs without specific ID
        );

        for (ModPattern mp : patterns) {
            if (mp.pattern().matcher(filename).find()) {
                if (mp.id() == null) return null; // Explicitly skip
                if (knownIds.contains(mp.id())) return null; // Already loaded
                String version = extractVersionFromFilename(filename);
                return new ModInfo(mp.id(), mp.name(), version,
                        "Detected from JAR file: " + jarPath.getFileName());
            }
        }

        // For unknown JARs, try to read fabric.mod.json or detect by package structure
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            // If it has fabric.mod.json, Fabric should have loaded it — skip
            JarEntry fmj = jar.getJarEntry("fabric.mod.json");
            if (fmj != null) return null;

            // Check for known package indicators
            if (jar.getJarEntry("net/optifine/") != null ||
                    jar.getJarEntry("optifine/") != null) {
                if (!knownIds.contains("optifine")) {
                    String version = extractVersionFromFilename(filename);
                    return new ModInfo("optifine", "OptiFine", version,
                            "Detected from JAR file: " + jarPath.getFileName());
                }
            }
        } catch (IOException e) {
            // Can't open JAR — skip
        }

        return null;
    }

    private static String extractVersionFromFilename(String filename) {
        // Try to find version-like patterns: 1.2.3, HD_U_J1, etc.
        Matcher m = Pattern.compile("(\\d+[._]\\d+[._]?\\d*[a-zA-Z_]*)").matcher(filename);
        if (m.find()) return m.group(1);
        return "unknown";
    }

    @Override public boolean isModLoaded(String id) { return FabricLoader.getInstance().isModLoaded(id); }

    @Override
    public Optional<ModInfo> getMod(String id) {
        return FabricLoader.getInstance().getModContainer(id).map(c -> {
            ModMetadata m = c.getMetadata();
            return new ModInfo(m.getId(), m.getName(), m.getVersion().getFriendlyString(), m.getDescription());
        });
    }

    @Override
    public boolean isClientEnvironment() {
        return FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT;
    }
}
