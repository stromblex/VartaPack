package com.stromblex.vartapack.crash;

import com.stromblex.vartapack.check.Severity;

/**
 * A pattern that the crash analyzer looks for in log/crash files.
 */
public record CrashPattern(
        String id,
        String title,
        Severity severity,
        String regex,
        String explanation,
        String suggestedFix,
        float confidence
) {
    public CrashPattern {
        if (id == null) id = "";
        if (title == null) title = "";
        if (severity == null) severity = Severity.ERROR;
        if (regex == null) regex = "";
        if (explanation == null) explanation = "";
        if (suggestedFix == null) suggestedFix = "";
        if (confidence < 0) confidence = 0;
        if (confidence > 1) confidence = 1;
    }
}
