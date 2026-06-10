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
    private static final int BOTTOM_ACTION_HEIGHT = 34;
    private static final int BOTTOM_CONTENT_GAP = 18;
    private static final int FIELD_ROW_NORMAL = 28;
    private static final int FIELD_ROW_NARROW = 42;

    private final Screen parent;
    private final VartaScrollArea scrollArea = new VartaScrollArea(new VartaRect(0, 0, 1, 1));
    private EditBox packId;
    private EditBox packName;
    private EditBox profileVersion;
    private EditBox supportUrl;
    private List<String> allowedMods = List.of();
    private boolean scanned;
    private VartaScreenMetrics metrics;
    private VartaRect frameBounds;
    private VartaRect contentBounds;
    private VartaRect bottomBounds;

    public VartaPackProfileWizardScreen(Screen parent) {
        super(VartaComponents.translatable(CommonTexts.PROFILE_WIZARD_TITLE));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        PackProfile profile = VartaPack.profile();
        layoutMetrics();
        scrollArea.layout(contentBounds, contentHeight());
        addFields(profile);
        if (!scanned) {
            scanInstalledMods();
            scanned = true;
        }
        addBottomButtons();
    }

    private void layoutMetrics() {
        metrics = VartaUiLayout.metrics(width, height);
        frameBounds = metrics.frame();
        int bottomReserved = bottomActionReservedHeight();
        int contentY = frameBounds.y() + metrics.headerHeight() + metrics.margin();
        int bottomY = Math.max(contentY + 1, height - metrics.margin() - bottomReserved);
        int contentBottom = Math.max(contentY + 1, bottomY - BOTTOM_CONTENT_GAP);
        contentBounds = new VartaRect(frameBounds.x() + metrics.margin(), contentY,
                Math.max(1, frameBounds.width() - metrics.margin() * 2), contentBottom - contentY);
        bottomBounds = new VartaRect(frameBounds.x(), bottomY, frameBounds.width(), bottomReserved);
    }

    private int bottomActionReservedHeight() {
        return metrics != null && metrics.mode() == VartaLayoutMode.NARROW && width < 360 ? 86 : BOTTOM_ACTION_HEIGHT;
    }

    private int contentHeight() {
        int row = metrics.mode() == VartaLayoutMode.NARROW ? FIELD_ROW_NARROW : FIELD_ROW_NORMAL;
        return SECTION_TOP_PADDING() + row * 4 + 54;
    }

    private int SECTION_TOP_PADDING() {
        return metrics.mode() == VartaLayoutMode.NARROW ? 6 : 12;
    }

    private void addFields(PackProfile profile) {
        int scroll = scrollArea.scroll();
        int row = metrics.mode() == VartaLayoutMode.NARROW ? FIELD_ROW_NARROW : FIELD_ROW_NORMAL;
        int x = contentBounds.x() + 14;
        int y = contentBounds.y() + SECTION_TOP_PADDING() - scroll;
        int labelWidth = metrics.mode() == VartaLayoutMode.NARROW ? contentBounds.width() - 28 : Math.min(130, contentBounds.width() / 3);
        int inputX = metrics.mode() == VartaLayoutMode.NARROW ? x : x + labelWidth;
        int inputYShift = metrics.mode() == VartaLayoutMode.NARROW ? 14 : 0;
        int inputWidth = Math.max(40, contentBounds.right() - inputX - 14);

        packId = editBox(inputX, y + inputYShift, inputWidth, profile.packId().isBlank() ? "my-modpack" : profile.packId());
        packName = editBox(inputX, y + row + inputYShift, inputWidth, profile.packName().isBlank() ? "My Modpack" : profile.packName());
        profileVersion = editBox(inputX, y + row * 2 + inputYShift, inputWidth, profile.profileVersion().isBlank() ? "1.0.0" : profile.profileVersion());
        supportUrl = editBox(inputX, y + row * 3 + inputYShift, inputWidth, profile.supportUrl());
    }

    private EditBox editBox(int x, int y, int width, String value) {
        EditBox box = new EditBox(this.font, x, y, width, 20, VartaComponents.empty());
        box.setMaxLength(256);
        box.setValue(value == null ? "" : value);
        box.visible = y >= contentBounds.y() && y + 20 <= contentBounds.bottom();
        box.active = box.visible;
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

    private void addBottomButtons() {
        int gap = metrics.gap();
        boolean stack = metrics.mode() == VartaLayoutMode.NARROW && bottomBounds.width() < 360;
        int buttonCount = 3;
        int buttonWidth = stack
                ? VartaUiLayout.buttonWidth(bottomBounds.width(), 100, 160)
                : VartaUiLayout.buttonWidth((bottomBounds.width() - gap * (buttonCount - 1)) / buttonCount, 100, 160);
        int x = stack ? bottomBounds.x() + (bottomBounds.width() - buttonWidth) / 2
                : bottomBounds.x() + (bottomBounds.width() - buttonWidth * buttonCount - gap * (buttonCount - 1)) / 2;
        int y = stack ? bottomBounds.y() + 1 : bottomBounds.y() + 5;

        addRenderableWidget(VartaPackButton.of(x, y, buttonWidth, 24,
                VartaButtonHelper.fittingLabel(this.font, buttonWidth, VartaComponents.translatable(CommonTexts.BTN_SCAN_PROFILE), "Scan"),
                b -> {
                    scanInstalledMods();
                    rebuildResponsiveWidgets();
                }, VartaPackButton.Style.SECONDARY));
        int saveX = stack ? x : x + buttonWidth + gap;
        int saveY = stack ? y + 28 : y;
        addRenderableWidget(VartaPackButton.of(saveX, saveY, buttonWidth, 24,
                VartaButtonHelper.fittingLabel(this.font, buttonWidth, VartaComponents.translatable(CommonTexts.BTN_SAVE_PROFILE), "Save"),
                b -> saveProfile(), VartaPackButton.Style.PRIMARY));
        int backX = stack ? x : x + (buttonWidth + gap) * 2;
        int backY = stack ? y + 56 : y;
        addRenderableWidget(VartaPackButton.of(backX, backY, buttonWidth, 24,
                VartaButtonHelper.fittingLabel(this.font, buttonWidth, VartaComponents.translatable(CommonTexts.BTN_BACK)),
                b -> Minecraft.getInstance().setScreen(parent), VartaPackButton.Style.SECONDARY));
    }

    private void rebuildResponsiveWidgets() {
        clearWidgets();
        init();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBase(g);
        renderContent(g);
        scrollArea.renderScrollbar(g);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderBase(GuiGraphics g) {
        g.fill(0, 0, width, height, 0xFF05070B);
        g.fill(0, 0, width, height, 0xFF0B1018);
        g.fill(0, 0, width, 1, 0xFF566477);
        g.fill(0, metrics.headerHeight() - 6, width, metrics.headerHeight() - 5, 0xFF334050);
        g.drawCenteredString(this.font, VartaComponents.translatable(CommonTexts.PROFILE_WIZARD_TITLE), width / 2, 8,
                VartaUiLayout.textColor(0xFFFFFF));
        g.drawCenteredString(this.font, VartaComponents.translatable(CommonTexts.PROFILE_WIZARD_SUBTITLE), width / 2, 22,
                VartaUiLayout.textColor(0xD4DCE8));
        g.fill(0, bottomBounds.y() - metrics.gap() / 2, width, bottomBounds.y() - metrics.gap() / 2 + 1, 0xFF252D38);
    }

    private void renderContent(GuiGraphics g) {
        scrollArea.enableScissor(g);
        int scroll = scrollArea.scroll();
        int row = metrics.mode() == VartaLayoutMode.NARROW ? FIELD_ROW_NARROW : FIELD_ROW_NORMAL;
        int x = contentBounds.x() + 14;
        int y = contentBounds.y() + SECTION_TOP_PADDING() - scroll;
        int labelWidth = metrics.mode() == VartaLayoutMode.NARROW ? contentBounds.width() - 28 : Math.min(130, contentBounds.width() / 3);

        drawLabel(g, CommonTexts.PROFILE_FIELD_ID, x, y, labelWidth);
        drawLabel(g, CommonTexts.PROFILE_FIELD_NAME, x, y + row, labelWidth);
        drawLabel(g, CommonTexts.PROFILE_FIELD_VERSION, x, y + row * 2, labelWidth);
        drawLabel(g, CommonTexts.PROFILE_FIELD_SUPPORT, x, y + row * 3, labelWidth);

        int infoY = y + row * 4 + 16;
        int textWidth = Math.max(40, contentBounds.width() - 28 - VartaUiLayout.SCROLLBAR_GUTTER);
        for (String line : VartaTextWrapHelper.wrap(this.font,
                VartaComponents.translatable(CommonTexts.PROFILE_SCAN_SUMMARY, allowedMods.size()).getString(), textWidth, 2)) {
            drawVisibleLine(g, line, x, infoY, VartaUiLayout.textColor(0xFFFFFF));
            infoY += 10;
        }
        infoY += 4;
        for (String line : VartaTextWrapHelper.wrap(this.font,
                VartaComponents.translatable(CommonTexts.PROFILE_SCAN_HINT).getString(), textWidth, 3)) {
            drawVisibleLine(g, line, x, infoY, VartaUiLayout.textColor(0xD4DCE8));
            infoY += 10;
        }
        VartaScissor.disable(g);
    }

    private void drawLabel(GuiGraphics g, String key, int x, int y, int width) {
        int labelY = metrics.mode() == VartaLayoutMode.NARROW ? y : y + 6;
        drawVisibleLine(g, VartaTextWrapHelper.trim(this.font, VartaComponents.translatable(key).getString(), width),
                x, labelY, VartaUiLayout.textColor(0xFFFFFF));
    }

    private void drawVisibleLine(GuiGraphics g, String text, int x, int y, int color) {
        if (y < contentBounds.y() || y + this.font.lineHeight > contentBounds.bottom()) {
            return;
        }
        g.drawString(this.font, text, x, y, color);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (scrollArea.contains(mouseX, mouseY) && scrollArea.scrollBy(deltaY)) {
            rebuildResponsiveWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
