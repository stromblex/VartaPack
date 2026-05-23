package com.stromblex.vartapack.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Loader-agnostic abstraction over the host loader (Fabric / NeoForge).
 * Common code must never import loader classes; it uses this interface.
 */
public interface Platform {
    String getLoaderName();
    String getLoaderVersion();
    String getMinecraftVersion();
    Path getGameDirectory();
    List<ModInfo> getInstalledMods();
    boolean isModLoaded(String modId);
    Optional<ModInfo> getMod(String modId);
    boolean isClientEnvironment();
}
