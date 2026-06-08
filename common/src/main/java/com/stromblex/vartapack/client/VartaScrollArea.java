package com.stromblex.vartapack.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;

public final class VartaScrollArea {
    private VartaRect bounds;
    private int contentHeight;
    private int scroll;

    public VartaScrollArea(VartaRect bounds) {
        this.bounds = bounds;
    }

    public VartaRect bounds() {
        return bounds;
    }

    public int scroll() {
        return scroll;
    }

    public int maxScroll() {
        return Math.max(0, contentHeight - bounds.height());
    }

    public boolean hasOverflow() {
        return maxScroll() > 0;
    }

    public void layout(VartaRect newBounds, int newContentHeight) {
        bounds = newBounds;
        contentHeight = Math.max(0, newContentHeight);
        scroll = VartaUiLayout.clamp(scroll, 0, maxScroll());
    }

    public boolean scrollBy(double deltaY) {
        if (!hasOverflow()) {
            return false;
        }
        scroll = VartaUiLayout.clamp(scroll - (int) (deltaY * 18), 0, maxScroll());
        return true;
    }

    public boolean contains(double mouseX, double mouseY) {
        return bounds.contains(mouseX, mouseY);
    }

    public void enableScissor(PoseStack graphics) {
        VartaScissor.enable(bounds.x(), bounds.y(), bounds.right(), bounds.bottom());
    }

    public void renderScrollbar(PoseStack graphics) {
        int maxScroll = maxScroll();
        if (maxScroll <= 0 || bounds.height() <= 0) {
            return;
        }
        int barHeight = Math.max(10, bounds.height() * bounds.height() / (maxScroll + bounds.height()));
        int barY = bounds.y() + (int) ((float) scroll / maxScroll * (bounds.height() - barHeight));
        int x = bounds.right() - 3;
        GuiComponent.fill(graphics, x, bounds.y(), x + 2, bounds.bottom(), 0xFF1B222D);
        GuiComponent.fill(graphics, x, barY, x + 2, barY + barHeight, 0xFF7E93AD);
    }
}
