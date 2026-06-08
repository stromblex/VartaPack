package com.stromblex.vartapack.crash;

import com.stromblex.vartapack.VartaPack;
import com.stromblex.vartapack.check.Severity;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes crash reports and log files for common error patterns.
 * Converts detected patterns into understandable findings with suggested fixes.
 *
 * <p><b>Important:</b> The analyzer does not claim certainty. Findings use
 * confidence levels and wording like "Likely cause" or "Possible cause".</p>
 */
public final class CrashAnalyzer {

    private static final int MAX_LOG_LINES = 5000;
    private static final int MAX_EXCERPT_LENGTH = 200;

    private static final List<CrashPattern> PATTERNS = List.of(
            new CrashPattern("mixin.apply.failed",
                    "Mixin injection failure",
                    Severity.CRITICAL, "(?i)mixin apply .* failed",
                    "A Mixin injection failed. This usually means two mods are trying to modify the same code in incompatible ways.",
                    "Remove one of the conflicting mods or update them to compatible versions.", 0.85f),

            new CrashPattern("mod.resolution.incompatible",
                    "Incompatible mod set",
                    Severity.CRITICAL, "(?i)mod resolution encountered an incompatible mod set",
                    "The mod loader could not resolve a compatible set of mods. One or more mods have conflicting dependencies.",
                    "Check the full error for which mods conflict, then remove or update the incompatible ones.", 0.9f),

            new CrashPattern("class.not.found",
                    "Missing class (ClassNotFoundException)",
                    Severity.ERROR, "(?i)java\\.lang\\.ClassNotFoundException:\\s*(\\S+)",
                    "A required class was not found. This usually means a dependency mod is missing or the wrong version is installed.",
                    "Install the missing dependency mod or update to compatible versions.", 0.7f),

            new CrashPattern("no.class.def",
                    "Missing class definition (NoClassDefFoundError)",
                    Severity.ERROR, "(?i)java\\.lang\\.NoClassDefFoundError:\\s*(\\S+)",
                    "A class definition was not found at runtime. This often indicates a missing or outdated dependency.",
                    "Ensure all required mods and their dependencies are installed at compatible versions.", 0.7f),

            new CrashPattern("incompatible.class.change",
                    "Binary incompatibility (IncompatibleClassChangeError)",
                    Severity.ERROR, "(?i)java\\.lang\\.IncompatibleClassChangeError",
                    "A mod was compiled against a different version of a class than what is currently loaded. This usually means version mismatches between mods.",
                    "Update all mods to versions compatible with each other and the current Minecraft version.", 0.75f),

            new CrashPattern("java.version.mismatch",
                    "Java version mismatch",
                    Severity.CRITICAL, "(?i)(class file version \\d+\\.\\d+|UnsupportedClassVersionError|has been compiled by a more recent version)",
                    "The game or a mod requires a newer Java version than what is currently running.",
                    "Update to the Java version required by the modpack.", 0.9f),

            new CrashPattern("loader.dependency.error",
                    "Loader dependency resolution failure",
                    Severity.CRITICAL, "(?i)(requires .* version|dependency .* not met|unmet dependency|missing dependency)",
                    "A mod's required dependency is not installed or has the wrong version.",
                    "Install the missing dependency or update to a compatible version.", 0.7f),

            new CrashPattern("duplicate.mod",
                    "Duplicate mod detected",
                    Severity.ERROR, "(?i)(duplicate mod|found duplicate|DuplicateModsFoundException)",
                    "The same mod was found multiple times (likely different versions in the mods folder).",
                    "Remove duplicate mod jars from the mods folder, keeping only one version.", 0.85f),

            new CrashPattern("wrong.environment",
                    "Wrong environment / wrong side",
                    Severity.ERROR, "(?i)(client-only mod on server|server-only mod on client|wrong environment|dist DEDICATED_SERVER)",
                    "A mod designed for one environment (client/server) was loaded in the wrong one.",
                    "Remove the mod or move it to the correct environment.", 0.8f),

            new CrashPattern("out.of.memory",
                    "Out of memory",
                    Severity.CRITICAL, "(?i)(OutOfMemoryError|java\\.lang\\.OutOfMemoryError|Failed to allocate)",
                    "The game ran out of memory. The allocated RAM may be insufficient for this modpack.",
                    "Increase allocated RAM in your launcher settings. Most modpacks need at least 4-6 GB.", 0.9f),

            new CrashPattern("renderer.conflict",
                    "Renderer/shader conflict",
                    Severity.ERROR, "(?i)(shader.*conflict|optifine.*sodium|iris.*optifine|render.*pipeline.*error)",
                    "A conflict between rendering or shader mods was detected.",
                    "Use only one rendering optimization mod. Remove OptiFine if using Sodium/Iris.", 0.65f),

            new CrashPattern("missing.method",
                    "Missing method (NoSuchMethodError)",
                    Severity.ERROR, "(?i)java\\.lang\\.NoSuchMethodError",
                    "A method expected by one mod does not exist. This usually means version incompatibility.",
                    "Update all mods to mutually compatible versions.", 0.7f),

            new CrashPattern("missing.field",
                    "Missing field (NoSuchFieldError)",
                    Severity.ERROR, "(?i)java\\.lang\\.NoSuchFieldError",
                    "A field expected by one mod does not exist. This indicates a version mismatch between mods.",
                    "Update all mods to mutually compatible versions.", 0.7f),

            new CrashPattern("stackoverflow",
                    "Stack overflow",
                    Severity.ERROR, "(?i)java\\.lang\\.StackOverflowError",
                    "An infinite recursion occurred. This may be caused by a mod bug or circular dependency.",
                    "Check recent mod updates and revert if this started after an update.", 0.5f),

            new CrashPattern("access.denied",
                    "File access denied",
                    Severity.WARNING, "(?i)(AccessDeniedException|access.*denied|permission.*denied)",
                    "A file could not be accessed. This may be caused by permissions, antivirus, or another process locking the file.",
                    "Close other programs that might be using the game files, or check file permissions.", 0.6f)
    );

    /**
     * Analyze crash reports and log files in the game directory.
     */
    public CrashAnalysisResult analyze(Path gameDir) {
        List<CrashFinding> findings = new ArrayList<>();
        boolean hasCrashReports = false;
        boolean hasLatestLog = false;

        // Check crash-reports directory
        Path crashDir = gameDir.resolve("crash-reports");
        if (Files.isDirectory(crashDir)) {
            Path latestCrash = findLatestFile(crashDir, "*.txt");
            if (latestCrash != null) {
                hasCrashReports = true;
                analyzeFile(latestCrash, findings);
            }
        }

        // Check logs/latest.log
        Path latestLog = gameDir.resolve("logs").resolve("latest.log");
        if (Files.exists(latestLog)) {
            hasLatestLog = true;
            analyzeFile(latestLog, findings);
        }

        // Check logs/debug.log
        Path debugLog = gameDir.resolve("logs").resolve("debug.log");
        if (Files.exists(debugLog)) {
            analyzeFile(debugLog, findings);
        }

        // Deduplicate by pattern ID
        findings = deduplicateFindings(findings);

        // Sort by severity descending, then confidence descending
        findings.sort(
                Comparator.comparingInt((CrashFinding f) -> f.severity().ordinal()).reversed()
                        .thenComparing(Comparator.comparingDouble(CrashFinding::confidence).reversed()));

        String summary = buildSummary(findings, hasCrashReports, hasLatestLog);
        return new CrashAnalysisResult(findings, hasCrashReports, hasLatestLog, summary);
    }

    private void analyzeFile(Path file, List<CrashFinding> findings) {
        try {
            List<String> lines = Files.readAllLines(file);
            if (lines.size() > MAX_LOG_LINES) {
                lines = lines.subList(lines.size() - MAX_LOG_LINES, lines.size());
            }
            String content = String.join("\n", lines);
            analyzeContent(content, findings);
        } catch (IOException e) {
            VartaPack.LOGGER.debug("Could not read file for crash analysis: {}", file);
        }
    }

    private void analyzeContent(String content, List<CrashFinding> findings) {
        for (CrashPattern pattern : PATTERNS) {
            if (pattern.regex().isBlank()) continue;
            try {
                Pattern compiled = Pattern.compile(pattern.regex());
                Matcher matcher = compiled.matcher(content);
                if (matcher.find()) {
                    String excerpt = extractExcerpt(content, matcher.start(), matcher.end());
                    findings.add(new CrashFinding(
                            pattern.id(),
                            pattern.title(),
                            pattern.severity(),
                            pattern.confidence(),
                            pattern.explanation(),
                            pattern.suggestedFix(),
                            excerpt,
                            ""
                    ));
                }
            } catch (Exception e) {
                VartaPack.LOGGER.debug("Crash pattern '{}' failed: {}", pattern.id(), e.getMessage());
            }
        }
    }

    private String extractExcerpt(String content, int matchStart, int matchEnd) {
        int start = Math.max(0, content.lastIndexOf('\n', matchStart - 1) + 1);
        int end = content.indexOf('\n', matchEnd);
        if (end < 0) end = content.length();
        String line = content.substring(start, Math.min(end, start + MAX_EXCERPT_LENGTH));
        return line.trim();
    }

    private Path findLatestFile(Path dir, String glob) {
        Path latest = null;
        long latestTime = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, glob)) {
            for (Path file : stream) {
                long modified = Files.getLastModifiedTime(file).toMillis();
                if (modified > latestTime) {
                    latestTime = modified;
                    latest = file;
                }
            }
        } catch (IOException e) {
            VartaPack.LOGGER.debug("Could not scan directory: {}", dir);
        }
        return latest;
    }

    private List<CrashFinding> deduplicateFindings(List<CrashFinding> findings) {
        List<CrashFinding> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (CrashFinding f : findings) {
            if (seen.add(f.patternId())) {
                result.add(f);
            }
        }
        return result;
    }

    private String buildSummary(List<CrashFinding> findings, boolean hasCrash, boolean hasLog) {
        if (findings.isEmpty()) {
            if (!hasCrash && !hasLog) return "No crash reports or logs found to analyze.";
            return "No known error patterns detected in available logs.";
        }
        int critical = 0, error = 0;
        for (CrashFinding f : findings) {
            if (f.severity() == Severity.CRITICAL) critical++;
            else if (f.severity() == Severity.ERROR) error++;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(findings.size()).append(" pattern(s) detected");
        if (critical > 0) sb.append(" (").append(critical).append(" critical)");
        if (error > 0) sb.append(" (").append(error).append(" errors)");
        sb.append(".");
        return sb.toString();
    }
}
