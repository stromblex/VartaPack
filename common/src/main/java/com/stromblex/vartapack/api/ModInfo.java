package com.stromblex.vartapack.api;

public record ModInfo(String id, String name, String version, String description) {
    public ModInfo {
        if (id == null) id = "";
        if (name == null) name = id;
        if (version == null) version = "";
        if (description == null) description = "";
    }
}
