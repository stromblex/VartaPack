package com.stromblex.vartapack.rules;

import com.stromblex.vartapack.api.EnvironmentInfo;
import com.stromblex.vartapack.api.ModInfo;
import com.stromblex.vartapack.check.CheckContext;
import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.config.PackProfile;
import com.stromblex.vartapack.config.VartaConfig;
import com.stromblex.vartapack.validation.Issue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

final class RuleEvaluatorTest {

    @Test
    void blockedRuleTriggersWhenModPresent() {
        CheckContext ctx = context(List.of(new ModInfo("optifine", "OptiFine", "1.0", "")));
        RulesConfig rules = new RulesConfig(1,
                List.of(new ModRule("blocked.optifine", RuleType.BLOCKED_MOD, "optifine",
                        "OptiFine", Severity.CRITICAL, "RENDERING_CONFLICT",
                        "OptiFine conflicts with Sodium.", "Remove optifine.jar.", "", true)),
                List.of(), "");

        RuleEvaluator evaluator = new RuleEvaluator();
        List<Issue> issues = evaluator.evaluate(rules, ctx);

        assertEquals(1, issues.size());
        assertEquals(Severity.CRITICAL, issues.get(0).severity());
        assertTrue(issues.get(0).blocksContinue());
        assertTrue(issues.get(0).fixInstruction().contains("Remove"));
    }

    @Test
    void blockedRuleDoesNotTriggerWhenModAbsent() {
        CheckContext ctx = context(List.of(new ModInfo("sodium", "Sodium", "0.5", "")));
        RulesConfig rules = new RulesConfig(1,
                List.of(new ModRule("blocked.optifine", RuleType.BLOCKED_MOD, "optifine",
                        "OptiFine", Severity.CRITICAL, "", "Bad.", "Remove it.", "", true)),
                List.of(), "");

        RuleEvaluator evaluator = new RuleEvaluator();
        List<Issue> issues = evaluator.evaluate(rules, ctx);

        assertTrue(issues.isEmpty());
    }

    @Test
    void requiredRuleTriggersWhenModMissing() {
        CheckContext ctx = context(List.of());
        RulesConfig rules = new RulesConfig(1,
                List.of(new ModRule("required.sodium", RuleType.REQUIRED_MOD, "sodium",
                        "Sodium", Severity.ERROR, "", "Performance.", "Install Sodium.", "", true)),
                List.of(), "");

        RuleEvaluator evaluator = new RuleEvaluator();
        List<Issue> issues = evaluator.evaluate(rules, ctx);

        assertEquals(1, issues.size());
        assertEquals(Severity.ERROR, issues.get(0).severity());
    }

    @Test
    void requiredRuleDoesNotTriggerWhenModPresent() {
        CheckContext ctx = context(List.of(new ModInfo("sodium", "Sodium", "0.5", "")));
        RulesConfig rules = new RulesConfig(1,
                List.of(new ModRule("required.sodium", RuleType.REQUIRED_MOD, "sodium",
                        "Sodium", Severity.ERROR, "", "", "", "", true)),
                List.of(), "");

        RuleEvaluator evaluator = new RuleEvaluator();
        List<Issue> issues = evaluator.evaluate(rules, ctx);

        assertTrue(issues.isEmpty());
    }

    @Test
    void softBlockedUsesWarning() {
        CheckContext ctx = context(List.of(new ModInfo("xray", "X-Ray", "1.0", "")));
        RulesConfig rules = new RulesConfig(1,
                List.of(new ModRule("soft.xray", RuleType.SOFT_BLOCKED_MOD, "xray",
                        "X-Ray", Severity.ERROR, "", "Cheating.", "", "", false)),
                List.of(), "");

        RuleEvaluator evaluator = new RuleEvaluator();
        List<Issue> issues = evaluator.evaluate(rules, ctx);

        assertEquals(1, issues.size());
        assertEquals(Severity.WARNING, issues.get(0).severity());
        assertFalse(issues.get(0).blocksContinue());
    }

    @Test
    void suspiciousRuleDetects() {
        CheckContext ctx = context(List.of(new ModInfo("cheatmod", "Cheat", "1.0", "")));
        RulesConfig rules = new RulesConfig(1,
                List.of(new ModRule("sus.cheat", RuleType.SUSPICIOUS_MOD, "cheatmod",
                        "Cheat Mod", Severity.WARNING, "", "This mod is suspicious.", "", "", false)),
                List.of(), "");

        RuleEvaluator evaluator = new RuleEvaluator();
        List<Issue> issues = evaluator.evaluate(rules, ctx);

        assertEquals(1, issues.size());
        assertTrue(issues.get(0).title().contains("Suspicious"));
    }

    private static CheckContext context(List<ModInfo> mods) {
        Map<String, ModInfo> installed = mods.stream()
                .collect(Collectors.toMap(m -> m.id().toLowerCase(Locale.ROOT), m -> m));
        EnvironmentInfo env = new EnvironmentInfo(
                "1.18.2", "Fabric", "0.16.5", "17", 17,
                "Linux", "test", 8192, "/tmp/vartapack-test");
        return new CheckContext(VartaConfig.defaults(), PackProfile.defaults(), null, env, installed);
    }
}
