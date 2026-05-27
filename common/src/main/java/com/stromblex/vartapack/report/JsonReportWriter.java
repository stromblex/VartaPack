package com.stromblex.vartapack.report;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.stromblex.vartapack.VartaPack;
import com.stromblex.vartapack.api.EnvironmentInfo;
import com.stromblex.vartapack.api.ModInfo;
import com.stromblex.vartapack.api.Platform;
import com.stromblex.vartapack.config.PackProfile;
import com.stromblex.vartapack.config.VartaConfig;
import com.stromblex.vartapack.config.JsonUtil;
import com.stromblex.vartapack.crash.CrashAnalysisResult;
import com.stromblex.vartapack.crash.CrashFinding;
import com.stromblex.vartapack.validation.Issue;
import com.stromblex.vartapack.validation.ValidationResult;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Generates JSON support reports from a {@link ValidationResult}.
 * Useful for tooling integration and automated processing.
 */
public final class JsonReportWriter {

    public String write(Platform platform, VartaConfig config, PackProfile profile,
                        ValidationResult result, CrashAnalysisResult crashResult) {
        PrivacyRedactor redactor = new PrivacyRedactor(config.redactUserHomePath(), config.redactUsername());
        EnvironmentInfo env = EnvironmentInfo.capture(platform);

        JsonObject root = new JsonObject();
        root.addProperty("reportVersion", 1);
        root.addProperty("generatedAt", OffsetDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        root.addProperty("vartapackVersion", VartaPack.MOD_VERSION);

        // Status
        root.addProperty("packStatus", result.status().name());
        root.addProperty("packStatusDescription", result.status().description());

        // Pack
        JsonObject pack = new JsonObject();
        pack.addProperty("packId", safe(profile.packId()));
        pack.addProperty("packName", safe(profile.packName()));
        pack.addProperty("profileVersion", safe(profile.profileVersion()));
        pack.addProperty("supportUrl", safe(profile.supportUrl()));
        root.add("pack", pack);

        // Environment
        JsonObject environment = new JsonObject();
        environment.addProperty("minecraft", env.minecraftVersion());
        environment.addProperty("loader", env.loaderName());
        environment.addProperty("loaderVersion", env.loaderVersion());
        environment.addProperty("java", env.javaVersion());
        environment.addProperty("javaMajor", env.javaMajor());
        environment.addProperty("os", env.osName() + " " + env.osVersion());
        environment.addProperty("allocatedRamMb", env.maxMemoryMb());
        environment.addProperty("gameDirectory", redactor.redact(env.gameDirectory()));
        root.add("environment", environment);

        // Summary
        JsonObject summary = new JsonObject();
        summary.addProperty("critical", result.countBySeverity(com.stromblex.vartapack.check.Severity.CRITICAL));
        summary.addProperty("errors", result.countBySeverity(com.stromblex.vartapack.check.Severity.ERROR));
        summary.addProperty("warnings", result.countBySeverity(com.stromblex.vartapack.check.Severity.WARNING));
        summary.addProperty("info", result.countBySeverity(com.stromblex.vartapack.check.Severity.INFO));
        root.add("summary", summary);

        // Issues
        JsonArray issues = new JsonArray();
        for (Issue issue : result.issues()) {
            JsonObject io = new JsonObject();
            io.addProperty("id", issue.id());
            io.addProperty("title", issue.title());
            io.addProperty("severity", issue.severity().name());
            io.addProperty("category", issue.category().name());
            io.addProperty("message", redactor.redact(issue.message()));
            if (!issue.detailedExplanation().isBlank())
                io.addProperty("details", redactor.redact(issue.detailedExplanation()));
            if (!issue.fixInstruction().isBlank())
                io.addProperty("fix", issue.fixInstruction());
            if (!issue.affectedModId().isBlank())
                io.addProperty("affectedMod", issue.affectedModId());
            if (!issue.affectedFilePath().isBlank())
                io.addProperty("affectedFile", redactor.redact(issue.affectedFilePath()));
            io.addProperty("blocksContinue", issue.blocksContinue());
            issues.add(io);
        }
        root.add("issues", issues);

        // Crash analysis
        if (crashResult != null && crashResult.hasFindings()) {
            JsonObject crash = new JsonObject();
            crash.addProperty("summary", crashResult.summary());
            crash.addProperty("hasCrashReports", crashResult.hasCrashReports());
            crash.addProperty("hasLatestLog", crashResult.hasLatestLog());
            JsonArray findings = new JsonArray();
            for (CrashFinding f : crashResult.findings()) {
                JsonObject fo = new JsonObject();
                fo.addProperty("patternId", f.patternId());
                fo.addProperty("title", f.title());
                fo.addProperty("severity", f.severity().name());
                fo.addProperty("confidence", f.confidence());
                fo.addProperty("confidenceLabel", f.confidenceLabel());
                fo.addProperty("explanation", f.explanation());
                fo.addProperty("suggestedFix", f.suggestedFix());
                if (!f.matchedExcerpt().isBlank())
                    fo.addProperty("excerpt", redactor.redact(f.matchedExcerpt()));
                findings.add(fo);
            }
            crash.add("findings", findings);
            root.add("crashAnalysis", crash);
        }

        // Installed mods
        if (config.includeInstalledModsInReport()) {
            JsonArray mods = new JsonArray();
            List<ModInfo> modList = platform.getInstalledMods();
            modList.sort(Comparator.comparing(m -> m.id() == null ? "" : m.id()));
            for (ModInfo m : modList) {
                JsonObject mo = new JsonObject();
                mo.addProperty("id", safe(m.id()));
                mo.addProperty("name", safe(m.name()));
                mo.addProperty("version", safe(m.version()));
                mods.add(mo);
            }
            root.add("installedMods", mods);
        }

        return JsonUtil.GSON.toJson(root);
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
