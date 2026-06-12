package com.stromblex.vartapack;

import com.stromblex.vartapack.api.Platform;
import com.stromblex.vartapack.check.CheckManager;
import com.stromblex.vartapack.check.CheckResult;
import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.config.ConfigManager;
import com.stromblex.vartapack.config.PackProfile;
import com.stromblex.vartapack.config.VartaConfig;
import com.stromblex.vartapack.crash.CrashAnalysisResult;
import com.stromblex.vartapack.crash.CrashAnalyzer;
import com.stromblex.vartapack.integrity.IntegrityLoader;
import com.stromblex.vartapack.integrity.IntegrityManifest;
import com.stromblex.vartapack.rules.RulesConfig;
import com.stromblex.vartapack.rules.RulesLoader;
import com.stromblex.vartapack.validation.ValidationEngine;
import com.stromblex.vartapack.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Loader-agnostic core of VartaPack. Loader modules call {@link #init(Platform)}
 * after their platform implementation is ready.
 *
 * <p>VartaPack is a support toolkit only:
 * <ul>
 *   <li>No telemetry.</li>
 *   <li>No HTTP requests.</li>
 *   <li>No modpack update checking — for that, use the separate
 *       <a href="https://github.com/stromblex/PackPing">PackPing</a> mod.</li>
 * </ul>
 */
public final class VartaPack {
    public static final String MOD_ID = "vartapack";
    public static final String MOD_NAME = "VartaPack";
    public static final String MOD_VERSION = resolveModVersion();
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static volatile Platform platform;
    private static volatile VartaConfig config = VartaConfig.defaults();
    private static volatile PackProfile profile = PackProfile.defaults();
    private static volatile RulesConfig rulesConfig = RulesConfig.empty();
    private static volatile IntegrityManifest integrityManifest = IntegrityManifest.empty();
    private static volatile List<CheckResult> lastResults = Collections.emptyList();
    private static volatile ValidationResult lastValidation = null;
    private static volatile CrashAnalysisResult lastCrashAnalysis = null;
    private static volatile boolean initialized = false;
    private static volatile boolean screenShownThisSession = false;

    private VartaPack() {}

    public static synchronized void init(Platform platform) {
        if (initialized) return;
        VartaPack.platform = platform;
        initialized = true;
        reload();
    }

    public static synchronized void reload() {
        if (platform == null) return;
        try {
            ConfigManager mgr = new ConfigManager(platform.getGameDirectory());
            config = mgr.loadVartaConfig();
            profile = mgr.loadPackProfile();
            if (!config.enabled()) {
                LOGGER.info("VartaPack is disabled via config; skipping checks.");
                lastResults = Collections.emptyList();
                lastValidation = null;
                lastCrashAnalysis = null;
                return;
            }

            // Load advanced rules and integrity manifest
            rulesConfig = RulesLoader.load(mgr.configDir());
            integrityManifest = IntegrityLoader.load(mgr.configDir());

            // Run validation engine
            ValidationEngine engine = new ValidationEngine();
            lastValidation = engine.validate(platform, config, profile, rulesConfig, integrityManifest);
            lastResults = lastValidation.legacyResults();

            // Run crash analysis
            try {
                CrashAnalyzer crashAnalyzer = new CrashAnalyzer();
                lastCrashAnalysis = crashAnalyzer.analyze(platform.getGameDirectory());
            } catch (Throwable t) {
                LOGGER.debug("Crash analysis failed: {}", t.getMessage());
                lastCrashAnalysis = CrashAnalysisResult.empty();
            }

            LOGGER.info("VartaPack validation complete. Status: {}. {} issue(s).",
                    lastValidation.status().displayName(), lastValidation.issues().size());
        } catch (Throwable t) {
            LOGGER.error("VartaPack failed to initialize cleanly. Continuing without crashing the game.", t);
            lastResults = Collections.emptyList();
            lastValidation = null;
            lastCrashAnalysis = null;
        }
    }

    public static Platform platform() { return platform; }
    public static VartaConfig config() { return config; }
    public static PackProfile profile() { return profile; }
    public static RulesConfig rulesConfig() { return rulesConfig; }
    public static IntegrityManifest integrityManifest() { return integrityManifest; }
    public static List<CheckResult> lastResults() { return lastResults; }
    public static ValidationResult lastValidation() { return lastValidation; }
    public static CrashAnalysisResult lastCrashAnalysis() { return lastCrashAnalysis; }

    public static boolean hasIssuesAtLeast(Severity min) {
        ValidationResult validation = lastValidation;
        if (validation != null) {
            return validation.highestSeverity().ordinal() >= min.ordinal();
        }
        for (CheckResult r : lastResults) {
            if (r.severity().ordinal() >= min.ordinal()) return true;
        }
        return false;
    }

    public static boolean shouldBlockContinue() {
        if (!config.enabled() || !hasIssuesAtLeast(Severity.ERROR)) return false;
        ValidationResult validation = lastValidation;
        if (validation != null && validation.hasBlockingIssues()) return true;
        return config.strictMode() || !config.allowContinueAnyway();
    }

    public static boolean isScreenShownThisSession() { return screenShownThisSession; }
    public static void markScreenShown() { screenShownThisSession = true; }

    private static String resolveModVersion() {
        Package pkg = VartaPack.class.getPackage();
        String implementationVersion = pkg == null ? null : pkg.getImplementationVersion();
        if (implementationVersion != null && !implementationVersion.isBlank()) return implementationVersion;
        String propertyVersion = System.getProperty("vartapack.version", "");
        return propertyVersion.isBlank() ? "dev" : propertyVersion;
    }
}
