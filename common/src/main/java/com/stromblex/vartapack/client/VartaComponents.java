package com.stromblex.vartapack.client;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

public final class VartaComponents {
    private VartaComponents() {
    }

    public static MutableComponent empty() {
        return new TextComponent("");
    }

    public static MutableComponent literal(String text) {
        return new TextComponent(text);
    }

    public static MutableComponent translatable(String key, Object... args) {
        return new TranslatableComponent(key, args);
    }
}
