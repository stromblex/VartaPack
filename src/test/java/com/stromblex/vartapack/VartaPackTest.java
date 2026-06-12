package com.stromblex.vartapack;

import com.stromblex.vartapack.check.CheckResult;
import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.config.VartaConfig;
import com.stromblex.vartapack.validation.Issue;
import com.stromblex.vartapack.validation.IssueCategory;
import com.stromblex.vartapack.validation.ValidationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VartaPackTest {

    @AfterEach
    void resetRuntimeState() throws Exception {
        setField("config", VartaConfig.defaults());
        setField("lastResults", Collections.emptyList());
        setField("lastValidation", null);
    }

    @Test
    void hasIssuesAtLeastUsesValidationSeverityWhenAvailable() throws Exception {
        setValidationResult(Issue.builder("test.error")
                .severity(Severity.ERROR)
                .category(IssueCategory.INTERNAL)
                .build());
        setField("lastResults", Collections.<CheckResult>emptyList());

        assertTrue(VartaPack.hasIssuesAtLeast(Severity.ERROR));
    }

    @Test
    void shouldBlockContinueUsesBlockingValidationIssues() throws Exception {
        setField("config", VartaConfig.defaults());
        setValidationResult(Issue.builder("test.blocking")
                .severity(Severity.ERROR)
                .category(IssueCategory.INTERNAL)
                .blocksContinue(true)
                .build());

        assertTrue(VartaPack.shouldBlockContinue());
    }

    @Test
    void shouldBlockContinueAllowsNonBlockingValidationErrorsWhenConfigured() throws Exception {
        setField("config", VartaConfig.defaults());
        setValidationResult(Issue.builder("test.non_blocking")
                .severity(Severity.ERROR)
                .category(IssueCategory.INTERNAL)
                .blocksContinue(false)
                .build());

        assertFalse(VartaPack.shouldBlockContinue());
    }

    private static void setValidationResult(Issue issue) throws Exception {
        ValidationResult result = new ValidationResult(
                ValidationResult.computeStatus(List.of(issue)),
                List.of(issue),
                Collections.emptyList()
        );
        setField("lastValidation", result);
    }

    private static void setField(String fieldName, Object value) throws Exception {
        Field field = VartaPack.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }
}
