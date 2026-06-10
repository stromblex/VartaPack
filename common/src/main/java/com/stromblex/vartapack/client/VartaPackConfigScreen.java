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

import java.util.ArrayList;
import java.util.List;

public final class VartaPackConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 26;
    private static final int SECTION_HEADER_HEIGHT = 22;
    private static final int SECTION_PADDING = 8;
    private static final int BOTTOM_ACTION_HEIGHT = 34;

    private final Screen parent;
    private final ClipboardService clipboard;
    private final VartaScrollArea scrollArea = new VartaScrollArea(new VartaRect(0, 0, 1, 1));
    private VartaConfig draft;
    private VartaScreenMetrics metrics;
    private VartaRect frameBounds;
    private VartaRect contentBounds;
    private VartaRect bottomBounds;
    private List<SectionLayout> sectionLayouts = List.of();

    public VartaPackConfigScreen(Screen parent, ClipboardService clipboard) {
        super(VartaComponents.translatable(CommonTexts.CONFIG_TITLE));
        this.parent = parent;
        this.clipboard = clipboard;
        this.draft = VartaPack.config();
    }

    @Override
    protected void init() {
        clearWidgets();
        layoutMetrics();
        scrollArea.layout(contentBounds, computeContentHeight());
        addSectionButtons();
        addBottomButtons();
    }

    private void layoutMetrics() {
        metrics = VartaUiLayout.metrics(width, height);
        frameBounds = metrics.frame();
        int bottomReserved = bottomActionReservedHeight();
        contentBounds = metrics.contentBounds(bottomReserved + metrics.gap()).inset(0);
        bottomBounds = new VartaRect(frameBounds.x(), Math.max(contentBounds.bottom() + metrics.gap(), height - metrics.margin() - bottomReserved),
                frameBounds.width(), bottomReserved);
        sectionLayouts = layoutSections();
    }

    private int bottomActionReservedHeight() {
        return metrics != null && metrics.mode() == VartaLayoutMode.NARROW && width < 260 ? 58 : BOTTOM_ACTION_HEIGHT;
    }

    private List<SectionLayout> layoutSections() {
        List<SettingsSection> sections = sections();
        List<SectionLayout> layouts = new ArrayList<>();
        int columns = VartaUiLayout.columnsFor(metrics.mode());
        int gap = metrics.gap();
        int usableWidth = Math.max(1, contentBounds.width() - VartaUiLayout.SCROLLBAR_GUTTER);
        int sectionWidth = Math.max(1, (usableWidth - gap * (columns - 1)) / columns);
        int y = contentBounds.y();

        for (int i = 0; i < sections.size(); i += columns) {
            int rowHeight = 0;
            for (int col = 0; col < columns && i + col < sections.size(); col++) {
                SettingsSection section = sections.get(i + col);
                rowHeight = Math.max(rowHeight, sectionHeight(section));
            }
            for (int col = 0; col < columns && i + col < sections.size(); col++) {
                SettingsSection section = sections.get(i + col);
                int x = contentBounds.x() + col * (sectionWidth + gap);
                layouts.add(new SectionLayout(section, new VartaRect(x, y, sectionWidth, sectionHeight(section))));
            }
            y += rowHeight + gap;
        }
        return layouts;
    }

    private int computeContentHeight() {
        int bottom = contentBounds.y();
        for (SectionLayout layout : sectionLayouts) {
            bottom = Math.max(bottom, layout.bounds().bottom());
        }
        return Math.max(1, bottom - contentBounds.y());
    }

    private int sectionHeight(SettingsSection section) {
        return SECTION_HEADER_HEIGHT + SECTION_PADDING + section.rows().size() * ROW_HEIGHT + SECTION_PADDING;
    }

    private List<SettingsSection> sections() {
        List<SettingsSection> sections = new ArrayList<>();
        sections.add(new SettingsSection("Startup", List.of(
                toggleRow(CommonTexts.SETTING_ENABLED, List.of("Enabled"), draft.enabled(), value -> draft = new VartaConfig(
                        draft.schema(), value, draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                        draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), draft.redactUserHomePath(), draft.redactUsername(),
                        draft.strictMode(), draft.extraModsSeverity(), draft.requiredModsSeverity(), draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                        draft.fixedGuiScale(), draft.targetGuiScale())),
                toggleRow(CommonTexts.SETTING_TOAST, List.of("Toast"), draft.showToastOnStartup(), value -> draft = new VartaConfig(
                        draft.schema(), draft.enabled(), value, draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                        draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), draft.redactUserHomePath(), draft.redactUsername(),
                        draft.strictMode(), draft.extraModsSeverity(), draft.requiredModsSeverity(), draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                        draft.fixedGuiScale(), draft.targetGuiScale())),
                toggleRow(CommonTexts.SETTING_AUTO_SCREEN, List.of("Auto-open"), draft.showScreenOnCriticalIssues(), value -> draft = new VartaConfig(
                        draft.schema(), draft.enabled(), draft.showToastOnStartup(), value, draft.allowContinueAnyway(),
                        draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), draft.redactUserHomePath(), draft.redactUsername(),
                        draft.strictMode(), draft.extraModsSeverity(), draft.requiredModsSeverity(), draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                        draft.fixedGuiScale(), draft.targetGuiScale())),
                toggleRow(CommonTexts.SETTING_ALLOW_CONTINUE, List.of("Continue"), draft.allowContinueAnyway(), value -> draft = new VartaConfig(
                        draft.schema(), draft.enabled(), draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), value,
                        draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), draft.redactUserHomePath(), draft.redactUsername(),
                        draft.strictMode(), draft.extraModsSeverity(), draft.requiredModsSeverity(), draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                        draft.fixedGuiScale(), draft.targetGuiScale())),
                toggleRow(CommonTexts.SETTING_STRICT, List.of("Strict"), draft.strictMode(), value -> draft = new VartaConfig(
                        draft.schema(), draft.enabled(), draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                        draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), draft.redactUserHomePath(), draft.redactUsername(),
                        value, draft.extraModsSeverity(), draft.requiredModsSeverity(), draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                        draft.fixedGuiScale(), draft.targetGuiScale()))
        )));

        sections.add(new SettingsSection("Report & Privacy", List.of(
                toggleRow(CommonTexts.SETTING_INCLUDE_INSTALLED, List.of("Installed mods", "Mods"), draft.includeInstalledModsInReport(), value -> draft = new VartaConfig(
                        draft.schema(), draft.enabled(), draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                        value, draft.includeExtraModsInReport(), draft.redactUserHomePath(), draft.redactUsername(),
                        draft.strictMode(), draft.extraModsSeverity(), draft.requiredModsSeverity(), draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                        draft.fixedGuiScale(), draft.targetGuiScale())),
                toggleRow(CommonTexts.SETTING_INCLUDE_EXTRA, List.of("Extra mods", "Extra"), draft.includeExtraModsInReport(), value -> draft = new VartaConfig(
                        draft.schema(), draft.enabled(), draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                        draft.includeInstalledModsInReport(), value, draft.redactUserHomePath(), draft.redactUsername(),
                        draft.strictMode(), draft.extraModsSeverity(), draft.requiredModsSeverity(), draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                        draft.fixedGuiScale(), draft.targetGuiScale())),
                toggleRow(CommonTexts.SETTING_REDACT_HOME, List.of("Home path", "Home"), draft.redactUserHomePath(), value -> draft = new VartaConfig(
                        draft.schema(), draft.enabled(), draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                        draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), value, draft.redactUsername(),
                        draft.strictMode(), draft.extraModsSeverity(), draft.requiredModsSeverity(), draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                        draft.fixedGuiScale(), draft.targetGuiScale())),
                toggleRow(CommonTexts.SETTING_REDACT_USER, List.of("Username", "User"), draft.redactUsername(), value -> draft = new VartaConfig(
                        draft.schema(), draft.enabled(), draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                        draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), draft.redactUserHomePath(), value,
                        draft.strictMode(), draft.extraModsSeverity(), draft.requiredModsSeverity(), draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                        draft.fixedGuiScale(), draft.targetGuiScale()))
        )));

        sections.add(new SettingsSection("Severity Policy", List.of(
                severityRow(CommonTexts.SETTING_REQUIRED_SEVERITY, List.of("Required"), draft.requiredModsSeverity(), value -> draft = new VartaConfig(
                        draft.schema(), draft.enabled(), draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                        draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), draft.redactUserHomePath(), draft.redactUsername(),
                        draft.strictMode(), draft.extraModsSeverity(), value, draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                        draft.fixedGuiScale(), draft.targetGuiScale())),
                severityRow(CommonTexts.SETTING_BLOCKED_SEVERITY, List.of("Blocked"), draft.blockedModsSeverity(), value -> draft = new VartaConfig(
                        draft.schema(), draft.enabled(), draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                        draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), draft.redactUserHomePath(), draft.redactUsername(),
                        draft.strictMode(), draft.extraModsSeverity(), draft.requiredModsSeverity(), value, draft.recommendedModsSeverity(),
                        draft.fixedGuiScale(), draft.targetGuiScale())),
                severityRow(CommonTexts.SETTING_RECOMMENDED_SEVERITY, List.of("Recommended", "Recommend"), draft.recommendedModsSeverity(), value -> draft = new VartaConfig(
                        draft.schema(), draft.enabled(), draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                        draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), draft.redactUserHomePath(), draft.redactUsername(),
                        draft.strictMode(), draft.extraModsSeverity(), draft.requiredModsSeverity(), draft.blockedModsSeverity(), value,
                        draft.fixedGuiScale(), draft.targetGuiScale())),
                severityRow(CommonTexts.SETTING_EXTRA_SEVERITY, List.of("Extra"), draft.extraModsSeverity(), value -> draft = new VartaConfig(
                        draft.schema(), draft.enabled(), draft.showToastOnStartup(), draft.showScreenOnCriticalIssues(), draft.allowContinueAnyway(),
                        draft.includeInstalledModsInReport(), draft.includeExtraModsInReport(), draft.redactUserHomePath(), draft.redactUsername(),
                        draft.strictMode(), value, draft.requiredModsSeverity(), draft.blockedModsSeverity(), draft.recommendedModsSeverity(),
                        draft.fixedGuiScale(), draft.targetGuiScale()))
        )));

        sections.add(new SettingsSection("Tools", List.of(
                buttonRow(VartaComponents.translatable(CommonTexts.BTN_RESET_DEFAULTS), List.of("Defaults"), VartaPackButton.Style.SUBTLE, b -> {
                    draft = VartaConfig.defaults();
                    refreshWidgets();
                }),
                buttonRow(VartaComponents.translatable(CommonTexts.BTN_PROFILE_WIZARD), List.of("Wizard"), VartaPackButton.Style.SECONDARY,
                        b -> Minecraft.getInstance().setScreen(new VartaPackProfileWizardScreen(this))),
                infoRow(VartaComponents.literal("Responsive UI"), List.of("UI"), "AUTO")
        )));
        return sections;
    }

    private SettingRow toggleRow(String key, List<String> compactLabels, boolean initial, BooleanSetter setter) {
        return new SettingRow(VartaComponents.translatable(key), compactLabels, valueLabel(initial), VartaPackButton.Style.SECONDARY,
                b -> {
                    boolean next = !currentToggleValue(b.getMessage().getString());
                    setter.set(next);
                    b.setMessage(rowMessage(VartaComponents.translatable(key), compactLabels, valueLabel(next), b.getWidth()));
                });
    }

    private boolean currentToggleValue(String text) {
        return text.endsWith("ON");
    }

    private SettingRow severityRow(String key, List<String> compactLabels, Severity initial, SeveritySetter setter) {
        final Severity[] value = {initial};
        return new SettingRow(VartaComponents.translatable(key), compactLabels, value[0].name(), severityStyle(value[0]),
                b -> {
                    Severity[] values = Severity.values();
                    value[0] = values[(value[0].ordinal() + 1) % values.length];
                    setter.set(value[0]);
                    b.setMessage(rowMessage(VartaComponents.translatable(key), compactLabels, value[0].name(), b.getWidth()));
                });
    }

    private SettingRow buttonRow(Component label, List<String> compactLabels, VartaPackButton.Style style, VartaPackButton.OnPress onPress) {
        return new SettingRow(label, compactLabels, "", style, onPress);
    }

    private SettingRow infoRow(Component label, List<String> compactLabels, String value) {
        return new SettingRow(label, compactLabels, value, VartaPackButton.Style.SUBTLE, b -> {});
    }

    private String valueLabel(boolean value) {
        return value ? "ON" : "OFF";
    }

    private void addSectionButtons() {
        int scroll = scrollArea.scroll();
        for (SectionLayout layout : sectionLayouts) {
            VartaRect bounds = layout.bounds();
            int buttonX = bounds.x() + SECTION_PADDING;
            int buttonWidth = Math.max(1, bounds.width() - SECTION_PADDING * 2);
            int y = bounds.y() + SECTION_HEADER_HEIGHT + SECTION_PADDING - scroll;
            for (SettingRow row : layout.section().rows()) {
                VartaPackButton button = VartaPackButton.of(buttonX, y, buttonWidth, 22,
                        rowMessage(row.label(), row.compactLabels(), row.value(), buttonWidth),
                        row.onPress(), row.style());
                button.visible = y >= contentBounds.y() && y + 22 <= contentBounds.bottom();
                button.active = button.visible && !row.value().equals("AUTO");
                addRenderableWidget(button);
                y += ROW_HEIGHT;
            }
        }
    }

    private Component rowMessage(Component label, List<String> compactLabels, String value, int width) {
        if (value == null || value.isBlank()) {
            return VartaButtonHelper.fittingLabel(this.font, width, label, compactLabels);
        }
        List<String> candidates = new ArrayList<>();
        for (String compact : compactLabels) {
            candidates.add(compact + ": " + value);
        }
        return VartaButtonHelper.fittingLabel(this.font, width,
                VartaComponents.literal(label.getString() + ": " + value), candidates);
    }

    private void addBottomButtons() {
        int gap = metrics.gap();
        boolean stack = metrics.mode() == VartaLayoutMode.NARROW && bottomBounds.width() < 260;
        int buttonWidth = stack
                ? VartaUiLayout.buttonWidth(bottomBounds.width(), 100, 180)
                : VartaUiLayout.buttonWidth((bottomBounds.width() - gap) / 2, 100, 180);
        int x = stack ? bottomBounds.x() + (bottomBounds.width() - buttonWidth) / 2 : bottomBounds.x() + (bottomBounds.width() - buttonWidth * 2 - gap) / 2;
        int y = stack ? bottomBounds.y() + 2 : bottomBounds.y() + 5;
        addRenderableWidget(VartaPackButton.of(x, y, buttonWidth, 24,
                VartaButtonHelper.fittingLabel(this.font, buttonWidth, VartaComponents.translatable(CommonTexts.BTN_SAVE)),
                b -> saveAndClose(), VartaPackButton.Style.PRIMARY));
        int backX = stack ? x : x + buttonWidth + gap;
        int backY = stack ? y + 28 : y;
        addRenderableWidget(VartaPackButton.of(backX, backY, buttonWidth, 24,
                VartaButtonHelper.fittingLabel(this.font, buttonWidth, VartaComponents.translatable(CommonTexts.BTN_BACK)),
                b -> Minecraft.getInstance().setScreen(parent), VartaPackButton.Style.SECONDARY));
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

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBase(g);
        renderSections(g);
        scrollArea.renderScrollbar(g);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderBase(GuiGraphics g) {
        g.fill(0, 0, width, height, 0xFF05070B);
        g.fill(0, 0, width, height, 0xFF0B1018);
        g.fill(0, 0, width, 1, 0xFF566477);
        g.fill(0, metrics.headerHeight() - 6, width, metrics.headerHeight() - 5, 0xFF334050);
        g.drawCenteredString(this.font, VartaComponents.translatable(CommonTexts.CONFIG_TITLE), width / 2, 8,
                VartaUiLayout.textColor(0xFFFFFF));
        g.drawCenteredString(this.font, VartaComponents.translatable(CommonTexts.CONFIG_SUBTITLE), width / 2, 22,
                VartaUiLayout.textColor(0xD4DCE8));
        g.fill(0, bottomBounds.y() - metrics.gap() / 2, width, bottomBounds.y() - metrics.gap() / 2 + 1, 0xFF252D38);
    }

    private void renderSections(GuiGraphics g) {
        scrollArea.enableScissor(g);
        int scroll = scrollArea.scroll();
        for (SectionLayout layout : sectionLayouts) {
            VartaRect bounds = layout.bounds();
            int x = bounds.x();
            int y = bounds.y() - scroll;
            g.fill(x, y, x + bounds.width(), y + bounds.height(), 0xFF101722);
            g.fill(x, y, x + bounds.width(), y + 1, 0xFF566477);
            g.drawString(this.font, VartaTextWrapHelper.trim(this.font, layout.section().title(), bounds.width() - SECTION_PADDING * 2),
                    x + SECTION_PADDING, y + 7, VartaUiLayout.textColor(0xFFFFFF));
        }
        VartaScissor.disable(g);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (scrollArea.contains(mouseX, mouseY) && scrollArea.scrollBy(deltaY)) {
            refreshWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private interface BooleanSetter { void set(boolean value); }
    private interface SeveritySetter { void set(Severity value); }

    private record SettingsSection(String title, List<SettingRow> rows) {}
    private record SectionLayout(SettingsSection section, VartaRect bounds) {}
    private record SettingRow(Component label, List<String> compactLabels, String value, VartaPackButton.Style style,
                              VartaPackButton.OnPress onPress) {}
}
