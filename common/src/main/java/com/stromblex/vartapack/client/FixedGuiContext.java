package com.stromblex.vartapack.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;

/**
 * Immutable coordinate mapping for VartaPack screens rendered at a fixed GUI scale.
 */
public record FixedGuiContext(
        int width,
        int height,
        double targetGuiScale,
        double scaleCorrection,
        int framebufferWidth,
        int framebufferHeight,
        int baseLayoutWidth,
        int baseLayoutHeight,
        int layoutWidth,
        int layoutHeight,
        double contentScale,
        double offsetX,
        double offsetY,
        boolean tooSmall
) {
    public static final double DEFAULT_TARGET_GUI_SCALE = 2.0;
    public static final int DEFAULT_BASE_LAYOUT_WIDTH = 960;
    public static final int DEFAULT_BASE_LAYOUT_HEIGHT = 540;
    public static final double MIN_READABLE_SCALE = 0.50;

    public static FixedGuiContext create(double targetGuiScale, boolean enabled, int fallbackWidth, int fallbackHeight) {
        return create(targetGuiScale, enabled, fallbackWidth, fallbackHeight,
                DEFAULT_BASE_LAYOUT_WIDTH, DEFAULT_BASE_LAYOUT_HEIGHT);
    }

    public static FixedGuiContext create(double targetGuiScale, boolean enabled, int fallbackWidth, int fallbackHeight,
                                         int baseLayoutWidth, int baseLayoutHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft == null ? null : minecraft.getWindow();
        int baseWidth = Math.max(1, baseLayoutWidth);
        int baseHeight = Math.max(1, baseLayoutHeight);
        if (!enabled || window == null || targetGuiScale <= 0.0) {
            int width = Math.max(1, fallbackWidth);
            int height = Math.max(1, fallbackHeight);
            return new FixedGuiContext(
                    width,
                    height,
                    targetGuiScale <= 0.0 ? DEFAULT_TARGET_GUI_SCALE : targetGuiScale,
                    1.0,
                    width,
                    height,
                    width,
                    height,
                    width,
                    height,
                    1.0,
                    0.0,
                    0.0,
                    false);
        }

        int framebufferWidth = Math.max(1, window.getWidth());
        int framebufferHeight = Math.max(1, window.getHeight());
        int currentGuiWidth = Math.max(1, window.getGuiScaledWidth());
        int currentGuiHeight = Math.max(1, window.getGuiScaledHeight());

        int fakeWidth = Math.max(1, (int) Math.ceil(framebufferWidth / targetGuiScale));
        int fakeHeight = Math.max(1, (int) Math.ceil(framebufferHeight / targetGuiScale));
        double scaleCorrection = (double) currentGuiWidth / fakeWidth;
        if (!Double.isFinite(scaleCorrection) || scaleCorrection <= 0.0) {
            scaleCorrection = (double) currentGuiHeight / fakeHeight;
        }
        if (!Double.isFinite(scaleCorrection) || scaleCorrection <= 0.0) {
            scaleCorrection = 1.0;
        }

        boolean shouldFitDown = fakeWidth < baseWidth || fakeHeight < baseHeight;
        int layoutWidth = shouldFitDown ? baseWidth : fakeWidth;
        int layoutHeight = shouldFitDown ? baseHeight : fakeHeight;

        double contentScale = 1.0;
        if (shouldFitDown) {
            double fitScaleX = (double) fakeWidth / layoutWidth;
            double fitScaleY = (double) fakeHeight / layoutHeight;
            contentScale = Math.min(fitScaleX, fitScaleY);
            if (!Double.isFinite(contentScale) || contentScale <= 0.0) {
                contentScale = 1.0;
            }
            // Clamp to minimum readable scale — allow overflow/clipping instead of shrinking further
            contentScale = Math.max(contentScale, MIN_READABLE_SCALE);
        }

        double offsetX = (fakeWidth - layoutWidth * contentScale) / 2.0;
        double offsetY = (fakeHeight - layoutHeight * contentScale) / 2.0;
        boolean tooSmall = false;

        return new FixedGuiContext(fakeWidth, fakeHeight, targetGuiScale, scaleCorrection,
                framebufferWidth, framebufferHeight, baseWidth, baseHeight, layoutWidth, layoutHeight,
                contentScale, offsetX, offsetY, tooSmall);
    }

    public int toVirtualX(double mouseX) {
        return (int) Math.floor(mouseX / scaleCorrection);
    }

    public int toVirtualY(double mouseY) {
        return (int) Math.floor(mouseY / scaleCorrection);
    }

    public double toVirtualMouseX(double mouseX) {
        return mouseX / scaleCorrection;
    }

    public double toVirtualMouseY(double mouseY) {
        return mouseY / scaleCorrection;
    }

    public int toBaseX(double mouseX) {
        return (int) Math.floor(toBaseMouseX(mouseX));
    }

    public int toBaseY(double mouseY) {
        return (int) Math.floor(toBaseMouseY(mouseY));
    }

    public double toBaseMouseX(double mouseX) {
        return (toVirtualMouseX(mouseX) - offsetX) / contentScale;
    }

    public double toBaseMouseY(double mouseY) {
        return (toVirtualMouseY(mouseY) - offsetY) / contentScale;
    }

    public double baseToVirtualX(double x) {
        return offsetX + x * contentScale;
    }

    public double baseToVirtualY(double y) {
        return offsetY + y * contentScale;
    }
}
