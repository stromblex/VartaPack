package com.stromblex.vartapack.validation;

import com.stromblex.vartapack.check.CheckResult;
import com.stromblex.vartapack.check.Severity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The result of a full validation pass. Contains the computed {@link PackStatus},
 * all issues found, and the original legacy check results for backward compatibility.
 */
public final class ValidationResult {
    private final PackStatus status;
    private final List<Issue> issues;
    private final List<CheckResult> legacyResults;

    public ValidationResult(PackStatus status, List<Issue> issues, List<CheckResult> legacyResults) {
        this.status = status;
        this.issues = List.copyOf(issues);
        this.legacyResults = List.copyOf(legacyResults);
    }

    public PackStatus status() { return status; }
    public List<Issue> issues() { return issues; }
    public List<CheckResult> legacyResults() { return legacyResults; }

    public int countBySeverity(Severity severity) {
        int count = 0;
        for (Issue issue : issues) {
            if (issue.severity() == severity) count++;
        }
        return count;
    }

    public int countByCategory(IssueCategory category) {
        int count = 0;
        for (Issue issue : issues) {
            if (issue.category() == category) count++;
        }
        return count;
    }

    public List<Issue> issuesBySeverity(Severity severity) {
        List<Issue> result = new ArrayList<>();
        for (Issue issue : issues) {
            if (issue.severity() == severity) result.add(issue);
        }
        return Collections.unmodifiableList(result);
    }

    public List<Issue> issuesByCategory(IssueCategory category) {
        List<Issue> result = new ArrayList<>();
        for (Issue issue : issues) {
            if (issue.category() == category) result.add(issue);
        }
        return Collections.unmodifiableList(result);
    }

    public boolean hasBlockingIssues() {
        for (Issue issue : issues) {
            if (issue.blocksContinue()) return true;
        }
        return false;
    }

    public Severity highestSeverity() {
        Severity highest = Severity.INFO;
        for (Issue issue : issues) {
            if (issue.severity().ordinal() > highest.ordinal()) {
                highest = issue.severity();
            }
        }
        return highest;
    }

    /**
     * Compute the appropriate PackStatus from a list of issues.
     */
    public static PackStatus computeStatus(List<Issue> issues) {
        boolean hasCritical = false;
        boolean hasError = false;
        boolean hasWarning = false;
        boolean hasModification = false;

        for (Issue issue : issues) {
            switch (issue.severity()) {
                case CRITICAL -> hasCritical = true;
                case ERROR -> hasError = true;
                case WARNING -> hasWarning = true;
                case INFO -> {
                    // INFO-level issues from extra mods or config changes = modification
                    if (issue.category() == IssueCategory.EXTRA_MOD
                            || issue.category() == IssueCategory.INTEGRITY
                            || issue.category() == IssueCategory.CONFIGURATION) {
                        hasModification = true;
                    }
                }
            }
        }

        if (hasCritical || hasError) return PackStatus.BROKEN;
        if (hasWarning) return PackStatus.UNSUPPORTED;
        if (hasModification) return PackStatus.MODIFIED;
        return PackStatus.CLEAN;
    }
}
