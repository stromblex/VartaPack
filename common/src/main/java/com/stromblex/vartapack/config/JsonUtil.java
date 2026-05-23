package com.stromblex.vartapack.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Centralized Gson instance with pretty-printing for stable on-disk config files.
 */
public final class JsonUtil {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();

    private JsonUtil() {}
}
