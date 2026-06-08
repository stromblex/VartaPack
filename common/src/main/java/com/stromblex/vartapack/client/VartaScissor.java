package com.stromblex.vartapack.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.Window;

public final class VartaScissor {
    private VartaScissor() {
    }

    public static void enable(int x, int y, int right, int bottom) {
        Window window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        int scissorX = (int) Math.floor(x * scale);
        int scissorY = (int) Math.floor((window.getGuiScaledHeight() - bottom) * scale);
        int scissorWidth = Math.max(0, (int) Math.ceil((right - x) * scale));
        int scissorHeight = Math.max(0, (int) Math.ceil((bottom - y) * scale));
        RenderSystem.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    public static void disable() {
        RenderSystem.disableScissor();
    }
}
