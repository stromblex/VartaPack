package com.stromblex.vartapack.fabric;

import com.stromblex.vartapack.api.ModInfo;
import com.stromblex.vartapack.api.Platform;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class FabricPlatform implements Platform {

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
        for (ModContainer c : FabricLoader.getInstance().getAllMods()) {
            ModMetadata m = c.getMetadata();
            out.add(new ModInfo(m.getId(), m.getName(), m.getVersion().getFriendlyString(),
                    m.getDescription()));
        }
        return out;
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
