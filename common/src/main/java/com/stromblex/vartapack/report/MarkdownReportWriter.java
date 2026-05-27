package com.stromblex.vartapack.report;

import com.stromblex.vartapack.VartaPack;
import com.stromblex.vartapack.api.EnvironmentInfo;
import com.stromblex.vartapack.api.ModInfo;
import com.stromblex.vartapack.api.Platform;
import com.stromblex.vartapack.config.PackProfile;
import com.stromblex.vartapack.config.VartaConfig;
import com.stromblex.vartapack.crash.CrashAnalysisResult;
import com.stromblex.vartapack.crash.CrashFinding;
import com.stromblex.vartapack.validation.Issue;
import com.stromblex.vartapack.validation.IssueCategory;
import com.stromblex.vartapack.validation.PackStatus;
import com.stromblex.vartapack.validation.ValidationResult;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Generates enhanced Markdown support reports from a {@link ValidationResult}.
 * Includes pack status, categorized issues, fix instructions, and crash summary.
 */
public final class MarkdownReportWriter {

    public String write(Platform platform, VartaConfig config, PackProfile profile,
                        ValidationResult result, CrashAnalysisResult crashResult) {
        PrivacyRedactor redactor = new PrivacyRedactor(config.redactUserHomePath(), config.redactUsername());
        EnvironmentInfo env = EnvironmentInfo.capture(platform);

        StringBuilder sb = new StringBuilder(4096);
        sb.append("# VartaPack Support Report\n\n");

        // Pack Status
        sb.append("## Pack Status: ").append(result.status().displayName().toUpperCase()).append("\n");
        sb.append(result.status().description()).append("\n\n");

        // Pack info
        sb.append("## Pack\n");
        sb.append("| Field | Value |\n|-------|-------|\n");
        sb.append("| Pack name | ").append(cell(profile.packName())).append(" |\n");
        sb.append("| Pack ID | ").append(cell(profile.packId())).append(" |\n");
        sb.append("| Profile version | ").append(cell(profile.profileVersion())).append(" |\n");
        sb.append("| VartaPack version | ").append(cell(VartaPack.MOD_VERSION)).append(" |\n");
        if (!profile.supportUrl().isBlank()) {
            sb.append("| Support URL | ").append(cell(profile.supportUrl())).append(" |\n");
        }
        sb.append("\n");

        // Environment
        sb.append("## Environment\n");
        sb.append("| Field | Value |\n|-------|-------|\n");
        sb.append("| Minecraft | ").append(cell(env.minecraftVersion())).append(" |\n");
        sb.append("| Loader | ").append(cell(env.loaderName() + " " + env.loaderVersion())).append(" |\n");
        sb.append("| Java | ").append(cell(env.javaVersion() + " (major " + env.javaMajor() + ")")).append(" |\n");
        sb.append("| OS | ").append(cell(env.osName() + " " + env.osVersion())).append(" |\n");
        sb.append("| Allocated RAM | ").append(env.maxMemoryMb()).append(" MB |\n");
        sb.append("| Game directory | ").append(cell(redactor.redact(env.gameDirectory()))).append(" |\n");
        sb.append("\n");

        // Summary
        sb.append("## Issues Summary\n");
        sb.append("| Severity | Count |\n|----------|-------|\n");
        sb.append("| Critical | ").append(result.countBySeverity(com.stromblex.vartapack.check.Severity.CRITICAL)).append(" |\n");
        sb.append("| Error | ").append(result.countBySeverity(com.stromblex.vartapack.check.Severity.ERROR)).append(" |\n");
        sb.append("| Warning | ").append(result.countBySeverity(com.stromblex.vartapack.check.Severity.WARNING)).append(" |\n");
        sb.append("| Info | ").append(result.countBySeverity(com.stromblex.vartapack.check.Severity.INFO)).append(" |\n");
        sb.append("\n");

        // Issues by category
        sb.append("## Issues\n");
        if (result.issues().isEmpty()) {
            sb.append("_No issues detected._\n\n");
        } else {
            for (Issue issue : result.issues()) {
                sb.append("### [").append(issue.severity().name()).append("] ").append(issue.title()).append("\n");
                sb.append("- **Category:** ").append(issue.category().displayName()).append("\n");
                sb.append("- **Message:** ").append(redactor.redact(issue.message())).append("\n");
                if (!issue.detailedExplanation().isBlank()) {
                    sb.append("- **Details:** ").append(redactor.redact(issue.detailedExplanation())).append("\n");
                }
                if (!issue.fixInstruction().isBlank()) {
                    sb.append("- **Fix:** ").append(issue.fixInstruction()).append("\n");
                }
                if (!issue.affectedModId().isBlank()) {
                    sb.append("- **Affected mod:** ").append(issue.affectedModId()).append("\n");
                }
                if (!issue.affectedFilePath().isBlank()) {
                    sb.append("- **Affected file:** ").append(redactor.redact(issue.affectedFilePath())).append("\n");
                }
                sb.append("\n");
            }
        }

        // Suggested fixes
        List<Issue> fixable = result.issues().stream()
                .filter(i -> !i.fixInstruction().isBlank())
                .toList();
        if (!fixable.isEmpty()) {
            sb.append("## Suggested Fixes\n");
            int i = 1;
            for (Issue issue : fixable) {
                sb.append(i++).append(". ").append(issue.fixInstruction()).append("\n");
            }
            sb.append("\n");
        }

        // Crash summary
        if (crashResult != null && crashResult.hasFindings()) {
            sb.append("## Crash Analysis\n");
            sb.append(crashResult.summary()).append("\n\n");
            for (CrashFinding finding : crashResult.findings()) {
                sb.append("### ").append(finding.confidenceLabel()).append(": ").append(finding.title()).append("\n");
                sb.append("- **Explanation:** ").append(finding.explanation()).append("\n");
                sb.append("- **Suggested fix:** ").append(finding.suggestedFix()).append("\n");
                if (!finding.matchedExcerpt().isBlank()) {
                    sb.append("- **Log excerpt:** `").append(
                            redactor.redact(truncate(finding.matchedExcerpt(), 150))).append("`\n");
                }
                sb.append("\n");
            }
        }

        // Installed mods
        if (config.includeInstalledModsInReport()) {
            sb.append("## Installed Mods\n");
            List<ModInfo> mods = platform.getInstalledMods();
            mods.sort(Comparator.comparing(m -> m.id() == null ? "" : m.id()));
            sb.append("| Mod ID | Version |\n|--------|--------|\n");
            for (ModInfo m : mods) {
                sb.append("| ").append(cell(m.id())).append(" | ").append(cell(m.version())).append(" |\n");
            }
            sb.append("\n");
        }

        // Privacy notice
        sb.append("## Privacy\n");
        sb.append("This report was generated locally. VartaPack does not collect or send telemetry.\n");
        if (config.redactUserHomePath() || config.redactUsername()) {
            sb.append("Privacy redaction is enabled (");
            if (config.redactUserHomePath()) sb.append("home path");
            if (config.redactUserHomePath() && config.redactUsername()) sb.append(", ");
            if (config.redactUsername()) sb.append("username");
            sb.append(" redacted).\n");
        }
        sb.append("\n");

        // Footer
        sb.append("---\n");
        sb.append("Generated by ").append(VartaPack.MOD_NAME).append(" ").append(VartaPack.MOD_VERSION);
        sb.append(" at ").append(OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        sb.append("\n");

        return sb.toString();
    }

    private static String safe(String s) { return s == null ? "" : s; }

    /**
     * Escape a value for use inside a Markdown table cell. Pipes, backslashes,
     * and newlines would otherwise break the table layout.
     */
    private static String cell(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\r\n", " ")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
