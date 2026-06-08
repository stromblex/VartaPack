package com.stromblex.vartapack.fabric;

import com.stromblex.vartapack.VartaPack;
import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.client.VartaPackIssuesScreen;
import com.stromblex.vartapack.client.VartaPackToast;
import com.stromblex.vartapack.ui.CommonTexts;
import com.stromblex.vartapack.ui.IssueViewModel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public final class VartaPackFabricClient implements ClientModInitializer {

    private final FabricPlatform platform = new FabricPlatform();
    private final FabricClipboardService clipboard = new FabricClipboardService();
    private boolean startupDone = false;
    private boolean wasMouseDown = false;
    private boolean wasKeyDown = false;
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(VartaPack.MOD_ID, "main"));

    private KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        VartaPack.init(platform);

        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                CommonTexts.KEY_OPEN_SCREEN,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                KEY_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft mc) {
        if (mc.getWindow() == null) return;

        if (!startupDone) {
            if (mc.screen == null && mc.level == null) return;
            startupDone = true;
            if (tryOpenStartupIssuesScreen(mc)) return;
            handleStartupToast(mc);
        }

        if (tryOpenStartupIssuesScreen(mc)) return;

        handleKeybind(mc);

        handleToastClick(mc);
    }

    private void handleStartupToast(Minecraft mc) {
        if (!VartaPack.config().enabled()) return;

        boolean hasWarnings = VartaPack.hasIssuesAtLeast(Severity.WARNING);
        boolean hasErrors = VartaPack.hasIssuesAtLeast(Severity.ERROR);

        if (hasErrors && VartaPack.config().showScreenOnCriticalIssues()) return;

        if (hasWarnings && VartaPack.config().showToastOnStartup()) {
            IssueViewModel vm = IssueViewModel.build();
            String keyName = openKey.getTranslatedKeyMessage().getString();
            VartaPackToast.show(mc,
                    Component.translatable(CommonTexts.TOAST_TITLE),
                    Component.translatable(CommonTexts.TOAST_ISSUES_SHORT, vm.rows().size(), keyName),
                    hasErrors ? Severity.ERROR : Severity.WARNING);
        }
    }

    private boolean tryOpenStartupIssuesScreen(Minecraft mc) {
        if (VartaPack.isScreenShownThisSession()
                || !VartaPack.hasIssuesAtLeast(Severity.ERROR)
                || !VartaPack.config().enabled()
                || !VartaPack.config().showScreenOnCriticalIssues()
                || !(mc.screen instanceof TitleScreen)) {
            return false;
        }

        VartaPack.markScreenShown();
        VartaPackToast.dismiss();
        IssueViewModel vm = IssueViewModel.build();
        mc.setScreen(new VartaPackIssuesScreen(mc.screen, vm, clipboard));
        return true;
    }

    private void handleKeybind(Minecraft mc) {
        int keyCode = openKey.key.getValue();
        boolean keyDown = InputConstants.isKeyDown(mc.getWindow(), keyCode);
        if (keyDown && !wasKeyDown) {
            if (!(mc.screen instanceof VartaPackIssuesScreen)
                    && !(mc.screen != null && mc.screen.getFocused() instanceof net.minecraft.client.gui.components.EditBox)) {
                openIssuesScreen(mc);
            }
        }
        wasKeyDown = keyDown;
    }

    private void openIssuesScreen(Minecraft mc) {
        if (mc.screen instanceof VartaPackIssuesScreen) return;

        VartaPackToast.dismiss();
        IssueViewModel vm = IssueViewModel.build();
        mc.setScreen(new VartaPackIssuesScreen(mc.screen, vm, clipboard));
    }

    private void handleToastClick(Minecraft mc) {
        if (mc.screen instanceof VartaPackIssuesScreen) {
            VartaPackToast.dismiss();
            wasMouseDown = false;
            return;
        }

        if (!VartaPackToast.isToastVisible()) {
            wasMouseDown = false;
            return;
        }
        long windowHandle = mc.getWindow().handle();
        boolean mouseDown = GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (mouseDown && !wasMouseDown) {
            double rawX = mc.mouseHandler.xpos();
            double rawY = mc.mouseHandler.ypos();
            int guiW = mc.getWindow().getGuiScaledWidth();
            int screenW = mc.getWindow().getScreenWidth();
            int screenH = mc.getWindow().getScreenHeight();
            double guiX = rawX * guiW / screenW;
            double guiY = rawY * mc.getWindow().getGuiScaledHeight() / screenH;
            if (guiX >= guiW - 160 && guiY <= 32) {
                VartaPackToast.dismiss();
                openIssuesScreen(mc);
            }
        }
        wasMouseDown = mouseDown;
    }
}
