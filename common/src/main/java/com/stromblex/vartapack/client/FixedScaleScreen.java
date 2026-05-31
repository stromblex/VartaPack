package com.stromblex.vartapack.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stromblex.vartapack.VartaPack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base class for VartaPack technical screens that should render at a stable visual scale.
 *
 * <p>The Minecraft GUI scale option is never changed. Widgets are laid out in a virtual
 * coordinate space and mouse input is converted into that same space before vanilla widget
 * dispatch runs.</p>
 */
public abstract class FixedScaleScreen extends Screen {
    private FixedGuiContext fixedContext = FixedGuiContext.create(
            FixedGuiContext.DEFAULT_TARGET_GUI_SCALE, false, 1, 1);

    protected FixedScaleScreen(Component title) {
        super(title);
    }

    @Override
    protected final void init() {
        refreshFixedContext();
        initFixed();
    }

    protected abstract void initFixed();

    protected FixedGuiContext fixedContext() {
        return fixedContext;
    }

    protected int uiWidth() {
        return fixedContext.layoutWidth();
    }

    protected int uiHeight() {
        return fixedContext.layoutHeight();
    }

    protected int baseLayoutWidth() {
        return FixedGuiContext.DEFAULT_BASE_LAYOUT_WIDTH;
    }

    protected int baseLayoutHeight() {
        return FixedGuiContext.DEFAULT_BASE_LAYOUT_HEIGHT;
    }

    protected boolean fixedGuiScaleEnabled() {
        return VartaPack.config() == null || VartaPack.config().fixedGuiScale();
    }

    protected double targetGuiScale() {
        return VartaPack.config() == null
                ? FixedGuiContext.DEFAULT_TARGET_GUI_SCALE
                : VartaPack.config().targetGuiScale();
    }

    private void refreshFixedContext() {
        fixedContext = FixedGuiContext.create(targetGuiScale(), fixedGuiScaleEnabled(), this.width, this.height,
                baseLayoutWidth(), baseLayoutHeight());
    }

    @Override
    public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refreshFixedContext();
        int fixedMouseX = fixedContext.toBaseX(mouseX);
        int fixedMouseY = fixedContext.toBaseY(mouseY);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        float correction = (float) fixedContext.scaleCorrection();
        pose.scale(correction, correction, 1.0F);
        renderViewportBackground(graphics);
        pose.translate((float) fixedContext.offsetX(), (float) fixedContext.offsetY(), 0.0F);
        float contentScale = (float) fixedContext.contentScale();
        pose.scale(contentScale, contentScale, 1.0F);
        renderFixed(graphics, fixedMouseX, fixedMouseY, partialTick);
        pose.popPose();
    }

    private void renderViewportBackground(GuiGraphics graphics) {
        graphics.fill(0, 0, fixedContext.width(), fixedContext.height(), 0xFF05070B);
    }

    private void renderTooSmallMessage(GuiGraphics graphics) {
        int centerX = fixedContext.width() / 2;
        int centerY = fixedContext.height() / 2;
        graphics.drawCenteredString(this.font, "Window too small", centerX, centerY - 12, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "Resize to view VartaPack.", centerX, centerY + 4, 0xD4DCE8);
    }

    protected abstract void renderFixed(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    protected final void renderFixedWidgets(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    protected final void enableFixedScissor(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        int guiX1 = (int) Math.floor(fixedContext.baseToVirtualX(x1) * fixedContext.scaleCorrection());
        int guiY1 = (int) Math.floor(fixedContext.baseToVirtualY(y1) * fixedContext.scaleCorrection());
        int guiX2 = (int) Math.ceil(fixedContext.baseToVirtualX(x2) * fixedContext.scaleCorrection());
        int guiY2 = (int) Math.ceil(fixedContext.baseToVirtualY(y2) * fixedContext.scaleCorrection());

        int clippedX1 = clamp(Math.min(guiX1, guiX2), 0, this.width);
        int clippedY1 = clamp(Math.min(guiY1, guiY2), 0, this.height);
        int clippedX2 = clamp(Math.max(guiX1, guiX2), 0, this.width);
        int clippedY2 = clamp(Math.max(guiY1, guiY2), 0, this.height);
        graphics.enableScissor(clippedX1, clippedY1, clippedX2, clippedY2);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    protected final void disableFixedScissor(GuiGraphics graphics) {
        graphics.disableScissor();
    }

    @Override
    public final boolean mouseClicked(double mouseX, double mouseY, int button) {
        refreshFixedContext();
        return mouseClickedFixed(fixedContext.toBaseMouseX(mouseX), fixedContext.toBaseMouseY(mouseY), button);
    }

    @Override
    public final boolean mouseReleased(double mouseX, double mouseY, int button) {
        refreshFixedContext();
        return mouseReleasedFixed(fixedContext.toBaseMouseX(mouseX), fixedContext.toBaseMouseY(mouseY), button);
    }

    @Override
    public final boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        refreshFixedContext();
        double inputScale = fixedContext.scaleCorrection() * fixedContext.contentScale();
        return mouseDraggedFixed(
                fixedContext.toBaseMouseX(mouseX),
                fixedContext.toBaseMouseY(mouseY),
                button,
                dragX / inputScale,
                dragY / inputScale);
    }

    @Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        refreshFixedContext();
        return mouseScrolledFixed(fixedContext.toBaseMouseX(mouseX), fixedContext.toBaseMouseY(mouseY), deltaX, deltaY);
    }

    protected boolean mouseClickedFixed(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected boolean mouseReleasedFixed(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    protected boolean mouseDraggedFixed(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    protected boolean mouseScrolledFixed(double mouseX, double mouseY, double deltaX, double deltaY) {
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }
}
