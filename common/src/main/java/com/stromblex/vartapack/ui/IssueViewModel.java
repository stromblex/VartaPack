package com.stromblex.vartapack.ui;

import com.stromblex.vartapack.VartaPack;
import com.stromblex.vartapack.check.CheckResult;
import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.report.JsonReportWriter;
import com.stromblex.vartapack.report.MarkdownReportWriter;
import com.stromblex.vartapack.report.SupportReport;
import com.stromblex.vartapack.report.SupportReportBuilder;
import com.stromblex.vartapack.validation.Issue;
import com.stromblex.vartapack.validation.PackStatus;
import com.stromblex.vartapack.validation.ValidationResult;

import java.util.List;

/**
 * Pure-data view model shared between Fabric/NeoForge screens.
 * Loader-specific screen classes read this and render with their own widgets.
 */
public final class IssueViewModel {

    public record Row(Severity severity, String title, String message, String technicalDetails, String fix) {}

    private final List<CheckResult> results;
    private final ValidationResult validationResult;
    private final String packName;
    private final String profileVersion;
    private final boolean packPingInstalled;
    private final boolean packPingRecommended;
    private final String supportUrl;

    public IssueViewModel(List<CheckResult> results, ValidationResult validationResult,
                          String packName, String profileVersion,
                          boolean packPingInstalled, boolean packPingRecommended,
                          String supportUrl) {
        this.results = List.copyOf(results);
        this.validationResult = validationResult;
        this.packName = packName == null ? "" : packName;
        this.profileVersion = profileVersion == null ? "" : profileVersion;
        this.packPingInstalled = packPingInstalled;
        this.packPingRecommended = packPingRecommended;
        this.supportUrl = supportUrl == null ? "" : supportUrl;
    }

    public static IssueViewModel build() {
        var profile = VartaPack.profile();
        boolean ppRecommended = profile.recommendedMods().stream()
                .anyMatch(r -> SupportReportBuilder.PACKPING_ID.equalsIgnoreCase(r.id()));
        boolean ppInstalled = VartaPack.platform() != null
                && VartaPack.platform().isModLoaded(SupportReportBuilder.PACKPING_ID);
        return new IssueViewModel(
                VartaPack.lastResults(),
                VartaPack.lastValidation(),
                profile.packName(),
                profile.profileVersion(),
                ppInstalled,
                ppRecommended,
                profile.supportUrl()
        );
    }

    public PackStatus packStatus() {
        if (validationResult != null) return validationResult.status();
        if (results.isEmpty()) return PackStatus.CLEAN;
        for (CheckResult r : results) {
            if (r.severity().ordinal() >= Severity.ERROR.ordinal()) return PackStatus.BROKEN;
        }
        for (CheckResult r : results) {
            if (r.severity() == Severity.WARNING) return PackStatus.UNSUPPORTED;
        }
        return PackStatus.MODIFIED;
    }

    public List<Row> rows() {
        if (validationResult != null) {
            return validationResult.issues().stream()
                    .filter(i -> i.includeInReport() && i.severity().ordinal() >= Severity.INFO.ordinal())
                    .map(i -> new Row(i.severity(), i.title(), i.message(), i.detailedExplanation(), i.fixInstruction()))
                    .toList();
        }
        return results.stream()
                .filter(CheckResult::visibleToPlayer)
                .map(r -> new Row(r.severity(), r.title(), r.message(), r.technicalDetails(), ""))
                .toList();
    }

    public SupportReport buildReport() {
        return new SupportReportBuilder().build(
                VartaPack.platform(),
                VartaPack.config(),
                VartaPack.profile(),
                results
        );
    }

    public String buildMarkdownReport() {
        if (validationResult != null && VartaPack.platform() != null) {
            MarkdownReportWriter writer = new MarkdownReportWriter();
            return writer.write(VartaPack.platform(), VartaPack.config(), VartaPack.profile(),
                    validationResult, VartaPack.lastCrashAnalysis());
        }
        return buildReport().markdown();
    }

    public String buildJsonReport() {
        if (validationResult != null && VartaPack.platform() != null) {
            JsonReportWriter writer = new JsonReportWriter();
            return writer.write(VartaPack.platform(), VartaPack.config(), VartaPack.profile(),
                    validationResult, VartaPack.lastCrashAnalysis());
        }
        return "{}";
    }

    public String packName() { return packName; }
    public String profileVersion() { return profileVersion; }
    public boolean packPingInstalled() { return packPingInstalled; }
    public boolean packPingRecommended() { return packPingRecommended; }
    public String supportUrl() { return supportUrl; }
    public List<CheckResult> rawResults() { return results; }
}
