package com.stromblex.vartapack.validation;

import com.stromblex.vartapack.check.CheckResult;
import com.stromblex.vartapack.check.Severity;

/**
 * Enhanced issue representation with full metadata for UI, reports, and fix instructions.
 * This extends the simpler {@link CheckResult} with additional context.
 */
public record Issue(
        String id,
        String title,
        Severity severity,
        IssueCategory category,
        String message,
        String detailedExplanation,
        String fixInstruction,
        String affectedModId,
        String affectedFilePath,
        String documentationUrl,
        boolean blocksContinue,
        boolean includeInReport
) {
    public Issue {
        if (id == null) id = "unknown";
        if (title == null) title = "";
        if (severity == null) severity = Severity.INFO;
        if (category == null) category = IssueCategory.INTERNAL;
        if (message == null) message = "";
        if (detailedExplanation == null) detailedExplanation = "";
        if (fixInstruction == null) fixInstruction = "";
        if (affectedModId == null) affectedModId = "";
        if (affectedFilePath == null) affectedFilePath = "";
        if (documentationUrl == null) documentationUrl = "";
    }

    /**
     * Convert a legacy {@link CheckResult} into an Issue with reasonable defaults.
     */
    public static Issue fromCheckResult(CheckResult result, IssueCategory category) {
        boolean blocks = result.severity().ordinal() >= Severity.ERROR.ordinal();
        return new Issue(
                result.code(),
                result.title(),
                result.severity(),
                category,
                result.message(),
                result.technicalDetails(),
                "",
                "",
                "",
                "",
                blocks,
                true
        );
    }

    /**
     * Convert a legacy {@link CheckResult} into an Issue with fix instruction.
     */
    public static Issue fromCheckResult(CheckResult result, IssueCategory category, String fix) {
        return fromCheckResult(result, category, fix, "");
    }

    /**
     * Convert a legacy {@link CheckResult} into an Issue with fix instruction and affected mod ID.
     */
    public static Issue fromCheckResult(CheckResult result, IssueCategory category, String fix, String modId) {
        boolean blocks = result.severity().ordinal() >= Severity.ERROR.ordinal();
        return new Issue(
                result.code(),
                result.title(),
                result.severity(),
                category,
                result.message(),
                result.technicalDetails(),
                fix,
                modId,
                "",
                "",
                blocks,
                true
        );
    }

    /** Builder for constructing Issues fluently. */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private String title = "";
        private Severity severity = Severity.INFO;
        private IssueCategory category = IssueCategory.INTERNAL;
        private String message = "";
        private String detailedExplanation = "";
        private String fixInstruction = "";
        private String affectedModId = "";
        private String affectedFilePath = "";
        private String documentationUrl = "";
        private boolean blocksContinue = false;
        private boolean includeInReport = true;

        private Builder(String id) { this.id = id; }

        public Builder title(String title) { this.title = title; return this; }
        public Builder severity(Severity severity) { this.severity = severity; return this; }
        public Builder category(IssueCategory category) { this.category = category; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder detailedExplanation(String explanation) { this.detailedExplanation = explanation; return this; }
        public Builder fix(String fix) { this.fixInstruction = fix; return this; }
        public Builder affectedMod(String modId) { this.affectedModId = modId; return this; }
        public Builder affectedFile(String path) { this.affectedFilePath = path; return this; }
        public Builder documentationUrl(String url) { this.documentationUrl = url; return this; }
        public Builder blocksContinue(boolean blocks) { this.blocksContinue = blocks; return this; }
        public Builder includeInReport(boolean include) { this.includeInReport = include; return this; }

        public Issue build() {
            return new Issue(id, title, severity, category, message, detailedExplanation,
                    fixInstruction, affectedModId, affectedFilePath, documentationUrl,
                    blocksContinue, includeInReport);
        }
    }
}
