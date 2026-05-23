package com.stromblex.vartapack.fabric;

import com.stromblex.vartapack.api.ClipboardService;
import net.minecraft.client.Minecraft;

public final class FabricClipboardService implements ClipboardService {
    @Override
    public void copy(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.keyboardHandler != null && text != null) {
            mc.keyboardHandler.setClipboard(text);
        }
    }
}
