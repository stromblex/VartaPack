package com.stromblex.vartapack.config;

public record ModRule(String id, String name, String requiredVersion, String reason) {
    public ModRule {
        if (id == null) id = "";
        if (name == null || name.isBlank()) name = id;
        if (requiredVersion == null) requiredVersion = "";
        if (reason == null) reason = "";
    }
}
