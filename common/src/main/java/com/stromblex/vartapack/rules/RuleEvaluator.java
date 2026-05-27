package com.stromblex.vartapack.rules;

import com.stromblex.vartapack.check.CheckContext;
import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.validation.Issue;
import com.stromblex.vartapack.validation.IssueCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates advanced rules from rules.json against the current check context.
 * Produces {@link Issue} instances for each rule violation.
 */
public final class RuleEvaluator {

    public List<Issue> evaluate(RulesConfig rules, CheckContext ctx) {
        List<Issue> issues = new ArrayList<>();
        for (ModRule rule : rules.rules()) {
            Issue issue = evaluateRule(rule, ctx);
            if (issue != null) issues.add(issue);
        }
        return issues;
    }

    private Issue evaluateRule(ModRule rule, CheckContext ctx) {
        if (rule.modId().isBlank()) return null;

        return switch (rule.type()) {
            case BLOCKED_MOD -> evaluateBlocked(rule, ctx);
            case SOFT_BLOCKED_MOD -> evaluateSoftBlocked(rule, ctx);
            case REQUIRED_MOD -> evaluateRequired(rule, ctx);
            case RECOMMENDED_MOD -> evaluateRecommended(rule, ctx);
            case SUSPICIOUS_MOD -> evaluateSuspicious(rule, ctx);
            // MOD_CONFLICT is handled by ValidationEngine via ConflictRule entries.
            // ALLOWED_EXTRA_MOD is consumed by the extra-mods legacy check
            // (the modId is treated as an implicit allowlist entry).
            case ALLOWED_EXTRA_MOD, MOD_CONFLICT, ENVIRONMENT_RULE, FILE_RULE -> null;
        };
    }

    private Issue evaluateBlocked(ModRule rule, CheckContext ctx) {
        if (!ctx.hasMod(rule.modId())) return null;
        return Issue.builder("rule.blocked." + rule.modId())
                .title("Blocked mod: " + rule.displayName())
                .severity(rule.severity())
                .category(categoryFromString(rule.category()))
                .message(rule.reason().isBlank()
                        ? rule.displayName() + " is not allowed in this modpack."
                        : rule.reason())
                .fix(rule.fix())
                .affectedMod(rule.modId())
                .blocksContinue(rule.blockContinue())
                .build();
    }

    private Issue evaluateSoftBlocked(ModRule rule, CheckContext ctx) {
        if (!ctx.hasMod(rule.modId())) return null;
        Severity sev = rule.severity().ordinal() > Severity.WARNING.ordinal()
                ? Severity.WARNING : rule.severity();
        return Issue.builder("rule.softblocked." + rule.modId())
                .title("Discouraged mod: " + rule.displayName())
                .severity(sev)
                .category(categoryFromString(rule.category()))
                .message(rule.reason().isBlank()
                        ? rule.displayName() + " is discouraged for this modpack."
                        : rule.reason())
                .fix(rule.fix())
                .affectedMod(rule.modId())
                .blocksContinue(false)
                .build();
    }

    private Issue evaluateRequired(ModRule rule, CheckContext ctx) {
        if (ctx.hasMod(rule.modId())) return null;
        return Issue.builder("rule.required." + rule.modId())
                .title("Missing required mod: " + rule.displayName())
                .severity(rule.severity())
                .category(IssueCategory.REQUIRED_MOD)
                .message(rule.reason().isBlank()
                        ? rule.displayName() + " is required for this modpack."
                        : rule.reason())
                .fix(rule.fix().isBlank()
                        ? "Install " + rule.displayName() + "."
                        : rule.fix())
                .affectedMod(rule.modId())
                .blocksContinue(rule.blockContinue())
                .build();
    }

    private Issue evaluateRecommended(ModRule rule, CheckContext ctx) {
        if (ctx.hasMod(rule.modId())) return null;
        Severity sev = rule.severity().ordinal() > Severity.WARNING.ordinal()
                ? Severity.WARNING : rule.severity();
        return Issue.builder("rule.recommended." + rule.modId())
                .title("Recommended mod missing: " + rule.displayName())
                .severity(sev)
                .category(IssueCategory.RECOMMENDED_MOD)
                .message(rule.reason().isBlank()
                        ? rule.displayName() + " is recommended for this modpack."
                        : rule.reason())
                .fix(rule.fix().isBlank()
                        ? "Consider installing " + rule.displayName() + "."
                        : rule.fix())
                .affectedMod(rule.modId())
                .blocksContinue(false)
                .build();
    }

    private Issue evaluateSuspicious(ModRule rule, CheckContext ctx) {
        if (!ctx.hasMod(rule.modId())) return null;
        return Issue.builder("rule.suspicious." + rule.modId())
                .title("Suspicious mod detected: " + rule.displayName())
                .severity(rule.severity())
                .category(IssueCategory.EXTRA_MOD)
                .message(rule.reason().isBlank()
                        ? rule.displayName() + " is flagged as suspicious."
                        : rule.reason())
                .fix(rule.fix())
                .affectedMod(rule.modId())
                .blocksContinue(rule.blockContinue())
                .build();
    }

    private static IssueCategory categoryFromString(String category) {
        if (category == null || category.isBlank()) return IssueCategory.BLOCKED_MOD;
        try {
            return IssueCategory.valueOf(category.toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            if (category.toLowerCase().contains("render")) return IssueCategory.RENDERING_CONFLICT;
            return IssueCategory.BLOCKED_MOD;
        }
    }
}
