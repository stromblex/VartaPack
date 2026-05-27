package com.stromblex.vartapack.doctor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.stromblex.vartapack.config.JsonUtil;
import com.stromblex.vartapack.validation.Issue;

/**
 * CLI entry point for doctor mode. Can be run standalone:
 * {@code java -cp vartapack.jar com.stromblex.vartapack.doctor.DoctorCli --instance /path/to/.minecraft}
 */
public final class DoctorCli {

    public static void main(String[] args) {
        String instancePath = ".";
        boolean jsonOutput = false;
        boolean verbose = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--instance", "-i" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("Error: " + args[i] + " requires a path argument.");
                        printHelp();
                        System.exit(2);
                    }
                    instancePath = args[++i];
                }
                case "--json", "-j" -> jsonOutput = true;
                case "--verbose", "-v" -> verbose = true;
                case "--help", "-h" -> {
                    printHelp();
                    System.exit(0);
                }
                default -> {
                    System.err.println("Error: unknown argument '" + args[i] + "'.");
                    printHelp();
                    System.exit(2);
                }
            }
        }

        DoctorOptions options = new DoctorOptions(instancePath, jsonOutput, verbose);
        DoctorRunner runner = new DoctorRunner();
        DoctorRunner.DoctorResult result = runner.run(options);

        if (jsonOutput) {
            System.out.println(toJson(result));
        } else {
            printHuman(result, verbose);
        }

        System.exit(result.exitCode().code());
    }

    private static void printHelp() {
        System.out.println("VartaPack Doctor - Standalone instance validator");
        System.out.println();
        System.out.println("Usage: java -cp vartapack.jar com.stromblex.vartapack.doctor.DoctorCli [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --instance, -i <path>  Path to .minecraft instance directory (default: current dir)");
        System.out.println("  --json, -j             Output JSON instead of human-readable text");
        System.out.println("  --verbose, -v          Show detailed notes and checks");
        System.out.println("  --help, -h             Show this help");
        System.out.println();
        System.out.println("Exit codes:");
        System.out.println("  0  Clean (no issues or only INFO)");
        System.out.println("  1  Warnings found");
        System.out.println("  2  Errors or critical issues found");
        System.out.println();
        System.out.println("Note: Doctor mode cannot check loaded mods since Minecraft is not running.");
        System.out.println("It validates config files, integrity manifests, and analyzes crash logs.");
    }

    private static void printHuman(DoctorRunner.DoctorResult result, boolean verbose) {
        System.out.println("=== VartaPack Doctor ===");
        System.out.println("Status: " + result.status().displayName().toUpperCase());
        System.out.println();

        if (verbose) {
            System.out.println("--- Structure Checks ---");
            for (String note : result.structureNotes()) {
                System.out.println("  " + note);
            }
            System.out.println();
        }

        if (!result.issues().isEmpty()) {
            System.out.println("--- Issues (" + result.issues().size() + ") ---");
            for (Issue issue : result.issues()) {
                System.out.println("  [" + issue.severity().name() + "] " + issue.title());
                System.out.println("    " + issue.message());
                if (!issue.fixInstruction().isBlank()) {
                    System.out.println("    Fix: " + issue.fixInstruction());
                }
            }
            System.out.println();
        } else {
            System.out.println("No issues found.");
            System.out.println();
        }

        if (result.crashResult() != null && result.crashResult().hasFindings()) {
            System.out.println("--- Crash Analysis ---");
            System.out.println("  " + result.crashResult().summary());
        }

        System.out.println("Exit code: " + result.exitCode().code());
    }

    private static String toJson(DoctorRunner.DoctorResult result) {
        JsonObject root = new JsonObject();
        root.addProperty("status", result.status().name());
        root.addProperty("exitCode", result.exitCode().code());

        JsonArray issuesArr = new JsonArray();
        for (Issue issue : result.issues()) {
            JsonObject io = new JsonObject();
            io.addProperty("id", issue.id());
            io.addProperty("title", issue.title());
            io.addProperty("severity", issue.severity().name());
            io.addProperty("category", issue.category().name());
            io.addProperty("message", issue.message());
            if (!issue.fixInstruction().isBlank()) io.addProperty("fix", issue.fixInstruction());
            issuesArr.add(io);
        }
        root.add("issues", issuesArr);

        JsonArray notesArr = new JsonArray();
        for (String note : result.structureNotes()) {
            notesArr.add(note);
        }
        root.add("structureNotes", notesArr);

        if (result.crashResult() != null) {
            JsonObject crash = new JsonObject();
            crash.addProperty("summary", result.crashResult().summary());
            crash.addProperty("findingsCount", result.crashResult().findings().size());
            root.add("crashAnalysis", crash);
        }

        return JsonUtil.GSON.toJson(root);
    }
}
