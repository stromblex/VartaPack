package com.stromblex.vartapack.check;

import com.stromblex.vartapack.api.EnvironmentInfo;
import com.stromblex.vartapack.api.ModInfo;
import com.stromblex.vartapack.config.ModRule;
import com.stromblex.vartapack.config.PackProfile;
import com.stromblex.vartapack.config.VartaConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChecksTest {
    @Test
    void extraModsIgnoreLoaderInternals() {
        CheckContext context = context(
                VartaConfig.defaults(),
                profile(List.of("sodium"), false),
                List.of(
                        new ModInfo("sodium", "Sodium", "0.5.11", ""),
                        new ModInfo("fabric-api-base", "Fabric API Base", "0.4.0", ""),
                        new ModInfo("journeymap", "JourneyMap", "1.0.0", ""))
        );

        List<CheckResult> results = new ExtraModsCheck().run(context);

        assertEquals(1, results.size());
        assertTrue(results.getFirst().technicalDetails().contains("journeymap"));
        assertFalse(results.getFirst().technicalDetails().contains("fabric-api-base"));
    }

    @Test
    void strictModeElevatesExtraMods() {
        VartaConfig config = new VartaConfig(
                1, true, true, true, false,
                true, true, true, true, true,
                Severity.INFO, Severity.ERROR, Severity.ERROR, Severity.WARNING,
                true, 2);
        CheckContext context = context(config, profile(List.of("sodium"), false),
                List.of(new ModInfo("journeymap", "JourneyMap", "1.0.0", "")));

        CheckResult result = new ExtraModsCheck().run(context).getFirst();

        assertEquals(Severity.ERROR, result.severity());
        assertTrue(result.visibleToPlayer());
    }

    @Test
    void requiredModsUseVersionRanges() {
        CheckContext ok = context(
                VartaConfig.defaults(),
                profile(List.of(), true),
                List.of(new ModInfo("sodium", "Sodium", "1.5.0", ""))
        );

        assertTrue(new RequiredModsCheck().run(ok).isEmpty());

        CheckContext mismatch = context(
                VartaConfig.defaults(),
                requiredProfile("[2.0,)"),
                List.of(new ModInfo("sodium", "Sodium", "1.5.0", ""))
        );

        assertEquals(1, new RequiredModsCheck().run(mismatch).size());
    }

    private static CheckContext context(VartaConfig config, PackProfile profile, List<ModInfo> mods) {
        Map<String, ModInfo> installed = mods.stream()
                .collect(Collectors.toMap(m -> m.id().toLowerCase(Locale.ROOT), m -> m));
        EnvironmentInfo env = new EnvironmentInfo(
                "1.21.1", "Fabric", "0.16.5", "21", 21,
                "Linux", "test", 8192, "/tmp/vartapack-test");
        return new CheckContext(config, profile, null, env, installed);
    }

    private static PackProfile profile(List<String> allowed, boolean includeRequired) {
        return includeRequired ? requiredProfile(">=1.4 <2.0") : new PackProfile(
                1, "test", "Test", "1.0.0", "", "",
                List.of("1.21.1"), List.of("fabric"), 21, 4096, 6144,
                List.of(), List.of(), List.of(), allowed);
    }

    private static PackProfile requiredProfile(String version) {
        return new PackProfile(
                1, "test", "Test", "1.0.0", "", "",
                List.of("1.21.1"), List.of("fabric"), 21, 4096, 6144,
                List.of(new ModRule("sodium", "Sodium", version, "renderer")),
                List.of(), List.of(), List.of());
    }
}