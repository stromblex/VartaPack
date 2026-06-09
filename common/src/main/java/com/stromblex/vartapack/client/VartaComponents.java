package com.stromblex.vartapack.client;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

public final class VartaComponents {
    private VartaComponents() {
    }

    public static MutableComponent empty() {
        return Component.empty();
    }

    public static MutableComponent literal(String text) {
        return Component.literal(text);
    }

    public static MutableComponent translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }
}
