package com.stromblex.vartapack.crash;

import com.stromblex.vartapack.check.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class CrashAnalyzerTest {

    @TempDir
    Path gameDir;

    @Test
    void detectsMixinApplyFailed() throws Exception {
        Path logDir = gameDir.resolve("logs");
        Files.createDirectories(logDir);
        Files.writeString(logDir.resolve("latest.log"),
                "2024-01-01 [ERROR] Mixin apply for mod_x failed due to incompatible targets\n");

        CrashAnalyzer analyzer = new CrashAnalyzer();
        CrashAnalysisResult result = analyzer.analyze(gameDir);

        assertTrue(result.hasFindings());
        assertTrue(result.findings().stream()
                .anyMatch(f -> f.patternId().equals("mixin.apply.failed")));
    }

    @Test
    void detectsOutOfMemory() throws Exception {
        Path logDir = gameDir.resolve("logs");
        Files.createDirectories(logDir);
        Files.writeString(logDir.resolve("latest.log"),
                "java.lang.OutOfMemoryError: Java heap space\n");

        CrashAnalyzer analyzer = new CrashAnalyzer();
        CrashAnalysisResult result = analyzer.analyze(gameDir);

        assertTrue(result.hasFindings());
        CrashFinding finding = result.findings().stream()
                .filter(f -> f.patternId().equals("out.of.memory"))
                .findFirst().orElse(null);
        assertNotNull(finding);
        assertEquals(Severity.CRITICAL, finding.severity());
        assertTrue(finding.confidence() >= 0.9f);
    }

    @Test
    void detectsClassNotFoundException() throws Exception {
        Path logDir = gameDir.resolve("logs");
        Files.createDirectories(logDir);
        Files.writeString(logDir.resolve("latest.log"),
                "java.lang.ClassNotFoundException: com.example.MissingClass\n");

        CrashAnalyzer analyzer = new CrashAnalyzer();
        CrashAnalysisResult result = analyzer.analyze(gameDir);

        assertTrue(result.hasFindings());
        assertTrue(result.findings().stream()
                .anyMatch(f -> f.patternId().equals("class.not.found")));
    }

    @Test
    void detectsJavaVersionMismatch() throws Exception {
        Path crashDir = gameDir.resolve("crash-reports");
        Files.createDirectories(crashDir);
        Files.writeString(crashDir.resolve("crash-2024-01-01.txt"),
                "class file version 65.0 was compiled by a more recent version of the JVM\n");

        CrashAnalyzer analyzer = new CrashAnalyzer();
        CrashAnalysisResult result = analyzer.analyze(gameDir);

        assertTrue(result.hasFindings());
        assertTrue(result.findings().stream()
                .anyMatch(f -> f.patternId().equals("java.version.mismatch")));
    }

    @Test
    void detectsDuplicateMod() throws Exception {
        Path logDir = gameDir.resolve("logs");
        Files.createDirectories(logDir);
        Files.writeString(logDir.resolve("latest.log"),
                "DuplicateModsFoundException: found duplicate mod xyz\n");

        CrashAnalyzer analyzer = new CrashAnalyzer();
        CrashAnalysisResult result = analyzer.analyze(gameDir);

        assertTrue(result.hasFindings());
        assertTrue(result.findings().stream()
                .anyMatch(f -> f.patternId().equals("duplicate.mod")));
    }

    @Test
    void noLogsReturnsEmpty() {
        CrashAnalyzer analyzer = new CrashAnalyzer();
        CrashAnalysisResult result = analyzer.analyze(gameDir);

        assertFalse(result.hasFindings());
        assertFalse(result.hasCrashReports());
        assertFalse(result.hasLatestLog());
    }

    @Test
    void cleanLogReturnsNoFindings() throws Exception {
        Path logDir = gameDir.resolve("logs");
        Files.createDirectories(logDir);
        Files.writeString(logDir.resolve("latest.log"),
                "[INFO] Game started successfully\n[INFO] Loading mods...\n[INFO] Done!\n");

        CrashAnalyzer analyzer = new CrashAnalyzer();
        CrashAnalysisResult result = analyzer.analyze(gameDir);

        assertFalse(result.hasFindings());
        assertTrue(result.hasLatestLog());
    }

    @Test
    void findingConfidenceLabels() {
        CrashFinding high = new CrashFinding("test", "T", Severity.ERROR, 0.95f, "", "", "", "");
        assertEquals("High confidence", high.confidenceLabel());

        CrashFinding likely = new CrashFinding("test", "T", Severity.ERROR, 0.7f, "", "", "", "");
        assertEquals("Likely cause", likely.confidenceLabel());

        CrashFinding possible = new CrashFinding("test", "T", Severity.ERROR, 0.4f, "", "", "", "");
        assertEquals("Possible cause", possible.confidenceLabel());

        CrashFinding pattern = new CrashFinding("test", "T", Severity.ERROR, 0.2f, "", "", "", "");
        assertEquals("Detected pattern", pattern.confidenceLabel());
    }
}
