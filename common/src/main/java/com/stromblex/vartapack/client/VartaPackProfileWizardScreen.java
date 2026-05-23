package com.stromblex.vartapack.client;

import com.stromblex.vartapack.VartaPack;
import com.stromblex.vartapack.api.EnvironmentInfo;
import com.stromblex.vartapack.api.ModInfo;
import com.stromblex.vartapack.check.ExtraModsCheck;
import com.stromblex.vartapack.config.ConfigManager;
import com.stromblex.vartapack.config.PackProfile;
import com.stromblex.vartapack.ui.CommonTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class VartaPackProfileWizardScreen extends Screen {
    private final Screen parent;
    private EditBox packId;
    private EditBox packName;
    private EditBox profileVersion;
    private EditBox supportUrl;
    private List<String> allowedMods = List.of();
    private int panelX;
    private int panelWidth;

    public VartaPackProfileWizardScreen(Screen parent) {
        super(Component.translatable(CommonTexts.PROFILE_WIZARD_TITLE));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        PackProfile profile = VartaPack.profile();
        layoutMetrics();
        int x = panelX + 24;
        int y = 58;
        int inputWidth = panelWidth - 150;

        packId = editBox(x + 140, y, inputWidth, profile.packId().isBlank() ? "my-modpack" : profile.packId());
        packName = editBox(x + 140, y + 26, inputWidth, profile.packName().isBlank() ? "My Modpack" : profile.packName());
        profileVersion = editBox(x + 140, y + 52, inputWidth, profile.profileVersion().isBlank() ? "1.0.0" : profile.profileVersion());
        supportUrl = editBox(x + 140, y + 78, inputWidth, profile.supportUrl());

        scanInstalledMods();

        int buttonWidth = 150;
        int bottomY = this.height - 38;
        int total = buttonWidth * 3 + 12;
        int startX = (this.width - total) / 2;
        addRenderableWidget(VartaPackButton.of(startX, bottomY, buttonWidth, 24,
            Component.translatable(CommonTexts.BTN_SCAN_PROFILE), b -> scanInstalledMods(), VartaPackButton.Style.SECONDARY));
        addRenderableWidget(VartaPackButton.of(startX + buttonWidth + 6, bottomY, buttonWidth, 24,
            Component.translatable(CommonTexts.BTN_SAVE_PROFILE), b -> saveProfile(), VartaPackButton.Style.PRIMARY));
        addRenderableWidget(VartaPackButton.of(startX + (buttonWidth + 6) * 2, bottomY, buttonWidth, 24,
            Component.translatable(CommonTexts.BTN_BACK), b -> Minecraft.getInstance().setScreen(parent), VartaPackButton.Style.SECONDARY));
    }

        private void layoutMetrics() {
        panelWidth = Math.min(660, Math.max(320, this.width - 40));
        panelX = (this.width - panelWidth) / 2;
        }

    private EditBox editBox(int x, int y, int width, String value) {
        EditBox box = new EditBox(this.font, x, y, width, 20, Component.empty());
        box.setMaxLength(256);
        box.setValue(value == null ? "" : value);
        addRenderableWidget(box);
        return box;
    }

    private void scanInstalledMods() {
        if (VartaPack.platform() == null) {
            allowedMods = List.of();
            return;
        }
        allowedMods = VartaPack.platform().getInstalledMods().stream()
                .map(ModInfo::id)
                .filter(id -> id != null && !id.isBlank())
                .map(id -> id.toLowerCase(Locale.ROOT))
                .filter(id -> !ExtraModsCheck.isPlatformModId(id))
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private void saveProfile() {
        if (VartaPack.platform() == null) return;
        EnvironmentInfo env = EnvironmentInfo.capture(VartaPack.platform());
        PackProfile existing = VartaPack.profile();
        PackProfile generated = new PackProfile(
                1,
                packId.getValue().trim(),
                packName.getValue().trim(),
                profileVersion.getValue().trim(),
                supportUrl.getValue().trim(),
                existing.homepageUrl(),
                List.of(env.minecraftVersion()),
                List.of(env.loaderName().toLowerCase(Locale.ROOT)),
                Math.max(1, env.javaMajor()),
                existing.minimumRamMb(),
                existing.recommendedRamMb(),
                existing.requiredMods(),
                existing.recommendedMods(),
                existing.blockedMods(),
                allowedMods
        );
        new ConfigManager(VartaPack.platform().getGameDirectory()).savePackProfile(generated);
        VartaPack.reload();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xFF05070B);
        g.fill(panelX - 10, 12, panelX + panelWidth + 10, this.height - 14, 0xFF0B1018);
        g.fill(panelX - 10, 12, panelX + panelWidth + 10, 13, 0xFF566477);
        g.fill(panelX - 10, 52, panelX + panelWidth + 10, 53, 0xFF334050);
        g.drawCenteredString(this.font, Component.translatable(CommonTexts.PROFILE_WIZARD_TITLE), this.width / 2, 20, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable(CommonTexts.PROFILE_WIZARD_SUBTITLE), this.width / 2, 34, 0xD4DCE8);

        int x = panelX + 24;
        int y = 64;
        drawLabel(g, CommonTexts.PROFILE_FIELD_ID, x, y);
        drawLabel(g, CommonTexts.PROFILE_FIELD_NAME, x, y + 26);
        drawLabel(g, CommonTexts.PROFILE_FIELD_VERSION, x, y + 52);
        drawLabel(g, CommonTexts.PROFILE_FIELD_SUPPORT, x, y + 78);

        int infoY = y + 118;
        g.drawString(this.font,
                Component.translatable(CommonTexts.PROFILE_SCAN_SUMMARY, allowedMods.size()),
            x, infoY, 0xFFFFFF, true);
        g.drawString(this.font,
                Component.translatable(CommonTexts.PROFILE_SCAN_HINT),
            x, infoY + 14, 0xD4DCE8, true);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawLabel(GuiGraphics g, String key, int x, int y) {
        g.drawString(this.font, Component.translatable(key), x, y + 6, 0xFFFFFF, true);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}