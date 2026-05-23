package com.stromblex.vartapack.neoforge;

import com.stromblex.vartapack.api.ClipboardService;
import net.minecraft.client.Minecraft;

public final class NeoForgeClipboardService implements ClipboardService {
    @Override
    public void copy(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.keyboardHandler != null && text != null) {
            mc.keyboardHandler.setClipboard(text);
        }
    }
}
