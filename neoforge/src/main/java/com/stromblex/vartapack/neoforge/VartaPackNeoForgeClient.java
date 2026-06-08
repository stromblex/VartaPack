package com.stromblex.vartapack.neoforge;

import com.stromblex.vartapack.VartaPack;
import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.client.VartaPackIssuesScreen;
import com.stromblex.vartapack.client.VartaPackToast;
import com.stromblex.vartapack.ui.CommonTexts;
import com.stromblex.vartapack.ui.IssueViewModel;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = VartaPack.MOD_ID, dist = Dist.CLIENT)
public final class VartaPackNeoForgeClient {

    private static boolean startupDone = false;
    private static boolean wasMouseDown = false;
    private static boolean wasKeyDown = false;
    private static final NeoForgeClipboardService CLIPBOARD = new NeoForgeClipboardService();

    private static KeyMapping openKey;

    public VartaPackNeoForgeClient(IEventBus modBus) {
        VartaPack.init(new NeoForgePlatform());
        openKey = new KeyMapping(
                CommonTexts.KEY_OPEN_SCREEN,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                CommonTexts.KEY_CATEGORY
        );
        modBus.addListener(VartaPackNeoForgeClient::registerKeys);
        NeoForge.EVENT_BUS.register(VartaPackNeoForgeClient.class);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(openKey);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) return;

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

    private static void handleStartupToast(Minecraft mc) {
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

    private static boolean tryOpenStartupIssuesScreen(Minecraft mc) {
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
        mc.setScreen(new VartaPackIssuesScreen(mc.screen, vm, CLIPBOARD));
        return true;
    }

    private static void handleKeybind(Minecraft mc) {
        long window = mc.getWindow().getWindow();
        int keyCode = openKey.getKey().getValue();
        boolean keyDown = InputConstants.isKeyDown(window, keyCode);
        if (keyDown && !wasKeyDown) {
            if (!(mc.screen instanceof VartaPackIssuesScreen)
                    && !(mc.screen != null && mc.screen.getFocused() instanceof net.minecraft.client.gui.components.EditBox)) {
                openIssuesScreen(mc);
            }
        }
        wasKeyDown = keyDown;
    }

    private static void openIssuesScreen(Minecraft mc) {
        if (mc.screen instanceof VartaPackIssuesScreen) return;

        VartaPackToast.dismiss();
        IssueViewModel vm = IssueViewModel.build();
        mc.setScreen(new VartaPackIssuesScreen(mc.screen, vm, CLIPBOARD));
    }

    private static void handleToastClick(Minecraft mc) {
        if (mc.screen instanceof VartaPackIssuesScreen) {
            VartaPackToast.dismiss();
            wasMouseDown = false;
            return;
        }

        if (!VartaPackToast.isToastVisible()) {
            wasMouseDown = false;
            return;
        }
        long windowHandle = mc.getWindow().getWindow();
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
