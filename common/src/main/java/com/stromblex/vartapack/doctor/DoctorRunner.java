package com.stromblex.vartapack.doctor;

import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.config.ConfigManager;
import com.stromblex.vartapack.config.PackProfile;
import com.stromblex.vartapack.config.VartaConfig;
import com.stromblex.vartapack.crash.CrashAnalysisResult;
import com.stromblex.vartapack.crash.CrashAnalyzer;
import com.stromblex.vartapack.integrity.IntegrityLoader;
import com.stromblex.vartapack.integrity.IntegrityManifest;
import com.stromblex.vartapack.rules.RulesConfig;
import com.stromblex.vartapack.rules.RulesLoader;
import com.stromblex.vartapack.validation.Issue;
import com.stromblex.vartapack.validation.PackStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Standalone doctor validation runner. Checks an instance directory without
 * requiring Minecraft to be running. Uses only file-system checks and config parsing.
 *
 * <p>This does NOT have access to the mod loader's mod list (since Minecraft isn't running).
 * It can check:</p>
 * <ul>
 *   <li>Config file presence and validity</li>
 *   <li>Rules file validity</li>
 *   <li>Integrity manifest checks</li>
 *   <li>Mods folder presence</li>
 *   <li>Crash report analysis</li>
 *   <li>Basic structural checks</li>
 * </ul>
 */
public final class DoctorRunner {

    public record DoctorResult(
            DoctorExitCode exitCode,
            PackStatus status,
            List<Issue> issues,
            CrashAnalysisResult crashResult,
            List<String> structureNotes
    ) {}

    public DoctorResult run(DoctorOptions options) {
        Path instanceDir = Path.of(options.instancePath()).toAbsolutePath();
        List<Issue> issues = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        // Check instance directory exists
        if (!Files.isDirectory(instanceDir)) {
            issues.add(Issue.builder("doctor.no_instance_dir")
                    .title("Instance directory not found")
                    .severity(Severity.CRITICAL)
                    .message("Directory does not exist: " + instanceDir)
                    .fix("Verify the instance path is correct.")
                    .blocksContinue(true)
                    .build());
            return new DoctorResult(DoctorExitCode.ERRORS, PackStatus.BROKEN, issues, null, notes);
        }

        // Check mods folder
        Path modsDir = instanceDir.resolve("mods");
        if (!Files.isDirectory(modsDir)) {
            notes.add("WARN: mods/ directory not found");
            issues.add(Issue.builder("doctor.no_mods_dir")
                    .title("Mods directory not found")
                    .severity(Severity.WARNING)
                    .message("No mods/ directory found in instance.")
                    .fix("Create the mods/ directory or verify this is the correct instance path.")
                    .build());
        } else {
            notes.add("OK: mods/ directory exists");
        }

        // Check config/vartapack directory
        Path configDir = instanceDir.resolve("config").resolve("vartapack");
        if (!Files.isDirectory(configDir)) {
            notes.add("WARN: config/vartapack/ directory not found");
            issues.add(Issue.builder("doctor.no_config_dir")
                    .title("VartaPack config directory not found")
                    .severity(Severity.WARNING)
                    .message("No config/vartapack/ directory found.")
                    .fix("Create config/vartapack/ and add profile.json.")
                    .build());
        } else {
            notes.add("OK: config/vartapack/ directory exists");

            // Check profile.json
            ConfigManager mgr = new ConfigManager(instanceDir);
            if (Files.exists(mgr.packProfilePath())) {
                notes.add("OK: profile.json exists");
                String parseError = validateJsonFile(mgr.packProfilePath());
                if (parseError != null) {
                    issues.add(Issue.builder("doctor.invalid_profile")
                            .title("Invalid profile.json")
                            .severity(Severity.ERROR)
                            .message("profile.json could not be parsed: " + parseError)
                            .fix("Fix the JSON syntax in config/vartapack/profile.json.")
                            .blocksContinue(true)
                            .build());
                } else {
                    try {
                        PackProfile profile = mgr.loadPackProfile();
                        notes.add("OK: profile.json is valid (pack: " + profile.packName() + ")");
                    } catch (Exception e) {
                        issues.add(Issue.builder("doctor.invalid_profile")
                                .title("Invalid profile.json")
                                .severity(Severity.ERROR)
                                .message("profile.json could not be parsed: " + e.getMessage())
                                .fix("Fix the JSON syntax in config/vartapack/profile.json.")
                                .blocksContinue(true)
                                .build());
                    }
                }
            } else {
                notes.add("WARN: profile.json not found");
            }

            // Check vartapack.json
            if (Files.exists(mgr.vartaConfigPath())) {
                notes.add("OK: vartapack.json exists");
                String parseError = validateJsonFile(mgr.vartaConfigPath());
                if (parseError != null) {
                    issues.add(Issue.builder("doctor.invalid_config")
                            .title("Invalid vartapack.json")
                            .severity(Severity.ERROR)
                            .message("vartapack.json could not be parsed: " + parseError)
                            .fix("Fix the JSON syntax in config/vartapack/vartapack.json.")
                            .blocksContinue(true)
                            .build());
                } else {
                    try {
                        mgr.loadVartaConfig();
                        notes.add("OK: vartapack.json is valid");
                    } catch (Exception e) {
                        issues.add(Issue.builder("doctor.invalid_config")
                                .title("Invalid vartapack.json")
                                .severity(Severity.ERROR)
                                .message("vartapack.json could not be parsed: " + e.getMessage())
                                .fix("Fix the JSON syntax in config/vartapack/vartapack.json.")
                                .blocksContinue(true)
                                .build());
                    }
                }
            } else {
                notes.add("INFO: vartapack.json not found (defaults will be used)");
            }

            // Check rules.json
            Path rulesPath = configDir.resolve("rules.json");
            if (Files.exists(rulesPath)) {
                notes.add("OK: rules.json exists");
                RulesConfig rules = RulesLoader.load(configDir);
                if (rules.rules().isEmpty() && rules.conflicts().isEmpty()) {
                    notes.add("INFO: rules.json is empty or has no rules");
                } else {
                    notes.add("OK: rules.json has " + rules.rules().size() + " rules, "
                            + rules.conflicts().size() + " conflicts");
                }
            }

            // Check integrity.json
            Path integrityPath = configDir.resolve("integrity.json");
            if (Files.exists(integrityPath)) {
                notes.add("OK: integrity.json exists");
                IntegrityManifest manifest = IntegrityLoader.load(configDir);
                if (!manifest.files().isEmpty()) {
                    notes.add("OK: integrity.json has " + manifest.files().size() + " file entries");
                    // Run integrity checks
                    var checker = new com.stromblex.vartapack.integrity.IntegrityChecker();
                    List<Issue> intIssues = checker.check(manifest, instanceDir);
                    issues.addAll(intIssues);
                }
            }
        }

        // Check crash reports
        CrashAnalyzer analyzer = new CrashAnalyzer();
        CrashAnalysisResult crashResult = analyzer.analyze(instanceDir);
        if (crashResult.hasFindings()) {
            notes.add("WARN: Crash patterns detected (" + crashResult.findings().size() + " findings)");
            for (var finding : crashResult.findings()) {
                issues.add(Issue.builder("doctor.crash." + finding.patternId())
                        .title(finding.confidenceLabel() + ": " + finding.title())
                        .severity(finding.severity())
                        .message(finding.explanation())
                        .fix(finding.suggestedFix())
                        .build());
            }
        } else {
            if (crashResult.hasCrashReports() || crashResult.hasLatestLog()) {
                notes.add("OK: No known crash patterns in logs");
            }
        }

        // Check logs directory
        Path logsDir = instanceDir.resolve("logs");
        if (Files.isDirectory(logsDir)) {
            notes.add("OK: logs/ directory exists");
            if (Files.exists(logsDir.resolve("latest.log"))) {
                notes.add("OK: logs/latest.log exists");
            }
        }

        // Compute status and exit code
        PackStatus status = computeStatus(issues);
        DoctorExitCode exitCode = computeExitCode(issues);

        return new DoctorResult(exitCode, status, issues, crashResult, notes);
    }

    private PackStatus computeStatus(List<Issue> issues) {
        boolean hasCritical = false, hasError = false, hasWarning = false;
        for (Issue issue : issues) {
            switch (issue.severity()) {
                case CRITICAL -> hasCritical = true;
                case ERROR -> hasError = true;
                case WARNING -> hasWarning = true;
                default -> {}
            }
        }
        if (hasCritical || hasError) return PackStatus.BROKEN;
        if (hasWarning) return PackStatus.UNSUPPORTED;
        return PackStatus.CLEAN;
    }

    private DoctorExitCode computeExitCode(List<Issue> issues) {
        boolean hasError = false, hasWarning = false;
        for (Issue issue : issues) {
            if (issue.severity() == Severity.CRITICAL || issue.severity() == Severity.ERROR) hasError = true;
            if (issue.severity() == Severity.WARNING) hasWarning = true;
        }
        if (hasError) return DoctorExitCode.ERRORS;
        if (hasWarning) return DoctorExitCode.WARNINGS;
        return DoctorExitCode.OK;
    }

    /**
     * Pre-validate a JSON file independently of {@link ConfigManager} (which auto-recovers
     * broken files into defaults). Returns the parse error message, or {@code null} if valid.
     */
    private static String validateJsonFile(Path path) {
        try {
            String text = Files.readString(path);
            if (text.isBlank()) return "file is empty";
            com.google.gson.JsonElement el = com.google.gson.JsonParser.parseString(text);
            if (!el.isJsonObject()) return "top-level value is not a JSON object";
            return null;
        } catch (com.google.gson.JsonSyntaxException e) {
            return e.getMessage();
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
