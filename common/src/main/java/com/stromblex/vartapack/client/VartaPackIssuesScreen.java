package com.stromblex.vartapack.client;

import com.stromblex.vartapack.VartaPack;
import com.stromblex.vartapack.api.ClipboardService;
import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.ui.CommonTexts;
import com.stromblex.vartapack.ui.IssueViewModel;
import com.stromblex.vartapack.util.UrlUtil;
import com.stromblex.vartapack.validation.PackStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Vanilla-styled, loader-agnostic VartaPack issues screen.
 * Lives in common source dir; both Fabric and Forge compile it via {@code srcDir}.
 */
public final class VartaPackIssuesScreen extends Screen {
    private static final int MIN_ACTION_PANEL_WIDTH = 150;
    private static final int NORMAL_ACTION_PANEL_MIN_WIDTH = 220;
    private static final int RIGHT_PANEL_MAX_WIDTH = 360;
    private static final int MIN_ISSUE_LIST_WIDTH = 260;
    private static final int ACTION_BUTTON_MIN_WIDTH = 140;
    private static final int ACTION_BUTTON_MAX_WIDTH = 260;
    private static final int MIN_STATUS_WIDTH = 220;
    private static final int MIN_BUTTON_COLUMN_WIDTH = 150;
    private static final int ACTION_COLUMN_GAP = 16;
    private static final int ACTION_GROUP_GAP = 12;
    private static final int ACTION_SECTION_LABEL_HEIGHT = 12;
    private static final int FIX_PREFIX_WIDTH_FALLBACK = 42;

    private enum ActionKind { COPY_REPORT, COPY_JSON, OPEN_DIR, OPEN_SUPPORT, CONTINUE, SETTINGS }
    private enum NarrowTab { ISSUES, ACTIONS }

    private final Screen parent;
    private final IssueViewModel vm;
    private final ClipboardService clipboard;

    private int issueScroll;
    private int maxIssueScroll;
    private int actionScroll;
    private int maxActionScroll;
    private int edgePadding;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentBottom;
    private int headerTop;
    private int headerBottom;
    private int listX;
    private int listWidth;
    private int listTop;
    private int listBottom;
    private int actionX;
    private int actionWidth;
    private int actionTop;
    private int actionBottom;
    private int actionButtonTop;
    private int actionButtonBottom;
    private int actionStatusHeight;
    private int actionStatusX;
    private int actionStatusY;
    private int actionStatusWidth;
    private int actionStatusBlockHeight;
    private boolean actionButtonsScroll;
    private VartaLayoutMode layoutMode = VartaLayoutMode.NORMAL;
    private NarrowTab narrowTab = NarrowTab.ISSUES;
    private int tabY;
    private int tabHeight;
    private int issuesTabX;
    private int actionsTabX;
    private int tabWidth;

    public VartaPackIssuesScreen(Screen parent, IssueViewModel vm, ClipboardService clipboard) {
        super(VartaComponents.translatable(CommonTexts.SCREEN_TITLE));
        this.parent = parent;
        this.vm = vm;
        this.clipboard = clipboard;
    }

    @Override
    protected void init() {
        clearWidgets();
        List<ActionSpec> actions = buildActions();
        layoutMetrics();

        int viewportHeight = Math.max(1, listBottom - listTop);
        maxIssueScroll = Math.max(0, computeIssueContentHeight() - viewportHeight);
        issueScroll = VartaUiLayout.clamp(issueScroll, 0, maxIssueScroll);

        if (layoutMode != VartaLayoutMode.NARROW || narrowTab == NarrowTab.ACTIONS) {
            addActionButtons(actions);
        }
    }

    private List<ActionSpec> buildActions() {
        List<ActionSpec> actions = new ArrayList<>();

        actions.add(new ActionSpec(ActionKind.COPY_REPORT,
                VartaComponents.translatable(CommonTexts.BTN_COPY_REPORT),
                b -> {
                    clipboard.copy(vm.buildMarkdownReport());
                    showCopiedToast();
                },
                VartaPackButton.Style.PRIMARY));

        actions.add(new ActionSpec(ActionKind.COPY_JSON,
                VartaComponents.translatable(CommonTexts.BTN_COPY_JSON),
                b -> {
                    clipboard.copy(vm.buildJsonReport());
                    showCopiedToast();
                },
                VartaPackButton.Style.SECONDARY));

        if (VartaPack.platform() != null) {
            actions.add(new ActionSpec(ActionKind.OPEN_DIR,
                    VartaComponents.translatable(CommonTexts.BTN_OPEN_GAME_DIR),
                    b -> Util.getPlatform().openFile(VartaPack.platform().getGameDirectory().toFile()),
                    VartaPackButton.Style.SECONDARY));
        }

        if (UrlUtil.isSafeWebUrl(vm.supportUrl())) {
            actions.add(new ActionSpec(ActionKind.OPEN_SUPPORT,
                    VartaComponents.translatable(CommonTexts.BTN_OPEN_SUPPORT),
                    b -> Util.getPlatform().openUri(URI.create(vm.supportUrl())),
                    VartaPackButton.Style.SUBTLE));
        }

        if (!VartaPack.shouldBlockContinue()) {
            actions.add(new ActionSpec(ActionKind.CONTINUE,
                    VartaComponents.translatable(CommonTexts.BTN_CONTINUE),
                    b -> onClose(),
                    VartaPackButton.Style.SUBTLE));
        }

        actions.add(new ActionSpec(ActionKind.SETTINGS,
                VartaComponents.translatable(CommonTexts.BTN_SETTINGS),
                b -> Minecraft.getInstance().setScreen(new VartaPackConfigScreen(this, clipboard)),
                VartaPackButton.Style.SUBTLE));

        return actions;
    }

    private void showCopiedToast() {
        VartaPackToast.show(
                Minecraft.getInstance(),
                VartaComponents.translatable(CommonTexts.TOAST_TITLE),
                VartaComponents.translatable(CommonTexts.REPORT_COPIED),
                Severity.INFO);
    }

    private void layoutMetrics() {
        layoutMode = chooseLayoutMode(Math.max(1, width - VartaUiLayout.EDGE_PADDING_NORMAL * 2));
        edgePadding = VartaUiLayout.edgePadding(layoutMode);
        contentX = edgePadding;
        contentWidth = Math.max(1, width - edgePadding * 2);
        layoutMode = chooseLayoutMode(contentWidth);
        edgePadding = VartaUiLayout.edgePadding(layoutMode);
        contentX = edgePadding;
        contentWidth = Math.max(1, width - edgePadding * 2);
        headerTop = 8;
        headerBottom = getHeaderHeight();
        tabHeight = layoutMode == VartaLayoutMode.NARROW ? 18 : 0;

        VartaRect content = getContentBounds();
        contentY = content.y();
        contentBottom = content.bottom();

        VartaRect issueList = getIssueListBounds();
        listX = issueList.x();
        listWidth = issueList.width();
        listTop = issueList.y();
        listBottom = issueList.bottom();

        VartaRect actionPanel = getActionPanelBounds();
        actionX = actionPanel.x();
        actionWidth = actionPanel.width();
        actionTop = actionPanel.y();
        actionBottom = actionPanel.bottom();

        actionStatusHeight = shouldShowStatusBlock() ? getActionStatusHeight() : 0;
        actionStatusX = actionX + actionPadding();
        actionStatusY = actionTop + 20;
        actionStatusWidth = Math.max(1, actionWidth - actionPadding() * 2);
        actionStatusBlockHeight = Math.max(0, actionStatusHeight - 6);
        actionButtonTop = actionTop + 26 + actionStatusHeight;
        actionButtonBottom = Math.max(actionButtonTop + 1, actionBottom);

        if (layoutMode == VartaLayoutMode.NARROW) {
            tabY = headerBottom + Math.max(4, edgePadding / 2);
            tabWidth = Math.max(60, Math.min(90, (contentWidth - 6) / 2));
            issuesTabX = contentX;
            actionsTabX = contentX + tabWidth + 6;
            actionButtonTop = actionTop;
            actionButtonBottom = Math.max(actionButtonTop + 1, actionBottom);
        }
    }

    private VartaLayoutMode chooseLayoutMode(int availableWidth) {
        int normalGap = VartaUiLayout.GAP_LARGE;
        int compactGap = VartaUiLayout.GAP_SMALL;
        boolean normalFits = availableWidth >= MIN_ISSUE_LIST_WIDTH + normalGap + NORMAL_ACTION_PANEL_MIN_WIDTH;
        boolean compactFits = availableWidth >= MIN_ISSUE_LIST_WIDTH + compactGap + MIN_ACTION_PANEL_WIDTH;
        if (width >= 900 && normalFits) {
            return VartaLayoutMode.NORMAL;
        }
        if (width >= 650 && compactFits) {
            return VartaLayoutMode.COMPACT;
        }
        return VartaLayoutMode.NARROW;
    }

    private int getPanelGap(int screenWidth) {
        if (layoutMode == VartaLayoutMode.NARROW) {
            return 0;
        }
        if (layoutMode == VartaLayoutMode.NORMAL && screenWidth >= 900) {
            return VartaUiLayout.GAP_LARGE;
        }
        if (layoutMode == VartaLayoutMode.COMPACT && screenWidth >= 640) {
            return VartaUiLayout.GAP_MEDIUM;
        }
        return VartaUiLayout.GAP_SMALL;
    }

    private int getRightPanelWidth(int screenWidth) {
        int availableWidth = Math.max(1, screenWidth - edgePadding * 2);
        if (layoutMode == VartaLayoutMode.NARROW) {
            return availableWidth;
        }

        int minWidth = layoutMode == VartaLayoutMode.NORMAL ? NORMAL_ACTION_PANEL_MIN_WIDTH : MIN_ACTION_PANEL_WIDTH;
        int maxWidth = layoutMode == VartaLayoutMode.NORMAL ? RIGHT_PANEL_MAX_WIDTH : 240;
        float ratio = layoutMode == VartaLayoutMode.NORMAL ? 0.26F : 0.32F;
        int desired = (int) (availableWidth * ratio);
        int panelWidth = VartaUiLayout.clamp(desired, minWidth, Math.min(maxWidth, availableWidth));
        int maxPanelWidth = Math.max(minWidth, availableWidth - getPanelGap(screenWidth) - MIN_ISSUE_LIST_WIDTH);
        return Math.min(panelWidth, maxPanelWidth);
    }

    private int getHeaderHeight() {
        int base = layoutMode == VartaLayoutMode.NORMAL ? 58 : 50;
        if (countersFitBesideTitle()) {
            return base;
        }
        int counterLineHeight = layoutMode == VartaLayoutMode.NARROW ? 14 : 18;
        return Math.max(base, 8 + 42 + getCounterRowCount(contentWidth, true) * counterLineHeight + 4);
    }

    private VartaRect getContentBounds() {
        int y = headerBottom + edgePadding;
        if (layoutMode == VartaLayoutMode.NARROW) {
            y += tabHeight + Math.max(4, edgePadding / 2);
        }
        int minimumHeight = shouldShowStatusBlock()
                ? 26 + getActionStatusHeight() + buttonHeight() + 4
                : 70;
        int bottom = Math.max(y + minimumHeight, height - edgePadding);
        return new VartaRect(contentX, y, contentWidth, bottom - y);
    }

    private VartaRect getIssueListBounds() {
        if (layoutMode == VartaLayoutMode.NARROW) {
            return new VartaRect(contentX, contentY, Math.max(1, contentWidth), Math.max(1, contentBottom - contentY));
        }
        int gap = getPanelGap(width);
        int rightPanelWidth = getRightPanelWidth(width);
        int leftWidth = Math.max(MIN_ISSUE_LIST_WIDTH, contentWidth - rightPanelWidth - gap);
        return new VartaRect(contentX, contentY, leftWidth, Math.max(1, contentBottom - contentY));
    }

    private VartaRect getActionPanelBounds() {
        if (layoutMode == VartaLayoutMode.NARROW) {
            return new VartaRect(contentX, contentY, Math.max(1, contentWidth), Math.max(1, contentBottom - contentY));
        }
        int gap = getPanelGap(width);
        int rightPanelWidth = getRightPanelWidth(width);
        int leftWidth = Math.max(MIN_ISSUE_LIST_WIDTH, contentWidth - rightPanelWidth - gap);
        int x = Math.min(width - edgePadding - rightPanelWidth, contentX + leftWidth + gap);
        return new VartaRect(Math.max(contentX, x), contentY, rightPanelWidth, Math.max(1, contentBottom - contentY));
    }

    private boolean countersFitBesideTitle() {
        if (layoutMode == VartaLayoutMode.NARROW) {
            return false;
        }
        int minCounterX = contentX + Math.min(260, Math.max(150, contentWidth / 2));
        int available = contentX + contentWidth - minCounterX;
        return contentWidth >= 560 && totalCounterWidth(layoutMode != VartaLayoutMode.NORMAL) <= available;
    }

    private int getCounterRowCount(int availableWidth, boolean compact) {
        int rows = 1;
        int x = 0;
        for (Counter counter : counters()) {
            int pillWidth = counterPillWidth(counter.severity(), counter.count(), compact);
            if (x > 0 && x + 4 + pillWidth > availableWidth) {
                rows++;
                x = 0;
            }
            x += (x == 0 ? 0 : 4) + pillWidth;
        }
        return rows;
    }

    private int totalCounterWidth(boolean compact) {
        int total = 0;
        for (Counter counter : counters()) {
            total += counterPillWidth(counter.severity(), counter.count(), compact);
        }
        return total + 12;
    }

    private int getActionStatusHeight() {
        return height < 260 ? 40 : 46;
    }

    private int actionPadding() {
        if (layoutMode == VartaLayoutMode.NARROW || actionWidth < 190) {
            return 6;
        }
        return actionWidth < 230 ? 8 : 12;
    }

    private int buttonHeight() {
        return Math.max(20, height < 260 ? 20 : 22);
    }

    private int buttonGap() {
        return height < 320 ? 5 : 8;
    }

    private void addActionButtons(List<ActionSpec> actions) {
        if (layoutMode == VartaLayoutMode.NARROW) {
            addNarrowActionButtons(actions);
            return;
        }

        int buttonHeight = buttonHeight();
        int gap = buttonGap();
        int buttonWidth = Math.max(1, actionWidth - actionPadding() * 2);
        int x = actionX + actionPadding();
        int viewportHeight = Math.max(1, actionButtonBottom - actionButtonTop);
        int topActionCount = 0;
        int bottomActionCount = 0;

        for (ActionSpec action : actions) {
            if (action.kind() == ActionKind.SETTINGS || action.kind() == ActionKind.CONTINUE) {
                bottomActionCount++;
            } else {
                topActionCount++;
            }
        }

        int topHeight = topActionCount == 0 ? 0 : topActionCount * buttonHeight + (topActionCount - 1) * gap;
        int bottomHeight = bottomActionCount == 0 ? 0 : bottomActionCount * buttonHeight + (bottomActionCount - 1) * gap;
        boolean pinBottom = topHeight + bottomHeight + Math.max(10, gap * 2) <= viewportHeight;
        int allHeight = actions.isEmpty() ? 0 : actions.size() * buttonHeight + (actions.size() - 1) * gap;

        actionButtonsScroll = !pinBottom && allHeight > viewportHeight;
        maxActionScroll = actionButtonsScroll ? Math.max(0, allHeight - viewportHeight) : 0;
        actionScroll = VartaUiLayout.clamp(actionScroll, 0, maxActionScroll);

        if (actionButtonsScroll) {
            int y = actionButtonTop - actionScroll;
            for (ActionSpec action : actions) {
                addActionButton(action, x, y, buttonWidth, buttonHeight);
                y += buttonHeight + gap;
            }
            return;
        }

        actionScroll = 0;
        int topY = actionButtonTop;
        for (ActionSpec action : actions) {
            if (action.kind() == ActionKind.SETTINGS || action.kind() == ActionKind.CONTINUE) {
                continue;
            }
            addActionButton(action, x, topY, buttonWidth, buttonHeight);
            topY += buttonHeight + gap;
        }

        if (pinBottom) {
            int bottomY = actionBottom - buttonHeight;
            ActionSpec settings = findAction(actions, ActionKind.SETTINGS);
            ActionSpec continueAction = findAction(actions, ActionKind.CONTINUE);
            if (settings != null) {
                addActionButton(settings, x, bottomY, buttonWidth, buttonHeight);
                bottomY -= buttonHeight + gap;
            }
            if (continueAction != null) {
                addActionButton(continueAction, x, bottomY, buttonWidth, buttonHeight);
            }
        } else {
            for (ActionSpec action : actions) {
                if (action.kind() == ActionKind.SETTINGS || action.kind() == ActionKind.CONTINUE) {
                    addActionButton(action, x, topY, buttonWidth, buttonHeight);
                    topY += buttonHeight + gap;
                }
            }
        }
    }

    private void addNarrowActionButtons(List<ActionSpec> actions) {
        int viewportHeight = Math.max(1, actionButtonBottom - actionButtonTop);
        int contentHeight = computeNarrowActionContentHeight(actions);
        maxActionScroll = Math.max(0, contentHeight - viewportHeight);
        actionButtonsScroll = maxActionScroll > 0;
        actionScroll = VartaUiLayout.clamp(actionScroll, 0, maxActionScroll);

        int padding = narrowActionPadding();
        int buttonWidth = narrowActionButtonWidth();
        boolean horizontal = useHorizontalNarrowActions(buttonWidth);
        int buttonX = horizontal
                ? actionX + actionWidth - padding - buttonWidth
                : actionX + (actionWidth - buttonWidth) / 2;
        int y = actionTop + 20 - actionScroll;

        if (horizontal) {
            y = addNarrowActionGroup(actions, ActionKind.COPY_REPORT, ActionKind.COPY_JSON, buttonX, y, buttonWidth);
            y += ACTION_GROUP_GAP;
            y = addNarrowActionGroup(actions, ActionKind.OPEN_DIR, ActionKind.OPEN_SUPPORT, buttonX, y, buttonWidth);
            y += ACTION_GROUP_GAP;
            addNarrowActionGroup(actions, ActionKind.CONTINUE, ActionKind.SETTINGS, buttonX, y, buttonWidth);
            return;
        }

        y += narrowStatusHeight(false) + ACTION_GROUP_GAP + 12;
        y = addNarrowActionGroup(actions, ActionKind.COPY_REPORT, ActionKind.COPY_JSON, buttonX, y, buttonWidth);
        y += ACTION_GROUP_GAP;
        y = addNarrowActionGroup(actions, ActionKind.OPEN_DIR, ActionKind.OPEN_SUPPORT, buttonX, y, buttonWidth);
        y += ACTION_GROUP_GAP;
        addNarrowActionGroup(actions, ActionKind.CONTINUE, ActionKind.SETTINGS, buttonX, y, buttonWidth);
    }

    private int addNarrowActionGroup(List<ActionSpec> actions, ActionKind first, ActionKind second, int x, int y, int width) {
        int buttonHeight = buttonHeight();
        int gap = buttonGap();
        ActionSpec firstAction = findAction(actions, first);
        ActionSpec secondAction = findAction(actions, second);
        if (firstAction != null || secondAction != null) {
            y += ACTION_SECTION_LABEL_HEIGHT;
        }
        if (firstAction != null) {
            addActionButton(firstAction, x, y, width, buttonHeight);
            y += buttonHeight + gap;
        }
        if (secondAction != null) {
            addActionButton(secondAction, x, y, width, buttonHeight);
            y += buttonHeight + gap;
        }
        return y;
    }

    private int computeNarrowActionContentHeight(List<ActionSpec> actions) {
        int buttonHeight = buttonHeight();
        int gap = buttonGap();
        int buttonWidth = narrowActionButtonWidth();
        boolean horizontal = useHorizontalNarrowActions(buttonWidth);
        int buttonCount = actions.size();
        int buttonColumnHeight = buttonCount == 0 ? 0 : buttonCount * buttonHeight + Math.max(0, buttonCount - 1) * gap + ACTION_GROUP_GAP * 2;
        buttonColumnHeight += ACTION_SECTION_LABEL_HEIGHT * 3;
        int contentHeight = 20 + buttonColumnHeight + narrowActionPadding();
        if (horizontal) {
            return Math.max(contentHeight, 20 + narrowStatusHeight(true) + narrowActionPadding());
        }
        return contentHeight + narrowStatusHeight(false) + ACTION_GROUP_GAP + 12;
    }

    private void addActionButton(ActionSpec spec, int x, int y, int width, int height) {
        VartaPackButton button = VartaPackButton.of(x, y, width, height, actionLabel(spec, width), spec.onPress(), spec.style());
        button.visible = y + height > actionButtonTop && y < actionButtonBottom;
        button.active = button.visible;
        this.addRenderableWidget(button);
    }

    private Component actionLabel(ActionSpec spec, int buttonWidth) {
        int textWidth = Math.max(8, buttonWidth - 10);
        List<String> candidates = actionLabelCandidates(spec);
        for (String candidate : candidates) {
            if (this.font.width(candidate) <= textWidth) {
                return candidate.equals(spec.label().getString()) ? spec.label() : VartaComponents.literal(candidate);
            }
        }
        String shortest = candidates.isEmpty() ? spec.label().getString() : candidates.get(candidates.size() - 1);
        return VartaComponents.literal(trim(shortest, textWidth));
    }

    private List<String> actionLabelCandidates(ActionSpec spec) {
        List<String> labels = new ArrayList<>();
        labels.add(spec.label().getString());
        switch (spec.kind()) {
            case COPY_REPORT -> {
                labels.add("Copy Report");
                labels.add("Report");
            }
            case COPY_JSON -> {
                labels.add("Copy JSON");
                labels.add("JSON");
            }
            case OPEN_DIR -> {
                labels.add("Game Dir");
                labels.add("Folder");
            }
            case OPEN_SUPPORT -> labels.add("Support");
            case CONTINUE -> labels.add("Continue");
            case SETTINGS -> labels.add("Settings");
        }
        return labels;
    }

    private int narrowActionPadding() {
        return actionWidth < 260 ? 6 : 10;
    }

    private int narrowActionButtonWidth() {
        int availableWidth = Math.max(1, actionWidth - narrowActionPadding() * 2);
        int desired = (int) (availableWidth * 0.34F);
        if (availableWidth < MIN_BUTTON_COLUMN_WIDTH) {
            return availableWidth;
        }
        return VartaUiLayout.clamp(desired, MIN_BUTTON_COLUMN_WIDTH, Math.min(ACTION_BUTTON_MAX_WIDTH, availableWidth));
    }

    private boolean useHorizontalNarrowActions(int buttonWidth) {
        int padding = narrowActionPadding();
        int availableWidth = Math.max(1, actionWidth - padding * 2);
        int statusWidth = availableWidth - buttonWidth - ACTION_COLUMN_GAP;
        return availableWidth >= MIN_STATUS_WIDTH + MIN_BUTTON_COLUMN_WIDTH + ACTION_COLUMN_GAP
                && statusWidth >= MIN_STATUS_WIDTH;
    }

    private int narrowStatusHeight(boolean horizontal) {
        return horizontal ? 64 : 58;
    }

    private static ActionSpec findAction(List<ActionSpec> actions, ActionKind kind) {
        for (ActionSpec action : actions) {
            if (action.kind() == kind) return action;
        }
        return null;
    }

    private int computeIssueContentHeight() {
        int contentHeight = 0;
        for (IssueViewModel.Row row : vm.rows()) {
            contentHeight += rowHeight(row) + cardGap();
        }
        return Math.max(0, contentHeight - cardGap());
    }

    @Override
    public void renderBackground(GuiGraphics g) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBase(g);
        renderHeader(g);
        if (layoutMode == VartaLayoutMode.NARROW) {
            renderNarrowTabs(g);
            if (narrowTab == NarrowTab.ISSUES) {
                renderIssueList(g);
            } else {
                renderNarrowActionPanel(g);
                renderActionWidgets(g, mouseX, mouseY, partialTick);
            }
            return;
        }

        renderActionPanel(g);
        renderIssueList(g);
        renderActionStatusText(g);
        renderActionWidgets(g, mouseX, mouseY, partialTick);
    }

    private void renderBase(GuiGraphics g) {
        g.fill(0, 0, width, height, 0xFF05070B);
        g.fill(0, 0, width, height, 0xFF0B1018);
        g.fill(0, 0, width, 1, 0xFF566477);
        g.fill(0, headerBottom, width, headerBottom + 1, 0xFF334050);
        if (layoutMode != VartaLayoutMode.NARROW) {
            g.fill(actionX - getPanelGap(width) / 2, listTop, actionX - getPanelGap(width) / 2 + 1, listBottom, 0xFF252D38);
        }
    }

    private void renderHeader(GuiGraphics g) {
        int titleY = headerTop;
        g.drawString(this.font, VartaComponents.translatable(CommonTexts.SCREEN_TITLE), contentX, titleY, VartaUiLayout.textColor(0xFFFFFF));

        PackStatus status = vm.packStatus();
        String statusText = "Status: " + status.displayName().toUpperCase();
        int statusColor = colorForStatus(status);
        g.drawString(this.font, trim(statusText, headerTextWidth()), contentX, titleY + 12, VartaUiLayout.textColor(statusColor));

        String pack = vm.packName().isBlank() ? "" : vm.packName() + " (v" + vm.profileVersion() + ")";
        if (!pack.isEmpty()) {
            g.drawString(this.font, trim(pack, headerTextWidth()), contentX, titleY + 25, VartaUiLayout.textColor(0xF0F4FA));
        }

        boolean compactCounters = layoutMode != VartaLayoutMode.NORMAL;
        if (countersFitBesideTitle()) {
            renderCounterPillsRightAligned(g, contentX + contentWidth, headerTop, compactCounters);
        } else {
            renderCounterPillsWrapped(g, contentX, headerTop + 42, contentX + contentWidth, true);
        }
    }

    private int headerTextWidth() {
        if (!countersFitBesideTitle()) {
            return Math.max(40, contentWidth);
        }
        int counterLeft = contentX + contentWidth - totalCounterWidth(false);
        return Math.max(40, counterLeft - contentX - 12);
    }

    private void renderCounterPillsRightAligned(GuiGraphics g, int right, int y, boolean compact) {
        int x = right;
        List<Counter> counters = counters();
        for (int i = counters.size() - 1; i >= 0; i--) {
            Counter counter = counters.get(i);
            x = drawSummaryPill(g, x, y, counter.severity(), counter.count(), compact);
        }
    }

    private void renderCounterPillsWrapped(GuiGraphics g, int x, int y, int right, boolean compact) {
        int lineX = x;
        int lineY = y;
        int lineHeight = layoutMode == VartaLayoutMode.NARROW ? 14 : 18;
        for (Counter counter : counters()) {
            int width = counterPillWidth(counter.severity(), counter.count(), compact);
            if (lineX > x && lineX + width > right) {
                lineX = x;
                lineY += lineHeight;
            }
            drawSummaryPillAt(g, lineX, lineY, width, counter.severity(), counter.count(), compact);
            lineX += width + 4;
        }
    }

    private void renderNarrowTabs(GuiGraphics g) {
        drawTab(g, issuesTabX, tabY, tabWidth, "Issues", narrowTab == NarrowTab.ISSUES);
        drawTab(g, actionsTabX, tabY, tabWidth, "Actions", narrowTab == NarrowTab.ACTIONS);
        int dividerY = tabY + tabHeight + Math.max(3, edgePadding / 3);
        g.fill(0, dividerY, width, dividerY + 1, 0xFF334050);
        g.fill(contentX, contentY - 1, contentX + contentWidth, contentY, 0xFF151D29);
    }

    private void drawTab(GuiGraphics g, int x, int y, int tabWidth, String label, boolean active) {
        int fill = active ? 0xFF263243 : 0xFF171E2A;
        int border = active ? 0xFF7E93AD : 0xFF3A4351;
        g.fill(x, y, x + tabWidth, y + tabHeight, fill);
        g.fill(x, y, x + tabWidth, y + 1, border);
        g.drawCenteredString(this.font, trim(label, tabWidth - 8), x + tabWidth / 2, y + 5,
                VartaUiLayout.textColor(active ? 0xFFFFFF : 0xB8C2D0));
    }

    private int drawSummaryPill(GuiGraphics g, int right, int y, Severity severity, int count, boolean compact) {
        int pillWidth = counterPillWidth(severity, count, compact);
        int x = right - pillWidth;
        drawSummaryPillAt(g, x, y, pillWidth, severity, count, compact);
        return x - 4;
    }

    private void drawSummaryPillAt(GuiGraphics g, int x, int y, int pillWidth, Severity severity, int count, boolean compact) {
        String text = counterText(severity, count, compact);
        int color = colorFor(severity);
        g.fill(x, y, x + pillWidth, y + 14, 0xFF1B222D);
        g.fill(x, y, x + 2, y + 14, 0xFF000000 | color);
        g.drawString(this.font, text, x + 6, y + 3, VartaUiLayout.textColor(0xFFFFFF));
    }

    private int counterPillWidth(Severity severity, int count, boolean compact) {
        return this.font.width(counterText(severity, count, compact)) + 12;
    }

    private String counterText(Severity severity, int count, boolean compact) {
        return compact ? severity.name().charAt(0) + " " + count : severity.name() + " " + count;
    }

    private List<Counter> counters() {
        List<Counter> counters = new ArrayList<>();
        counters.add(new Counter(Severity.INFO, count(Severity.INFO)));
        counters.add(new Counter(Severity.WARNING, count(Severity.WARNING)));
        counters.add(new Counter(Severity.ERROR, count(Severity.ERROR)));
        counters.add(new Counter(Severity.CRITICAL, count(Severity.CRITICAL)));
        return counters;
    }

    private int count(Severity severity) {
        int count = 0;
        for (IssueViewModel.Row row : vm.rows()) {
            if (row.severity() == severity) count++;
        }
        return count;
    }

    private void renderActionPanel(GuiGraphics g) {
        g.drawString(this.font, "Actions", actionX + actionPadding(), actionTop + 4, VartaUiLayout.textColor(0xB8C2D0));
        if (actionStatusHeight > 0) {
            renderStatusBlock(g);
        }

        if (maxActionScroll > 0 && actionButtonBottom > actionButtonTop) {
            int viewportHeight = actionButtonBottom - actionButtonTop;
            int barHeight = Math.max(10, viewportHeight * viewportHeight / (maxActionScroll + viewportHeight));
            int barY = actionButtonTop + (int) ((float) actionScroll / maxActionScroll * (viewportHeight - barHeight));
            int x = actionX + actionWidth - 3;
            g.fill(x, actionButtonTop, x + 2, actionButtonBottom, 0xFF1B222D);
            g.fill(x, barY, x + 2, barY + barHeight, 0xFF7E93AD);
        }
    }

    private void renderNarrowActionPanel(GuiGraphics g) {
        VartaScissor.enable(actionX, actionTop, actionX + actionWidth, actionBottom);

        int padding = narrowActionPadding();
        int buttonWidth = narrowActionButtonWidth();
        boolean horizontal = useHorizontalNarrowActions(buttonWidth);
        int buttonX = horizontal
                ? actionX + actionWidth - padding - buttonWidth
                : actionX + (actionWidth - buttonWidth) / 2;
        int y = actionTop + 4 - actionScroll;

        g.drawString(this.font, "Actions", actionX + padding, y, VartaUiLayout.textColor(0xB8C2D0));
        if (horizontal) {
            int statusX = actionX + padding;
            int statusY = actionTop + 20 - actionScroll;
            int statusWidth = Math.max(1, buttonX - statusX - ACTION_COLUMN_GAP);
            renderNarrowStatusBlock(g, statusX, statusY, statusWidth, narrowStatusHeight(true));
            renderNarrowActionGroupLabels(g, buttonX, statusY, buttonWidth);
        } else {
            int statusWidth = Math.min(actionWidth - padding * 2, ACTION_BUTTON_MAX_WIDTH + 80);
            int statusX = actionX + (actionWidth - statusWidth) / 2;
            int statusY = actionTop + 20 - actionScroll;
            renderNarrowStatusBlock(g, statusX, statusY, statusWidth, narrowStatusHeight(false));
            renderNarrowActionGroupLabels(g, buttonX, statusY + narrowStatusHeight(false) + ACTION_GROUP_GAP + 12, buttonWidth);
        }

        VartaScissor.disable();

        if (maxActionScroll > 0 && actionBottom > actionTop) {
            int viewportHeight = actionBottom - actionTop;
            int barHeight = Math.max(10, viewportHeight * viewportHeight / (maxActionScroll + viewportHeight));
            int barY = actionTop + (int) ((float) actionScroll / maxActionScroll * (viewportHeight - barHeight));
            int x = actionX + actionWidth - 3;
            g.fill(x, actionTop, x + 2, actionBottom, 0xFF1B222D);
            g.fill(x, barY, x + 2, barY + barHeight, 0xFF7E93AD);
        }
    }

    private void renderNarrowActionGroupLabels(GuiGraphics g, int x, int y, int width) {
        y = renderNarrowActionGroupLabel(g, "Reports", x, y, width, hasAction(ActionKind.COPY_REPORT) || hasAction(ActionKind.COPY_JSON));
        y += ACTION_GROUP_GAP;
        y = renderNarrowActionGroupLabel(g, "Files", x, y, width, hasAction(ActionKind.OPEN_DIR) || hasAction(ActionKind.OPEN_SUPPORT));
        y += ACTION_GROUP_GAP;
        renderNarrowActionGroupLabel(g, "Navigation", x, y, width, hasAction(ActionKind.CONTINUE) || hasAction(ActionKind.SETTINGS));
    }

    private int renderNarrowActionGroupLabel(GuiGraphics g, String label, int x, int y, int width, boolean present) {
        if (!present) {
            return y;
        }
        g.drawString(this.font, trim(label, width), x, y, VartaUiLayout.textColor(0x8FA1B8));
        int count = label.equals("Reports") ? actionCount(ActionKind.COPY_REPORT, ActionKind.COPY_JSON)
                : label.equals("Files") ? actionCount(ActionKind.OPEN_DIR, ActionKind.OPEN_SUPPORT)
                : actionCount(ActionKind.CONTINUE, ActionKind.SETTINGS);
        return y + ACTION_SECTION_LABEL_HEIGHT + count * (buttonHeight() + buttonGap());
    }

    private boolean hasAction(ActionKind kind) {
        if (kind == ActionKind.OPEN_DIR) {
            return VartaPack.platform() != null;
        }
        if (kind == ActionKind.OPEN_SUPPORT) {
            return UrlUtil.isSafeWebUrl(vm.supportUrl());
        }
        if (kind == ActionKind.CONTINUE) {
            return !VartaPack.shouldBlockContinue();
        }
        return true;
    }

    private int actionCount(ActionKind first, ActionKind second) {
        int count = 0;
        if (hasAction(first)) {
            count++;
        }
        if (hasAction(second)) {
            count++;
        }
        return count;
    }

    private void renderNarrowStatusBlock(GuiGraphics g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, 0xFF171E2A);
        g.fill(x, y, x + 3, y + height, 0xFF000000 | colorForStatus(vm.packStatus()));
        g.fill(x, y, x + width, y + 1, 0xFF4A596B);

        int textX = x + 10;
        int textWidth = Math.max(12, width - 18);
        String title = "Profile " + vm.packStatus().displayName().toUpperCase();
        g.drawString(this.font, trim(title, textWidth), textX, y + 6,
                VartaUiLayout.textColor(colorForStatus(vm.packStatus())));

        List<String> statusLines = switch (vm.packStatus()) {
            case CLEAN -> List.of("No blocking issues detected.");
            case MODIFIED -> List.of("The instance differs from the expected profile.");
            case UNSUPPORTED -> List.of("Support may be limited for this setup.");
            case BROKEN -> List.of("ERROR/CRITICAL issues need attention.", "Fix the listed issues or copy a support report.");
        };
        int lineY = y + 19;
        int remainingLines = height <= 58 ? 2 : 3;
        for (String line : statusLines) {
            if (remainingLines <= 0) {
                break;
            }
            List<String> wrappedLines = wrap(line, textWidth, remainingLines);
            for (String wrapped : wrappedLines) {
                g.drawString(this.font, wrapped, textX, lineY, VartaUiLayout.textColor(0xFFD9E2EC));
                lineY += 10;
                remainingLines--;
            }
        }
    }

    private void renderStatusBlock(GuiGraphics g) {
        int x = actionStatusX;
        int y = actionStatusY;
        int width = actionStatusWidth;
        int height = actionStatusBlockHeight;

        g.fill(x, y, x + width, y + height, 0xFF171E2A);
        g.fill(x, y, x + 3, y + height, 0xFFFF5555);
        g.fill(x, y, x + width, y + 1, 0xFF4A596B);
    }

    private void renderActionStatusText(GuiGraphics g) {
        if (actionStatusHeight <= 0 || actionStatusBlockHeight <= 0) {
            return;
        }

        int x = actionStatusX;
        int y = actionStatusY;
        int statusWidth = Math.max(20, actionStatusWidth);
        int statusHeight = actionStatusBlockHeight;
        int textWidth = Math.max(12, statusWidth - 18);

        String title = statusHeight < 38 && vm.packStatus() == PackStatus.BROKEN
            ? "BROKEN - fix errors"
            : vm.packStatus() == PackStatus.BROKEN ? "Profile BROKEN" : "Fix required";
        g.drawString(this.font, trim(title, textWidth), x + 10, y + 6, VartaUiLayout.textColor(0xFFFF7777));

        if (statusHeight >= 38) {
            String line = VartaPack.shouldBlockContinue()
                    ? "Fix ERROR/CRITICAL issues before continuing."
                    : "ERROR/CRITICAL issues need attention.";
            int lineY = y + 19;
            for (String wrapped : wrap(line, textWidth, 2)) {
                g.drawString(this.font, wrapped, x + 10, lineY, VartaUiLayout.textColor(0xFFD9E2EC));
                lineY += 10;
            }
        }
    }

    private void renderActionWidgets(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        VartaScissor.enable(actionX, actionButtonTop, actionX + actionWidth, actionButtonBottom);
        super.render(g, mouseX, mouseY, partialTick);
        VartaScissor.disable();
    }

    private boolean shouldShowStatusBlock() {
        return vm.packStatus() == PackStatus.BROKEN;
    }

    private void renderIssueList(GuiGraphics g) {
        List<IssueViewModel.Row> rows = vm.rows();
        if (rows.isEmpty()) {
            VartaScissor.enable(listX - 2, listTop, listX + listWidth + 2, listBottom);
            g.drawCenteredString(this.font,
                    VartaComponents.translatable(CommonTexts.NO_VISIBLE_ISSUES),
                    listX + listWidth / 2, listTop + 18, VartaUiLayout.textColor(0xFFFFFF));
            VartaScissor.disable();
            return;
        }

        VartaScissor.enable(listX - 2, listTop, listX + listWidth + 2, listBottom);

        int y = listTop - issueScroll;
        for (IssueViewModel.Row row : rows) {
            int rowHeight = rowHeight(row);
            if (y + rowHeight >= listTop && y <= listBottom) {
                renderIssueCard(g, row, y, rowHeight);
            }
            y += rowHeight + cardGap();
        }

        VartaScissor.disable();

        if (maxIssueScroll > 0 && listBottom > listTop) {
            int viewportHeight = listBottom - listTop;
            int barHeight = Math.max(10, viewportHeight * viewportHeight / (maxIssueScroll + viewportHeight));
            int barY = listTop + (int) ((float) issueScroll / maxIssueScroll * (viewportHeight - barHeight));
            int x = listX + listWidth - 3;
            g.fill(x, listTop, x + 2, listBottom, 0xFF1B222D);
            g.fill(x, barY, x + 2, barY + barHeight, 0xFF7E93AD);
        }
    }

    private void renderIssueCard(GuiGraphics g, IssueViewModel.Row row, int y, int rowHeight) {
        int color = colorFor(row.severity());
        int padding = cardPadding();
        int innerX = listX + padding;
        int cardRight = listX + issueCardWidth();
        int innerRight = cardRight - padding;
        int textWidth = Math.max(40, innerRight - innerX);
        boolean stackedHeader = shouldStackCardHeader();

        g.fill(listX, y, cardRight, y + rowHeight, 0xFF101722);
        g.fill(listX, y, cardRight, y + 1, 0xFF4A596B);
        g.fill(listX, y, listX + 3, y + rowHeight, 0xFF000000 | color);

        int lineY = y + padding;
        String severity = row.severity().name();
        int badgeWidth = Math.min(this.font.width(severity) + 12, textWidth);
        g.fill(innerX, lineY, innerX + badgeWidth, lineY + 14, 0xFF202633);
        g.fill(innerX, lineY + 13, innerX + badgeWidth, lineY + 14, 0xFF000000 | color);
        g.drawString(this.font, trim(severity, Math.max(10, badgeWidth - 12)), innerX + 6, lineY + 3,
                VartaUiLayout.textColor(color));

        if (stackedHeader) {
            lineY += 18;
            for (String line : wrap(row.title(), textWidth, maxTitleLines())) {
                g.drawString(this.font, line, innerX, lineY, VartaUiLayout.textColor(0xFFFFFF));
                lineY += 10;
            }
        } else {
            int titleX = innerX + badgeWidth + 8;
            int titleWidth = Math.max(40, innerRight - titleX);
            for (String line : wrap(row.title(), titleWidth, maxTitleLines())) {
                g.drawString(this.font, line, titleX, lineY + 3, VartaUiLayout.textColor(0xFFFFFF));
                lineY += 10;
            }
            lineY = Math.max(lineY, y + padding + 18);
        }

        lineY += 4;
        lineY = drawWrapped(g, row.message(), innerX, lineY, textWidth, maxMessageLines(), VartaUiLayout.textColor(0xEEF3FA));

        if (maxTechnicalLines() > 0 && row.technicalDetails() != null && !row.technicalDetails().isBlank()) {
            lineY += 2;
            lineY = drawWrapped(g, row.technicalDetails(), innerX, lineY, textWidth, maxTechnicalLines(), VartaUiLayout.textColor(0xB8C2D0));
        }

        if (row.fix() != null && !row.fix().isBlank()) {
            lineY += 2;
            String prefix = "-> Fix: ";
            g.drawString(this.font, prefix, innerX, lineY, VartaUiLayout.textColor(0x88DDFF));
            int prefixWidth = this.font.width(prefix);
            List<String> lines = wrap(row.fix(), Math.max(40, textWidth - prefixWidth), maxFixLines());
            for (int i = 0; i < lines.size(); i++) {
                int x = i == 0 ? innerX + prefixWidth : innerX;
                g.drawString(this.font, lines.get(i), x, lineY, VartaUiLayout.textColor(0x88DDFF));
                lineY += 10;
            }
        }
    }

    private int drawWrapped(GuiGraphics g, String text, int x, int y, int wrapWidth, int maxLines, int color) {
        for (String line : wrap(text, wrapWidth, maxLines)) {
            g.drawString(this.font, line, x, y, VartaUiLayout.textColor(color));
            y += 10;
        }
        return y;
    }

    private int rowHeight(IssueViewModel.Row row) {
        int padding = cardPadding();
        int textWidth = Math.max(40, issueCardWidth() - padding * 2);
        int badgeWidth = this.font.width(row.severity().name()) + 12;
        boolean stackedHeader = shouldStackCardHeader();
        int titleWidth = stackedHeader ? textWidth : Math.max(40, textWidth - badgeWidth - 8);
        int titleLines = Math.max(1, wrap(row.title(), titleWidth, maxTitleLines()).size());

        int rowHeight = padding;
        rowHeight += stackedHeader ? 18 + titleLines * 10 : Math.max(18, titleLines * 10);
        rowHeight += 4 + wrap(row.message(), textWidth, maxMessageLines()).size() * 10;

        if (maxTechnicalLines() > 0 && row.technicalDetails() != null && !row.technicalDetails().isBlank()) {
            rowHeight += 2 + wrap(row.technicalDetails(), textWidth, maxTechnicalLines()).size() * 10;
        }
        if (row.fix() != null && !row.fix().isBlank()) {
            int prefixWidth = this.font == null ? FIX_PREFIX_WIDTH_FALLBACK : this.font.width("-> Fix: ");
            rowHeight += 2 + wrap(row.fix(), Math.max(40, textWidth - prefixWidth), maxFixLines()).size() * 10;
        }

        return Math.max(height < 300 ? 50 : 58, rowHeight + padding);
    }

    private int cardPadding() {
        return layoutMode == VartaLayoutMode.NORMAL && listWidth >= 440 ? 12 : 8;
    }

    private int cardGap() {
        return layoutMode == VartaLayoutMode.NORMAL && height >= 320 ? 8 : 6;
    }

    private int issueCardWidth() {
        return Math.max(1, listWidth - VartaUiLayout.SCROLLBAR_GUTTER);
    }

    private boolean shouldStackCardHeader() {
        return layoutMode != VartaLayoutMode.NORMAL || listWidth < 440;
    }

    private int maxTitleLines() {
        return layoutMode == VartaLayoutMode.NORMAL ? 2 : 1;
    }

    private int maxMessageLines() {
        return layoutMode == VartaLayoutMode.NORMAL ? 3 : 2;
    }

    private int maxTechnicalLines() {
        return layoutMode == VartaLayoutMode.NORMAL ? 2 : layoutMode == VartaLayoutMode.COMPACT ? 1 : 0;
    }

    private int maxFixLines() {
        return layoutMode == VartaLayoutMode.NORMAL ? 2 : 1;
    }

    private String trim(String text, int pxWidth) {
        return VartaTextWrapHelper.trim(this.font, text, pxWidth);
    }

    private List<String> wrap(String text, int pxWidth, int maxLines) {
        return VartaTextWrapHelper.wrap(this.font, text, pxWidth, maxLines);
    }

    private static int colorFor(Severity severity) {
        return switch (severity) {
            case CRITICAL -> 0xFFFF5555;
            case ERROR -> 0xFFFFAA00;
            case WARNING -> 0xFFFFFF55;
            case INFO -> 0xFFA0A0A0;
        };
    }

    private static int colorForStatus(PackStatus status) {
        return switch (status) {
            case CLEAN -> 0xFF55FF55;
            case MODIFIED -> 0xFFFFFF55;
            case UNSUPPORTED -> 0xFFFFAA00;
            case BROKEN -> 0xFFFF5555;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (layoutMode == VartaLayoutMode.NARROW && mouseY >= tabY && mouseY <= tabY + tabHeight) {
            if (mouseX >= issuesTabX && mouseX <= issuesTabX + tabWidth) {
                narrowTab = NarrowTab.ISSUES;
                rebuildActionWidgets();
                return true;
            }
            if (mouseX >= actionsTabX && mouseX <= actionsTabX + tabWidth) {
                narrowTab = NarrowTab.ACTIONS;
                rebuildActionWidgets();
                return true;
            }
        }
        if (isActionPaneVisible() && isInsideActionButtons(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isActionPaneVisible() && isInsideActionButtons(mouseX, mouseY)) {
            return super.mouseReleased(mouseX, mouseY, button);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        if (isIssuePaneVisible() && isInsideList(mouseX, mouseY)) {
            issueScroll = VartaUiLayout.clamp(issueScroll - (int) (deltaY * 18), 0, maxIssueScroll);
            return true;
        }
        if (isActionPaneVisible() && isInsideActionPanel(mouseX, mouseY) && maxActionScroll > 0) {
            actionScroll = VartaUiLayout.clamp(actionScroll - (int) (deltaY * 18), 0, maxActionScroll);
            rebuildActionWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaY);
    }

    private boolean isIssuePaneVisible() {
        return layoutMode != VartaLayoutMode.NARROW || narrowTab == NarrowTab.ISSUES;
    }

    private boolean isActionPaneVisible() {
        return layoutMode != VartaLayoutMode.NARROW || narrowTab == NarrowTab.ACTIONS;
    }

    private boolean isInsideList(double mouseX, double mouseY) {
        return mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listTop && mouseY <= listBottom;
    }

    private boolean isInsideActionPanel(double mouseX, double mouseY) {
        return mouseX >= actionX && mouseX <= actionX + actionWidth && mouseY >= actionTop && mouseY <= actionBottom;
    }

    private boolean isInsideActionButtons(double mouseX, double mouseY) {
        return mouseX >= actionX && mouseX <= actionX + actionWidth && mouseY >= actionButtonTop && mouseY <= actionButtonBottom;
    }

    private void rebuildActionWidgets() {
        clearWidgets();
        init();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !VartaPack.shouldBlockContinue();
    }

    @Override
    public void onClose() {
        if (VartaPack.shouldBlockContinue()) {
            VartaPackToast.show(
                    Minecraft.getInstance(),
                    VartaComponents.translatable(CommonTexts.TOAST_TITLE),
                    VartaComponents.translatable(CommonTexts.CONTINUE_BLOCKED),
                    Severity.ERROR);
            return;
        }
        VartaPack.markScreenShown();
        Minecraft.getInstance().setScreen(parent);
    }

    private record Counter(Severity severity, int count) {}

    private record ActionSpec(ActionKind kind, Component label, VartaPackButton.OnPress onPress, VartaPackButton.Style style) {}
}
