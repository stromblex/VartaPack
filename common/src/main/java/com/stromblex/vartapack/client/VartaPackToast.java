package com.stromblex.vartapack.client;

import com.stromblex.vartapack.VartaPack;
import com.stromblex.vartapack.check.Severity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

/**
 * Thin wrapper around vanilla {@link SystemToast} so we get a familiar look
 * without shipping a custom texture or re-implementing the {@code Toast} interface.
 * Also tracks toast visibility so loaders can detect clicks in the toast area.
 */
public final class VartaPackToast {
    private VartaPackToast() {}

    private static volatile long visibleUntil = 0;
    private static final long TOAST_DURATION_MS = 8000;

    public static void show(Minecraft mc, Component title, Component message, Severity severity) {
        if (mc == null || mc.getToastManager() == null) return;
        try {
            SystemToast.SystemToastId id = switch (severity) {
                case CRITICAL, ERROR -> SystemToast.SystemToastId.WORLD_BACKUP;
                case WARNING, INFO -> SystemToast.SystemToastId.PERIODIC_NOTIFICATION;
            };
            SystemToast.addOrUpdate(mc.getToastManager(), id, title, message);
            visibleUntil = System.currentTimeMillis() + TOAST_DURATION_MS;
        } catch (Throwable t) {
            VartaPack.LOGGER.warn("Failed to show VartaPack toast: {}", t.toString());
        }
    }

    /**
     * Returns true if our toast is currently visible on screen.
     * Used by loader tick handlers to detect clicks in the toast region.
     */
    public static boolean isToastVisible() {
        return System.currentTimeMillis() < visibleUntil;
    }

    public static void dismiss() {
        visibleUntil = 0;
    }
}
