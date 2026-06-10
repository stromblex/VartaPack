package com.stromblex.vartapack.neoforge;

import com.stromblex.vartapack.api.ModInfo;
import com.stromblex.vartapack.api.Platform;
import net.minecraft.SharedConstants;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class NeoForgePlatform implements Platform {

    @Override public String getLoaderName() { return "NeoForge"; }

    @Override
    public String getLoaderVersion() {
        ModList list = ModList.get();
        if (list != null) {
            var c = list.getModContainerById("neoforge");
            if (c.isPresent()) return c.get().getModInfo().getVersion().toString();
        }
        try {
            FMLLoader loader = FMLLoader.getCurrentOrNull();
            if (loader != null) return loader.getVersionInfo().neoForgeVersion();
        }
        catch (Throwable t) { return "unknown"; }
        return "unknown";
    }

    @Override
    public String getMinecraftVersion() {
        try {
            FMLLoader loader = FMLLoader.getCurrentOrNull();
            if (loader != null) return loader.getVersionInfo().mcVersion();
        }
        catch (Throwable ignored) { }
        try {
            Object version = SharedConstants.getCurrentVersion();
            for (String method : List.of("id", "getName")) {
                try {
                    return String.valueOf(version.getClass().getMethod(method).invoke(version));
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        catch (Throwable t) { return "unknown"; }
        return "unknown";
    }

    @Override public Path getGameDirectory() { return FMLPaths.GAMEDIR.get(); }

    @Override
    public List<ModInfo> getInstalledMods() {
        List<ModInfo> out = new ArrayList<>();
        ModList list = ModList.get();
        if (list == null) return out;
        for (IModInfo info : list.getMods()) {
            out.add(new ModInfo(
                    info.getModId(),
                    info.getDisplayName(),
                    info.getVersion() == null ? "" : info.getVersion().toString(),
                    info.getDescription()));
        }
        return out;
    }

    @Override
    public boolean isModLoaded(String id) {
        ModList list = ModList.get();
        return list != null && list.isLoaded(id);
    }

    @Override
    public Optional<ModInfo> getMod(String id) {
        ModList list = ModList.get();
        if (list == null) return Optional.empty();
        return list.getModContainerById(id).map(c -> {
            IModInfo info = c.getModInfo();
            return new ModInfo(info.getModId(), info.getDisplayName(),
                    info.getVersion() == null ? "" : info.getVersion().toString(),
                    info.getDescription());
        });
    }

    @Override public boolean isClientEnvironment() { return FMLEnvironment.getDist().isClient(); }
}
