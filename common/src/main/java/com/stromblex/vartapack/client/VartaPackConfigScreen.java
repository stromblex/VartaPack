package com.stromblex.vartapack.client;

import com.stromblex.vartapack.VartaPack;
import com.stromblex.vartapack.api.ClipboardService;
import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.config.ConfigManager;
import com.stromblex.vartapack.config.VartaConfig;
import com.stromblex.vartapack.ui.CommonTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class VartaPackConfigScreen extends FixedScaleScreen {
    private final Screen parent;
    private final ClipboardService clipboard;
    private VartaConfig draft;
        private int panelX;
        private int panelWidth;
        private int sectionY;
        private int sectionWidth;
        private int sectionGap;

    public VartaPackConfigScreen(Screen parent, ClipboardService clipboard) {
        super(Component.translatable(CommonTexts.CONFIG_TITLE));
        this.parent = parent;
        this.clipboard = clipboard;
        this.draft = VartaPack.config();
    }

    @Override
        protected void initFixed() {
        layoutMetrics();
        int x = panelX + 18;
        int reportX = sectionX(1);
        int severityX = sectionX(2);
        int y = sectionY + 24;
        int row = 26;

        addToggle(x, y, sectionWidth, CommonTexts.SETTING_ENABLED, draft.enabled(), value -> draft = new VartaConfig(
                draft.schema(), value, draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), draft.redactUserHomePath(), draft.redactUsername(),
                draft.strictMode(), draft.extraModsSeverity(), draft.requiredModsSeverity(), draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                draft.fixedGuiScale(), draft.targetGuiScale()));
        addToggle(x, y + row, sectionWidth, CommonTexts.SETTING_TOAST, draft.showToastOnStartup(), value -> draft = new VartaConfig(
                draft.schema(), draft.enabled(), value, draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), draft.redactUserHomePath(), draft.redactUsername(),
                draft.strictMode(), draft.extraModsSeverity(), draft.requiredModsSeverity(), draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                draft.fixedGuiScale(), draft.targetGuiScale()));
        addToggle(x, y + row * 2, sectionWidth, CommonTexts.SETTING_AUTO_SCREEN, draft.showScreenOnCriticalIssues(), value -> draft = new VartaConfig(
                draft.schema(), draft.enabled(), draft.showToastOnStartup(), value, draft.allowContinueAnyway(),
                draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), draft.redactUserHomePath(), draft.redactUsername(),
                draft.strictMode(), draft.extraModsSeverity(), draft.requiredModsSeverity(), draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                draft.fixedGuiScale(), draft.targetGuiScale()));
        addToggle(x, y + row * 3, sectionWidth, CommonTexts.SETTING_ALLOW_CONTINUE, draft.allowContinueAnyway(), value -> draft = new VartaConfig(
                draft.schema(), draft.enabled(), draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), value,
                draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), draft.redactUserHomePath(), draft.redactUsername(),
                draft.strictMode(), draft.extraModsSeverity(), draft.requiredModsSeverity(), draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                draft.fixedGuiScale(), draft.targetGuiScale()));
        addToggle(x, y + row * 4, sectionWidth, CommonTexts.SETTING_STRICT, draft.strictMode(), value -> draft = new VartaConfig(
                draft.schema(), draft.enabled(), draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), draft.redactUserHomePath(), draft.redactUsername(),
                value, draft.extraModsSeverity(), draft.requiredModsSeverity(), draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                draft.fixedGuiScale(), draft.targetGuiScale()));

        addToggle(reportX, y, sectionWidth, CommonTexts.SETTING_INCLUDE_INSTALLED, draft.includeInstalledModsInReport(), value -> draft = new VartaConfig(
                draft.schema(), draft.enabled(), draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                value, draft.includeExtraModsInReport(), draft.redactUserHomePath(), draft.redactUsername(),
                draft.strictMode(), draft.extraModsSeverity(), draft.requiredModsSeverity(), draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                draft.fixedGuiScale(), draft.targetGuiScale()));
        addToggle(reportX, y + row, sectionWidth, CommonTexts.SETTING_INCLUDE_EXTRA, draft.includeExtraModsInReport(), value -> draft = new VartaConfig(
                draft.schema(), draft.enabled(), draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                draft.includeInstalledModsInReport(), value, draft.redactUserHomePath(), draft.redactUsername(),
                draft.strictMode(), draft.extraModsSeverity(), draft.requiredModsSeverity(), draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                draft.fixedGuiScale(), draft.targetGuiScale()));
        addToggle(reportX, y + row * 2, sectionWidth, CommonTexts.SETTING_REDACT_HOME, draft.redactUserHomePath(), value -> draft = new VartaConfig(
                draft.schema(), draft.enabled(), draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), value, draft.redactUsername(),
                draft.strictMode(), draft.extraModsSeverity(), draft.requiredModsSeverity(), draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                draft.fixedGuiScale(), draft.targetGuiScale()));
        addToggle(reportX, y + row * 3, sectionWidth, CommonTexts.SETTING_REDACT_USER, draft.redactUsername(), value -> draft = new VartaConfig(
                draft.schema(), draft.enabled(), draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), draft.redactUserHomePath(), value,
                draft.strictMode(), draft.extraModsSeverity(), draft.requiredModsSeverity(), draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                draft.fixedGuiScale(), draft.targetGuiScale()));

        addSeverity(severityX, y, sectionWidth, CommonTexts.SETTING_REQUIRED_SEVERITY, draft.requiredModsSeverity(), value -> draft = new VartaConfig(
                draft.schema(), draft.enabled(), draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), draft.redactUserHomePath(), draft.redactUsername(),
                draft.strictMode(), draft.extraModsSeverity(), value, draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                draft.fixedGuiScale(), draft.targetGuiScale()));
        addSeverity(severityX, y + row, sectionWidth, CommonTexts.SETTING_BLOCKED_SEVERITY, draft.blockedModsSeverity(), value -> draft = new VartaConfig(
                draft.schema(), draft.enabled(), draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), draft.redactUserHomePath(), draft.redactUsername(),
                draft.strictMode(), draft.extraModsSeverity(), draft.requiredModsSeverity(), value, draft.recommendedModsSeverity(),
                draft.fixedGuiScale(), draft.targetGuiScale()));
        addSeverity(severityX, y + row * 2, sectionWidth, CommonTexts.SETTING_RECOMMENDED_SEVERITY, draft.recommendedModsSeverity(), value -> draft = new VartaConfig(
                draft.schema(), draft.enabled(), draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), draft.redactUserHomePath(), draft.redactUsername(),
                draft.strictMode(), draft.extraModsSeverity(), draft.requiredModsSeverity(), draft.blockedModsSeverity(), value,
                draft.fixedGuiScale(), draft.targetGuiScale()));
        addSeverity(severityX, y + row * 3, sectionWidth, CommonTexts.SETTING_EXTRA_SEVERITY, draft.extraModsSeverity(), value -> draft = new VartaConfig(
                draft.schema(), draft.enabled(), draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), draft.redactUserHomePath(), draft.redactUsername(),
                draft.strictMode(), value, draft.requiredModsSeverity(), draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                draft.fixedGuiScale(), draft.targetGuiScale()));

        // 4th column: Tools (Reset + Profile Wizard)
        int toolsX = sectionX(3);
        addRenderableWidget(VartaPackButton.of(toolsX, y, sectionWidth, 22, Component.translatable(CommonTexts.BTN_RESET_DEFAULTS), b -> {
            draft = VartaConfig.defaults();
            refreshWidgets();
        }, VartaPackButton.Style.SUBTLE));
        addRenderableWidget(VartaPackButton.of(toolsX, y + row, sectionWidth, 22,
                Component.translatable(CommonTexts.BTN_PROFILE_WIZARD),
                b -> Minecraft.getInstance().setScreen(new VartaPackProfileWizardScreen(this)), VartaPackButton.Style.SECONDARY));
        addToggle(toolsX, y + row * 2, sectionWidth, CommonTexts.SETTING_FIXED_GUI_SCALE, draft.fixedGuiScale(), value -> draft = new VartaConfig(
                draft.schema(), draft.enabled(), draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), draft.redactUserHomePath(), draft.redactUsername(),
                draft.strictMode(), draft.extraModsSeverity(), draft.requiredModsSeverity(), draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                value, draft.targetGuiScale()));

        // Bottom: Save + Back
        int bottomY = uiHeight() - 43;
        int buttonWidth = 124;
        int total = buttonWidth * 2 + 6;
        int startX = (uiWidth() - total) / 2;
        addRenderableWidget(VartaPackButton.of(startX, bottomY, buttonWidth, 24,
                Component.translatable(CommonTexts.BTN_SAVE), b -> saveAndClose(), VartaPackButton.Style.PRIMARY));
        addRenderableWidget(VartaPackButton.of(startX + buttonWidth + 6, bottomY, buttonWidth, 24,
                Component.translatable(CommonTexts.BTN_BACK), b -> Minecraft.getInstance().setScreen(parent), VartaPackButton.Style.SECONDARY));
    }

    private void layoutMetrics() {
        panelWidth = Math.min(1080, Math.max(420, uiWidth() - 40));
        panelX = (uiWidth() - panelWidth) / 2;
        sectionGap = 16;
        sectionWidth = (panelWidth - 36 - sectionGap * 3) / 4;
        sectionY = 70;
    }

    private int sectionX(int index) {
        return panelX + 18 + index * (sectionWidth + sectionGap);
    }

    private void addToggle(int x, int y, int width, String key, boolean initial, BooleanSetter setter) {
        final boolean[] value = {initial};
        VartaPackButton button = VartaPackButton.of(x, y, width, 22, toggleMessage(key, value[0]), b -> {
            value[0] = !value[0];
            setter.set(value[0]);
            b.setMessage(toggleMessage(key, value[0]));
        }, value[0] ? VartaPackButton.Style.SECONDARY : VartaPackButton.Style.SUBTLE);
        addRenderableWidget(button);
    }

    private void addSeverity(int x, int y, int width, String key, Severity initial, SeveritySetter setter) {
        final Severity[] value = {initial};
        VartaPackButton button = VartaPackButton.of(x, y, width, 22, severityMessage(key, value[0]), b -> {
            Severity[] values = Severity.values();
            value[0] = values[(value[0].ordinal() + 1) % values.length];
            setter.set(value[0]);
            b.setMessage(severityMessage(key, value[0]));
        }, severityStyle(value[0]));
        addRenderableWidget(button);
    }

    private Component toggleMessage(String key, boolean value) {
        return Component.translatable(CommonTexts.SETTING_FORMAT,
                Component.translatable(key),
                Component.translatable(value ? CommonTexts.STATE_ON : CommonTexts.STATE_OFF));
    }

    private Component severityMessage(String key, Severity value) {
        return Component.translatable(CommonTexts.SETTING_FORMAT,
                Component.translatable(key), Component.literal(value.name()));
    }

        private VartaPackButton.Style severityStyle(Severity severity) {
                return severity.ordinal() >= Severity.ERROR.ordinal()
                                ? VartaPackButton.Style.WARNING
                                : VartaPackButton.Style.SECONDARY;
        }

    private void saveAndClose() {
        if (VartaPack.platform() != null) {
            new ConfigManager(VartaPack.platform().getGameDirectory()).saveVartaConfig(draft);
            VartaPack.reload();
        }
        Minecraft.getInstance().setScreen(parent);
    }

        private void refreshWidgets() {
        clearWidgets();
        init();
    }

    @Override    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

        @Override    protected void renderFixed(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                                g.fill(0, 0, uiWidth(), uiHeight(), 0xFF05070B);
                                g.fill(panelX - 10, 12, panelX + panelWidth + 10, uiHeight() - 14, 0xFF0B1018);
                g.fill(panelX - 10, 12, panelX + panelWidth + 10, 13, 0xFF566477);
                g.fill(panelX - 10, 52, panelX + panelWidth + 10, 53, 0xFF334050);

                                g.drawCenteredString(this.font, Component.translatable(CommonTexts.CONFIG_TITLE), uiWidth() / 2, 20, 0xFFFFFF);
                                g.drawCenteredString(this.font, Component.translatable(CommonTexts.CONFIG_SUBTITLE), uiWidth() / 2, 34, 0xD4DCE8);
                renderSection(g, 0, "Startup");
                renderSection(g, 1, "Report & Privacy");
                renderSection(g, 2, "Severity Policy");
                renderSection(g, 3, "Tools");
                renderFixedWidgets(g, mouseX, mouseY, partialTick);
    }

        private void renderSection(GuiGraphics g, int index, String title) {
                int x = sectionX(index);
                int y = sectionY;
                g.fill(x, y, x + sectionWidth, y + 1, 0xFF566477);
                g.drawString(this.font, Component.literal(title), x, y + 7, 0xFFFFFF, true);
        }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private interface BooleanSetter { void set(boolean value); }
    private interface SeveritySetter { void set(Severity value); }
}