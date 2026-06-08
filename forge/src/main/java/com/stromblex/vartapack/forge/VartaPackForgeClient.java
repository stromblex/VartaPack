package com.stromblex.vartapack.forge;

import com.mojang.blaze3d.platform.InputConstants;
import com.stromblex.vartapack.VartaPack;
import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.client.VartaPackIssuesScreen;
import com.stromblex.vartapack.client.VartaPackToast;
import com.stromblex.vartapack.client.VartaComponents;
import com.stromblex.vartapack.ui.CommonTexts;
import com.stromblex.vartapack.ui.IssueViewModel;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public final class VartaPackForgeClient {

    private static boolean startupDone = false;
    private static boolean wasMouseDown = false;
    private static boolean wasKeyDown = false;
    private static final ForgeClipboardService CLIPBOARD = new ForgeClipboardService();
    private static final String KEY_CATEGORY = "key.categories.vartapack.main";

    private static KeyMapping openKey;

    private VartaPackForgeClient() {
    }

    public static void init() {
        openKey = new KeyMapping(
                CommonTexts.KEY_OPEN_SCREEN,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                KEY_CATEGORY
        );
        ClientRegistry.registerKeyBinding(openKey);
        MinecraftForge.EVENT_BUS.register(VartaPackForgeClient.class);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) return;

        if (!startupDone) {
            if (!(mc.screen instanceof TitleScreen)) return;
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
                    VartaComponents.translatable(CommonTexts.TOAST_TITLE),
                    VartaComponents.translatable(CommonTexts.TOAST_ISSUES_SHORT, vm.rows().size(), keyName),
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
        boolean consumed = false;
        while (openKey.consumeClick()) {
            consumed = true;
            if (canOpenIssuesScreen(mc)) {
                openIssuesScreen(mc);
            }
        }
        if (consumed) {
            wasKeyDown = true;
            return;
        }

        int keyCode = openKey.getKey().getValue();
        boolean keyDown = InputConstants.isKeyDown(mc.getWindow().getWindow(), keyCode);
        if (keyDown && !wasKeyDown && canOpenIssuesScreen(mc)) {
            openIssuesScreen(mc);
        }
        wasKeyDown = keyDown;
    }

    private static boolean canOpenIssuesScreen(Minecraft mc) {
        return mc.screen instanceof TitleScreen;
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

        if (!VartaPackToast.isToastVisible() || !canOpenIssuesScreen(mc)) {
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
