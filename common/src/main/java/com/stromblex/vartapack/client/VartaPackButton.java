package com.stromblex.vartapack.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class VartaPackButton extends Button {
    public enum Style {
        PRIMARY(0xFF3E6A9F, 0xFF5484BF, 0xFFEAF3FF),
        SECONDARY(0xFF252B36, 0xFF343C4A, 0xFFE0E6F0),
        SUBTLE(0xFF1B202A, 0xFF282F3B, 0xFFCAD3DF),
        WARNING(0xFF6D521C, 0xFF8A6824, 0xFFFFE5A6),
        DISABLED(0xFF1A1D24, 0xFF1A1D24, 0xFF69717D);

        private final int base;
        private final int hover;
        private final int text;

        Style(int base, int hover, int text) {
            this.base = base;
            this.hover = hover;
            this.text = text;
        }
    }

    private final Style style;

    public VartaPackButton(int x, int y, int width, int height, Component message,
                           OnPress onPress, Style style) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.style = style;
    }

    public static VartaPackButton of(int x, int y, int width, int height, Component message,
                                     OnPress onPress, Style style) {
        return new VartaPackButton(x, y, width, height, message, onPress, style);
    }

    @Override
    protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Style resolved = this.active ? style : Style.DISABLED;
        int fill = this.isHoveredOrFocused() && this.active ? resolved.hover : resolved.base;
        int border = this.isHoveredOrFocused() && this.active ? 0xFF9DB9D8 : 0xFF3A4351;
        int x = getX();
        int y = getY();
        int right = getRight();
        int bottom = getBottom();

        g.fill(x, y, right, bottom, 0xFF0D1016);
        g.fill(x + 1, y + 1, right - 1, bottom - 1, fill);
        g.fill(x, y, right, y + 1, border);
        g.fill(x, bottom - 1, right, bottom, 0xFF11151C);
        g.fill(x, y, x + 1, bottom, border);
        g.fill(right - 1, y, right, bottom, 0xFF11151C);

        int textColor = this.active ? resolved.text : Style.DISABLED.text;
        Component message = getMessage().copy().withStyle(s -> s.withColor(textColor & 0xFFFFFF));
        renderScrollingStringOverContents(g.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE), message, 5);
    }
}
