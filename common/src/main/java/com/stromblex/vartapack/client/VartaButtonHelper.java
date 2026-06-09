package com.stromblex.vartapack.client;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class VartaButtonHelper {
    private VartaButtonHelper() {
    }

    public static Component fittingLabel(Font font, int buttonWidth, Component normal, String... compactLabels) {
        int textWidth = Math.max(8, buttonWidth - 10);
        String normalText = normal.getString();
        if (font.width(normalText) <= textWidth) {
            return normal;
        }
        for (String label : compactLabels) {
            if (font.width(label) <= textWidth) {
                return VartaComponents.literal(label);
            }
        }
        String fallback = compactLabels.length == 0 ? normalText : compactLabels[compactLabels.length - 1];
        return VartaComponents.literal(VartaTextWrapHelper.trim(font, fallback, textWidth));
    }

    public static Component fittingLabel(Font font, int buttonWidth, Component normal, List<String> compactLabels) {
        return fittingLabel(font, buttonWidth, normal, compactLabels.toArray(String[]::new));
    }
}
