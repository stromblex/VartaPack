package com.stromblex.vartapack.validation;

import com.stromblex.vartapack.VartaPack;
import com.stromblex.vartapack.api.Platform;
import com.stromblex.vartapack.check.CheckContext;
import com.stromblex.vartapack.check.CheckManager;
import com.stromblex.vartapack.check.CheckResult;
import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.config.PackProfile;
import com.stromblex.vartapack.config.VartaConfig;
import com.stromblex.vartapack.integrity.IntegrityChecker;
import com.stromblex.vartapack.integrity.IntegrityManifest;
import com.stromblex.vartapack.rules.ConflictRule;
import com.stromblex.vartapack.rules.RulesConfig;
import com.stromblex.vartapack.rules.RuleEvaluator;
import com.stromblex.vartapack.util.VersionUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Central validation engine. Runs all checks (legacy + new rules + integrity + conflicts)
 * and produces a unified {@link ValidationResult} with {@link PackStatus}.
 */
public final class ValidationEngine {

    private final CheckManager checkManager = new CheckManager();

    /**
     * Run full validation and return a comprehensive result.
     */
    public ValidationResult validate(Platform platform, VartaConfig config, PackProfile profile,
                                     RulesConfig rules, IntegrityManifest manifest) {
        // Run legacy checks
        List<CheckResult> legacyResults = checkManager.runAll(platform, config, profile);

        // Convert legacy results to enhanced issues
        List<Issue> issues = new ArrayList<>();
        for (CheckResult result : legacyResults) {
            IssueCategory category = categorizeCheckResult(result);
            String fix = suggestFix(result, profile);
            String modId = extractModIdFromCode(result.code());
            Issue issue = Issue.fromCheckResult(result, category, fix, modId);
            issues.add(issue);
        }

        // Run rules evaluation if rules are present
        if (rules != null && !rules.rules().isEmpty()) {
            CheckContext ctx = CheckContext.create(platform, config, profile);
            RuleEvaluator evaluator = new RuleEvaluator();
            List<Issue> ruleIssues = evaluator.evaluate(rules, ctx);
            issues.addAll(ruleIssues);
        }

        // Run conflict detection if rules have conflicts
        if (rules != null && !rules.conflicts().isEmpty()) {
            CheckContext ctx = CheckContext.create(platform, config, profile);
            List<Issue> conflictIssues = evaluateConflicts(rules.conflicts(), ctx);
            issues.addAll(conflictIssues);
        }

        // Run integrity checks if manifest is present
        if (manifest != null && !manifest.files().isEmpty()) {
            Path gameDir = platform.getGameDirectory();
            IntegrityChecker checker = new IntegrityChecker();
            List<Issue> integrityIssues = checker.check(manifest, gameDir);
            issues.addAll(integrityIssues);
        }

        // Sort by severity descending FIRST, so deduplication keeps the strictest issue per id/mod+category
        issues.sort((a, b) -> Integer.compare(b.severity().ordinal(), a.severity().ordinal()));

        // Deduplicate issues (rules may overlap with legacy checks)
        issues = deduplicateIssues(issues);

        // Compute status
        PackStatus status = ValidationResult.computeStatus(issues);

        return new ValidationResult(status, issues, legacyResults);
    }

    /**
     * Simplified validation using only legacy checks (backward compatible).
     */
    public ValidationResult validateLegacy(Platform platform, VartaConfig config, PackProfile profile) {
        return validate(platform, config, profile, null, null);
    }

    private List<Issue> evaluateConflicts(List<ConflictRule> conflicts, CheckContext ctx) {
        List<Issue> issues = new ArrayList<>();
        for (ConflictRule conflict : conflicts) {
            if (conflict.modA().isBlank() || conflict.modB().isBlank()) continue;
            boolean hasA = ctx.hasMod(conflict.modA());
            boolean hasB = ctx.hasMod(conflict.modB());
            if (hasA && hasB && versionMatches(ctx, conflict.modA(), conflict.versionRangeA())
                    && versionMatches(ctx, conflict.modB(), conflict.versionRangeB())) {
                issues.add(Issue.builder("conflict." + conflict.id())
                        .title("Mod conflict: " + conflict.modA() + " + " + conflict.modB())
                        .severity(conflict.severity())
                        .category(IssueCategory.MOD_CONFLICT)
                        .message(conflict.reason())
                        .fix(conflict.fix())
                        .blocksContinue(conflict.severity().ordinal() >= Severity.ERROR.ordinal())
                        .build());
            }
        }
        return issues;
    }

    private static boolean versionMatches(CheckContext ctx, String modId, String range) {
        if (range == null || range.isBlank()) return true;
        var mod = ctx.getMod(modId);
        if (mod == null) return false;
        return VersionUtil.satisfies(mod.version(), range);
    }

    private List<Issue> deduplicateIssues(List<Issue> issues) {
        List<Issue> deduplicated = new ArrayList<>();
        java.util.Set<String> seenIds = new java.util.HashSet<>();
        java.util.Set<String> seenModCategories = new java.util.HashSet<>();

        for (Issue issue : issues) {
            if (!seenIds.add(issue.id())) continue;

            // Also deduplicate by (affectedModId + category) to avoid showing the same
            // mod issue from both legacy checks and rules.json
            if (!issue.affectedModId().isBlank()) {
                String modCatKey = issue.affectedModId().toLowerCase(java.util.Locale.ROOT)
                        + ":" + issue.category().name();
                if (!seenModCategories.add(modCatKey)) continue;
            }

            deduplicated.add(issue);
        }
        return deduplicated;
    }

    private static IssueCategory categorizeCheckResult(CheckResult result) {
        String code = result.code();
        if (code.startsWith("java.")) return IssueCategory.ENVIRONMENT;
        if (code.startsWith("ram.")) return IssueCategory.ENVIRONMENT;
        if (code.startsWith("mc.")) return IssueCategory.MINECRAFT_VERSION;
        if (code.startsWith("loader.")) return IssueCategory.LOADER;
        if (code.startsWith("required.")) return IssueCategory.REQUIRED_MOD;
        if (code.startsWith("blocked.")) return IssueCategory.BLOCKED_MOD;
        if (code.startsWith("recommended.")) return IssueCategory.RECOMMENDED_MOD;
        if (code.startsWith("extra.")) return IssueCategory.EXTRA_MOD;
        if (code.startsWith("environment.")) return IssueCategory.ENVIRONMENT;
        if (code.startsWith("conflict.")) return IssueCategory.MOD_CONFLICT;
        if (code.startsWith("integrity.")) return IssueCategory.INTEGRITY;
        return IssueCategory.INTERNAL;
    }

    private static String suggestFix(CheckResult result, PackProfile profile) {
        String code = result.code();
        if (code.startsWith("java.tooOld")) {
            return "Update to Java " + profile.minimumJavaMajor() + " or newer. Check your launcher's Java settings.";
        }
        if (code.startsWith("ram.tooLow")) {
            return "Allocate at least " + profile.minimumRamMb() + " MB RAM in your launcher settings.";
        }
        if (code.startsWith("ram.belowRecommended")) {
            return "Allocate at least " + profile.recommendedRamMb() + " MB RAM for best performance.";
        }
        if (code.startsWith("loader.mismatch")) {
            return "Use the correct mod loader: " + String.join(" or ", profile.expectedLoaders()) + ".";
        }
        if (code.startsWith("mc.mismatch")) {
            return "Use Minecraft version " + String.join(" or ", profile.expectedMinecraftVersions()) + ".";
        }
        if (code.startsWith("required.missing.")) {
            String modId = code.substring("required.missing.".length());
            return "Install the missing required mod: " + modId + ".";
        }
        if (code.startsWith("blocked.present.")) {
            String modId = code.substring("blocked.present.".length());
            return "Remove " + modId + " from the mods folder and restart Minecraft.";
        }
        if (code.startsWith("recommended.missing.")) {
            String modId = code.substring("recommended.missing.".length());
            return "Consider installing " + modId + " for the best experience.";
        }
        if (code.equals("extra.mods")) {
            return "Remove extra mods or add them to the allowedExtraMods list in the profile.";
        }
        return "";
    }

    private static String extractModIdFromCode(String code) {
        if (code.startsWith("required.missing.")) return code.substring("required.missing.".length());
        if (code.startsWith("blocked.present.")) return code.substring("blocked.present.".length());
        if (code.startsWith("recommended.missing.")) return code.substring("recommended.missing.".length());
        return "";
    }
}
