package com.stromblex.vartapack.client;

import net.minecraft.client.gui.GuiGraphics;

public final class VartaScissor {
    private VartaScissor() {
    }

    public static void enable(GuiGraphics graphics, int x, int y, int right, int bottom) {
        graphics.enableScissor(x, y, right, bottom);
    }

    public static void disable(GuiGraphics graphics) {
        graphics.disableScissor();
    }
}
