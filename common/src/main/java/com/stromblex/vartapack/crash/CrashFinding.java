package com.stromblex.vartapack.crash;

import com.stromblex.vartapack.check.Severity;

/**
 * A single finding from crash/log analysis.
 */
public record CrashFinding(
        String patternId,
        String title,
        Severity severity,
        float confidence,
        String explanation,
        String suggestedFix,
        String matchedExcerpt,
        String affectedMod
) {
    public CrashFinding {
        if (patternId == null) patternId = "";
        if (title == null) title = "";
        if (severity == null) severity = Severity.WARNING;
        if (confidence < 0) confidence = 0;
        if (confidence > 1) confidence = 1;
        if (explanation == null) explanation = "";
        if (suggestedFix == null) suggestedFix = "";
        if (matchedExcerpt == null) matchedExcerpt = "";
        if (affectedMod == null) affectedMod = "";
    }

    public String confidenceLabel() {
        if (confidence >= 0.9f) return "High confidence";
        if (confidence >= 0.6f) return "Likely cause";
        if (confidence >= 0.3f) return "Possible cause";
        return "Detected pattern";
    }
}
