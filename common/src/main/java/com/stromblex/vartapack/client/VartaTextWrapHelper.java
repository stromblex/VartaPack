package com.stromblex.vartapack.client;

import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.List;

public final class VartaTextWrapHelper {
    private VartaTextWrapHelper() {
    }

    public static String trim(Font font, String text, int pxWidth) {
        if (text == null || pxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= pxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(8, pxWidth - 12)) + "...";
    }

    public static List<String> wrap(Font font, String text, int pxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank() || pxWidth <= 0) {
            return lines;
        }

        String remaining = text.trim();
        while (!remaining.isEmpty() && (maxLines <= 0 || lines.size() < maxLines)) {
            if (font.width(remaining) <= pxWidth) {
                lines.add(remaining);
                break;
            }

            String slice = font.plainSubstrByWidth(remaining, Math.max(8, pxWidth - 8));
            if (slice.isEmpty()) {
                slice = remaining.substring(0, 1);
            }

            int breakAt = slice.lastIndexOf(' ');
            if (breakAt > 8) {
                slice = slice.substring(0, breakAt);
            }

            remaining = remaining.substring(slice.length()).trim();
            boolean lastAllowedLine = maxLines > 0 && lines.size() == maxLines - 1;
            if (lastAllowedLine && !remaining.isEmpty()) {
                lines.add(trim(font, slice + "...", pxWidth));
                break;
            }
            lines.add(slice);
        }
        return lines;
    }
}
