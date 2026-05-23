package com.stromblex.vartapack.check;

import com.stromblex.vartapack.api.EnvironmentInfo;
import com.stromblex.vartapack.api.ModInfo;
import com.stromblex.vartapack.api.Platform;
import com.stromblex.vartapack.config.PackProfile;
import com.stromblex.vartapack.config.VartaConfig;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public record CheckContext(
        VartaConfig config,
        PackProfile profile,
        Platform platform,
        EnvironmentInfo environment,
        Map<String, ModInfo> installedMods
) {
    public static CheckContext create(Platform platform, VartaConfig config, PackProfile profile) {
        EnvironmentInfo env = EnvironmentInfo.capture(platform);
        Map<String, ModInfo> mods = platform.getInstalledMods().stream()
                .filter(m -> m.id() != null && !m.id().isBlank())
                .collect(Collectors.toMap(
                        m -> m.id().toLowerCase(Locale.ROOT),
                        m -> m,
                        (a, b) -> a));
        return new CheckContext(config, profile, platform, env, mods);
    }

    public boolean hasMod(String id) {
        if (id == null) return false;
        return installedMods.containsKey(id.toLowerCase(Locale.ROOT));
    }

    public ModInfo getMod(String id) {
        if (id == null) return null;
        return installedMods.get(id.toLowerCase(Locale.ROOT));
    }
}
