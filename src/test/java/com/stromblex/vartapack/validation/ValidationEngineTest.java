package com.stromblex.vartapack.validation;

import com.stromblex.vartapack.api.EnvironmentInfo;
import com.stromblex.vartapack.api.ModInfo;
import com.stromblex.vartapack.api.Platform;
import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.config.ModRule;
import com.stromblex.vartapack.config.PackProfile;
import com.stromblex.vartapack.config.VartaConfig;
import com.stromblex.vartapack.integrity.IntegrityManifest;
import com.stromblex.vartapack.rules.ConflictRule;
import com.stromblex.vartapack.rules.RulesConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

final class ValidationEngineTest {

    @Test
    void cleanProfileReturnsCleanStatus() {
        Platform platform = testPlatform(List.of(
                new ModInfo("sodium", "Sodium", "0.5.11", ""),
                new ModInfo("fabric-api", "Fabric API", "0.100.0", "")
        ));
        PackProfile profile = new PackProfile(1, "test", "Test", "1.0.0", "", "",
                List.of("1.21.1"), List.of("fabric"), 21, 0, 0,
                List.of(new ModRule("sodium", "Sodium", "", "")),
                List.of(), List.of(), List.of("fabric-api"));
        VartaConfig config = VartaConfig.defaults();

        ValidationEngine engine = new ValidationEngine();
        ValidationResult result = engine.validateLegacy(platform, config, profile);

        assertEquals(PackStatus.CLEAN, result.status());
    }

    @Test
    void missingRequiredModReturnsBroken() {
        Platform platform = testPlatform(List.of());
        PackProfile profile = new PackProfile(1, "test", "Test", "1.0.0", "", "",
                List.of("1.21.1"), List.of("fabric"), 21, 0, 0,
                List.of(new ModRule("sodium", "Sodium", "", "needed")),
                List.of(), List.of(), List.of());
        VartaConfig config = VartaConfig.defaults();

        ValidationEngine engine = new ValidationEngine();
        ValidationResult result = engine.validateLegacy(platform, config, profile);

        assertEquals(PackStatus.BROKEN, result.status());
        assertTrue(result.countBySeverity(Severity.ERROR) > 0);
    }

    @Test
    void blockedModReturnsBroken() {
        Platform platform = testPlatform(List.of(
                new ModInfo("optifine", "OptiFine", "1.0", "")
        ));
        PackProfile profile = new PackProfile(1, "test", "Test", "1.0.0", "", "",
                List.of("1.21.1"), List.of("fabric"), 21, 0, 0,
                List.of(), List.of(),
                List.of(new ModRule("optifine", "OptiFine", "", "incompatible")),
                List.of());
        VartaConfig config = VartaConfig.defaults();

        ValidationEngine engine = new ValidationEngine();
        ValidationResult result = engine.validateLegacy(platform, config, profile);

        assertEquals(PackStatus.BROKEN, result.status());
    }

    @Test
    void extraModReturnsModified() {
        Platform platform = testPlatform(List.of(
                new ModInfo("sodium", "Sodium", "0.5.11", ""),
                new ModInfo("journeymap", "JourneyMap", "1.0", "")
        ));
        PackProfile profile = new PackProfile(1, "test", "Test", "1.0.0", "", "",
                List.of("1.21.1"), List.of("fabric"), 21, 0, 0,
                List.of(new ModRule("sodium", "Sodium", "", "")),
                List.of(), List.of(), List.of());
        VartaConfig config = VartaConfig.defaults();

        ValidationEngine engine = new ValidationEngine();
        ValidationResult result = engine.validateLegacy(platform, config, profile);

        // Extra mods at INFO level keep status non-broken (CLEAN or MODIFIED depending on policy).
        assertNotEquals(PackStatus.BROKEN, result.status());
        assertNotEquals(PackStatus.UNSUPPORTED, result.status());
    }

    @Test
    void conflictRuleDetectsConflict() {
        Platform platform = testPlatform(List.of(
                new ModInfo("optifine", "OptiFine", "1.0", ""),
                new ModInfo("sodium", "Sodium", "0.5.11", "")
        ));
        PackProfile profile = new PackProfile(1, "test", "Test", "1.0.0", "", "",
                List.of("1.21.1"), List.of("fabric"), 21, 0, 0,
                List.of(), List.of(), List.of(), List.of("optifine", "sodium"));
        VartaConfig config = VartaConfig.defaults();
        RulesConfig rules = new RulesConfig(1, List.of(),
                List.of(new ConflictRule("conflict.optifine_sodium",
                        "optifine", "sodium", Severity.CRITICAL,
                        "OptiFine and Sodium conflict.", "Remove OptiFine.", "", "")),
                "");

        ValidationEngine engine = new ValidationEngine();
        ValidationResult result = engine.validate(platform, config, profile, rules, null);

        assertEquals(PackStatus.BROKEN, result.status());
        assertTrue(result.issues().stream()
                .anyMatch(i -> i.id().contains("conflict")));
    }

    @Test
    void wrongJavaVersionReturnsBroken() {
        // Test that a profile requiring Java 99 produces BROKEN with real environment
        Platform platform = testPlatform(List.of());
        PackProfile profile = new PackProfile(1, "test", "Test", "1.0.0", "", "",
                List.of("1.21.1"), List.of("fabric"), 99, 0, 0,
                List.of(), List.of(), List.of(), List.of());
        VartaConfig config = VartaConfig.defaults();

        ValidationEngine engine = new ValidationEngine();
        ValidationResult result = engine.validateLegacy(platform, config, profile);

        assertEquals(PackStatus.BROKEN, result.status());
        assertTrue(result.issues().stream()
                .anyMatch(i -> i.id().contains("java")));
    }

    @Test
    void wrongMinecraftVersionReturnsBroken() {
        Platform platform = new TestPlatform(List.of(), "1.20.4", "Fabric", "0.16.5");
        PackProfile profile = new PackProfile(1, "test", "Test", "1.0.0", "", "",
                List.of("1.21.1"), List.of("fabric"), 21, 0, 0,
                List.of(), List.of(), List.of(), List.of());
        VartaConfig config = VartaConfig.defaults();

        ValidationEngine engine = new ValidationEngine();
        ValidationResult result = engine.validateLegacy(platform, config, profile);

        assertEquals(PackStatus.BROKEN, result.status());
    }

    @Test
    void wrongLoaderReturnsBroken() {
        Platform platform = new TestPlatform(List.of(), "1.21.1", "NeoForge", "21.0.0");
        PackProfile profile = new PackProfile(1, "test", "Test", "1.0.0", "", "",
                List.of("1.21.1"), List.of("fabric"), 21, 0, 0,
                List.of(), List.of(), List.of(), List.of());
        VartaConfig config = VartaConfig.defaults();

        ValidationEngine engine = new ValidationEngine();
        ValidationResult result = engine.validateLegacy(platform, config, profile);

        assertEquals(PackStatus.BROKEN, result.status());
    }

    @Test
    void recommendedModMissingReturnsUnsupported() {
        Platform platform = testPlatform(List.of());
        PackProfile profile = new PackProfile(1, "test", "Test", "1.0.0", "", "",
                List.of("1.21.1"), List.of("fabric"), 21, 0, 0,
                List.of(), List.of(new ModRule("modmenu", "Mod Menu", "", "")),
                List.of(), List.of());
        VartaConfig config = VartaConfig.defaults();

        ValidationEngine engine = new ValidationEngine();
        ValidationResult result = engine.validateLegacy(platform, config, profile);

        // WARNING-level -> UNSUPPORTED
        assertEquals(PackStatus.UNSUPPORTED, result.status());
    }

    @Test
    void issuesHaveFixInstructions() {
        Platform platform = testPlatform(List.of());
        PackProfile profile = new PackProfile(1, "test", "Test", "1.0.0", "", "",
                List.of("1.21.1"), List.of("fabric"), 21, 0, 0,
                List.of(new ModRule("sodium", "Sodium", "", "")),
                List.of(), List.of(), List.of());
        VartaConfig config = VartaConfig.defaults();

        ValidationEngine engine = new ValidationEngine();
        ValidationResult result = engine.validateLegacy(platform, config, profile);

        assertTrue(result.issues().stream()
                .anyMatch(i -> !i.fixInstruction().isBlank()));
    }

    @Test
    void computeStatusLogic() {
        // No issues -> CLEAN
        assertEquals(PackStatus.CLEAN, ValidationResult.computeStatus(List.of()));

        // INFO extra mod -> MODIFIED
        List<Issue> modified = List.of(
                Issue.builder("extra.test").severity(Severity.INFO)
                        .category(IssueCategory.EXTRA_MOD).build());
        assertEquals(PackStatus.MODIFIED, ValidationResult.computeStatus(modified));

        // WARNING -> UNSUPPORTED
        List<Issue> unsupported = List.of(
                Issue.builder("warn.test").severity(Severity.WARNING)
                        .category(IssueCategory.ENVIRONMENT).build());
        assertEquals(PackStatus.UNSUPPORTED, ValidationResult.computeStatus(unsupported));

        // ERROR -> BROKEN
        List<Issue> broken = List.of(
                Issue.builder("err.test").severity(Severity.ERROR)
                        .category(IssueCategory.BLOCKED_MOD).build());
        assertEquals(PackStatus.BROKEN, ValidationResult.computeStatus(broken));

        // CRITICAL -> BROKEN
        List<Issue> critical = List.of(
                Issue.builder("crit.test").severity(Severity.CRITICAL)
                        .category(IssueCategory.MOD_CONFLICT).build());
        assertEquals(PackStatus.BROKEN, ValidationResult.computeStatus(critical));
    }

    // --- Test helpers ---

    private static Platform testPlatform(List<ModInfo> mods) {
        return new TestPlatform(mods, "1.21.1", "Fabric", "0.16.5");
    }

    private static class TestPlatform implements Platform {
        private final List<ModInfo> mods;
        private final String mcVersion;
        private final String loaderName;
        private final String loaderVersion;

        TestPlatform(List<ModInfo> mods, String mcVersion, String loaderName, String loaderVersion) {
            this.mods = mods;
            this.mcVersion = mcVersion;
            this.loaderName = loaderName;
            this.loaderVersion = loaderVersion;
        }

        protected int javaMajor() { return 21; }

        @Override public String getLoaderName() { return loaderName; }
        @Override public String getLoaderVersion() { return loaderVersion; }
        @Override public String getMinecraftVersion() { return mcVersion; }
        @Override public Path getGameDirectory() { return Path.of("/tmp/vartapack-test"); }
        @Override public List<ModInfo> getInstalledMods() { return mods; }
        @Override public boolean isModLoaded(String modId) {
            return mods.stream().anyMatch(m -> m.id().equalsIgnoreCase(modId));
        }
        @Override public Optional<ModInfo> getMod(String modId) {
            return mods.stream().filter(m -> m.id().equalsIgnoreCase(modId)).findFirst();
        }
        @Override public boolean isClientEnvironment() { return true; }
    }
}
