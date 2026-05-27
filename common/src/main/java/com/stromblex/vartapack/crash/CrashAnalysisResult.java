package com.stromblex.vartapack.crash;

import java.util.List;

/**
 * Result of analyzing crash reports and log files.
 */
public record CrashAnalysisResult(
        List<CrashFinding> findings,
        boolean hasCrashReports,
        boolean hasLatestLog,
        String summary
) {
    public CrashAnalysisResult {
        if (findings == null) findings = List.of();
        if (summary == null) summary = "";
    }

    public static CrashAnalysisResult empty() {
        return new CrashAnalysisResult(List.of(), false, false, "No crash data analyzed.");
    }

    public boolean hasFindings() {
        return !findings.isEmpty();
    }
}
