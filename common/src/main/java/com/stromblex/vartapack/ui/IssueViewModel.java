package com.stromblex.vartapack.ui;

import com.stromblex.vartapack.VartaPack;
import com.stromblex.vartapack.check.CheckResult;
import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.report.SupportReport;
import com.stromblex.vartapack.report.SupportReportBuilder;

import java.util.List;

/**
 * Pure-data view model shared between Fabric/NeoForge screens.
 * Loader-specific screen classes read this and render with their own widgets.
 */
public final class IssueViewModel {

    public record Row(Severity severity, String title, String message, String technicalDetails) {}

    private final List<CheckResult> results;
    private final String packName;
    private final String profileVersion;
    private final boolean packPingInstalled;
    private final boolean packPingRecommended;
    private final String supportUrl;

    public IssueViewModel(List<CheckResult> results, String packName, String profileVersion,
                          boolean packPingInstalled, boolean packPingRecommended,
                          String supportUrl) {
        this.results = List.copyOf(results);
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
                profile.packName(),
                profile.profileVersion(),
                ppInstalled,
                ppRecommended,
                profile.supportUrl()
        );
    }

    public List<Row> rows() {
        return results.stream()
                .filter(CheckResult::visibleToPlayer)
                .map(r -> new Row(r.severity(), r.title(), r.message(), r.technicalDetails()))
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

    public String packName() { return packName; }
    public String profileVersion() { return profileVersion; }
    public boolean packPingInstalled() { return packPingInstalled; }
    public boolean packPingRecommended() { return packPingRecommended; }
    public String supportUrl() { return supportUrl; }
    public List<CheckResult> rawResults() { return results; }
}
