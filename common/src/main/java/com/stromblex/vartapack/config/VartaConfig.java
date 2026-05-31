package com.stromblex.vartapack.config;

import com.stromblex.vartapack.check.Severity;

public record VartaConfig(
        int schema,
        boolean enabled,
        boolean showToastOnStartup,
        boolean showScreenOnCriticalIssues,
        boolean allowContinueAnyway,
        boolean includeInstalledModsInReport,
        boolean includeExtraModsInReport,
        boolean redactUserHomePath,
        boolean redactUsername,
        boolean strictMode,
        Severity extraModsSeverity,
        Severity requiredModsSeverity,
        Severity blockedModsSeverity,
        Severity recommendedModsSeverity,
        boolean fixedGuiScale,
        int targetGuiScale
) {
    public static VartaConfig defaults() {
        return new VartaConfig(
                1,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                false,
                Severity.INFO,
                Severity.ERROR,
                Severity.ERROR,
                Severity.WARNING,
                true,
                2
        );
    }
}
