package com.stromblex.vartapack;

import com.stromblex.vartapack.api.Platform;
import com.stromblex.vartapack.check.CheckManager;
import com.stromblex.vartapack.check.CheckResult;
import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.config.ConfigManager;
import com.stromblex.vartapack.config.PackProfile;
import com.stromblex.vartapack.config.VartaConfig;
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
    public static final String MOD_VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static Platform platform;
    private static VartaConfig config = VartaConfig.defaults();
    private static PackProfile profile = PackProfile.defaults();
    private static List<CheckResult> lastResults = Collections.emptyList();
    private static boolean initialized = false;
    private static boolean screenShownThisSession = false;

    private VartaPack() {}

    public static void init(Platform platform) {
        if (initialized) return;
        VartaPack.platform = platform;
        initialized = true;
        try {
            ConfigManager mgr = new ConfigManager(platform.getGameDirectory());
            config = mgr.loadVartaConfig();
            profile = mgr.loadPackProfile();
            if (!config.enabled()) {
                LOGGER.info("VartaPack is disabled via config; skipping checks.");
                lastResults = Collections.emptyList();
                return;
            }
            lastResults = new CheckManager().runAll(platform, config, profile);
            LOGGER.info("VartaPack ran {} checks, {} issue(s) reported.",
                    CheckManager.totalChecks(), lastResults.size());
        } catch (Throwable t) {
            LOGGER.error("VartaPack failed to initialize cleanly. Continuing without crashing the game.", t);
            lastResults = Collections.emptyList();
        }
    }

    public static Platform platform() { return platform; }
    public static VartaConfig config() { return config; }
    public static PackProfile profile() { return profile; }
    public static List<CheckResult> lastResults() { return lastResults; }

    public static boolean hasIssuesAtLeast(Severity min) {
        for (CheckResult r : lastResults) {
            if (r.severity().ordinal() >= min.ordinal()) return true;
        }
        return false;
    }

    public static boolean isScreenShownThisSession() { return screenShownThisSession; }
    public static void markScreenShown() { screenShownThisSession = true; }
}
