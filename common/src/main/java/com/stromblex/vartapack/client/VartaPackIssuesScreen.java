package com.stromblex.vartapack.client;

import com.stromblex.vartapack.VartaPack;
import com.stromblex.vartapack.api.ClipboardService;
import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.ui.CommonTexts;
import com.stromblex.vartapack.ui.IssueViewModel;
import com.stromblex.vartapack.validation.PackStatus;
import com.stromblex.vartapack.util.UrlUtil;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Vanilla-styled, loader-agnostic VartaPack issues screen.
 * Lives in common source dir; both Fabric and NeoForge compile it via {@code srcDir}.
 */
public final class VartaPackIssuesScreen extends FixedScaleScreen {

    private enum ActionKind { COPY_REPORT, COPY_JSON, OPEN_DIR, OPEN_SUPPORT, CONTINUE, SETTINGS }

    private final Screen parent;
    private final IssueViewModel vm;
    private final ClipboardService clipboard;

    private int scroll;
    private int maxScroll;
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
    private int actionStatusHeight;
    private int actionStatusX;
    private int actionStatusY;
    private int actionStatusWidth;
    private int actionStatusBlockHeight;

    public VartaPackIssuesScreen(Screen parent, IssueViewModel vm, ClipboardService clipboard) {
        super(Component.translatable(CommonTexts.SCREEN_TITLE));
        this.parent = parent;
        this.vm = vm;
        this.clipboard = clipboard;
    }

    @Override
    protected void initFixed() {
        List<ActionSpec> actions = buildActions();
        layoutMetrics();
        addActionButtons(actions);

        int viewportHeight = Math.max(1, listBottom - listTop);
        maxScroll = Math.max(0, computeContentHeight() - viewportHeight);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
    }

    private List<ActionSpec> buildActions() {
        List<ActionSpec> actions = new ArrayList<>();

        actions.add(new ActionSpec(ActionKind.COPY_REPORT,
                Component.translatable(CommonTexts.BTN_COPY_REPORT),
                b -> {
                    clipboard.copy(vm.buildMarkdownReport());
                    showCopiedToast();
                },
                VartaPackButton.Style.PRIMARY));

        actions.add(new ActionSpec(ActionKind.COPY_JSON,
                Component.translatable(CommonTexts.BTN_COPY_JSON),
                b -> {
                    clipboard.copy(vm.buildJsonReport());
                    showCopiedToast();
                },
                VartaPackButton.Style.SECONDARY));

        if (VartaPack.platform() != null) {
            actions.add(new ActionSpec(ActionKind.OPEN_DIR,
                    Component.translatable(CommonTexts.BTN_OPEN_GAME_DIR),
                    b -> Util.getPlatform().openFile(VartaPack.platform().getGameDirectory().toFile()),
                    VartaPackButton.Style.SECONDARY));
        }

        if (UrlUtil.isSafeWebUrl(vm.supportUrl())) {
            actions.add(new ActionSpec(ActionKind.OPEN_SUPPORT,
                    Component.translatable(CommonTexts.BTN_OPEN_SUPPORT),
                    b -> Util.getPlatform().openUri(URI.create(vm.supportUrl())),
                    VartaPackButton.Style.SUBTLE));
        }

        if (!VartaPack.shouldBlockContinue()) {
            actions.add(new ActionSpec(ActionKind.CONTINUE,
                    Component.translatable(CommonTexts.BTN_CONTINUE),
                    b -> onClose(),
                    VartaPackButton.Style.SUBTLE));
        }

        actions.add(new ActionSpec(ActionKind.SETTINGS,
                Component.translatable(CommonTexts.BTN_SETTINGS),
                b -> Minecraft.getInstance().setScreen(new VartaPackConfigScreen(this, clipboard)),
                VartaPackButton.Style.SUBTLE));

        return actions;
    }

    private void showCopiedToast() {
        VartaPackToast.show(
                Minecraft.getInstance(),
                Component.translatable(CommonTexts.TOAST_TITLE),
                Component.translatable(CommonTexts.REPORT_COPIED),
                Severity.INFO);
    }

    private void layoutMetrics() {
        int width = uiWidth();
        int height = uiHeight();
        int margin = Math.max(10, Math.min(24, width / 80));
        contentWidth = Math.max(120, width - margin * 2);
        contentX = (width - contentWidth) / 2;
        contentY = margin;
        contentBottom = Math.max(contentY + 80, height - margin);

        int headerHeight = 68;
        headerTop = contentY + 8;
        headerBottom = contentY + headerHeight;
        actionStatusHeight = shouldShowStatusBlock() ? 46 : 0;
        layoutWide();
    }

    private void layoutWide() {
        int gap = 16;
        actionWidth = Math.min(320, Math.max(220, contentWidth / 4));
        actionX = contentX + contentWidth - actionWidth;
        listX = contentX;
        listWidth = Math.max(120, contentWidth - actionWidth - gap);
        listTop = headerBottom + 12;
        listBottom = contentBottom - 12;
        actionTop = listTop;
        actionBottom = listBottom;
        actionStatusX = actionX + 12;
        actionStatusY = actionTop + 20;
        actionStatusWidth = Math.max(1, actionWidth - 24);
        actionStatusBlockHeight = Math.max(0, actionStatusHeight - 6);
    }

    private void addActionButtons(List<ActionSpec> actions) {
        addWideActionButtons(actions);
    }

    private void addWideActionButtons(List<ActionSpec> actions) {
        int buttonHeight = buttonHeight();
        int buttonWidth = Math.max(80, actionWidth - 24);
        int x = actionX + 12;
        int topY = actionTop + 26 + actionStatusHeight;
        int bottomY = actionBottom - buttonHeight;

        ActionSpec settings = findAction(actions, ActionKind.SETTINGS);
        ActionSpec continueAction = findAction(actions, ActionKind.CONTINUE);
        if (settings != null) {
            addButton(settings, x, bottomY, buttonWidth, buttonHeight);
            bottomY -= buttonHeight + 8;
        }
        if (continueAction != null) {
            addButton(continueAction, x, bottomY, buttonWidth, buttonHeight);
            bottomY -= buttonHeight + 8;
        }

        int y = topY;
        for (ActionSpec action : actions) {
            if (action.kind() == ActionKind.SETTINGS || action.kind() == ActionKind.CONTINUE) continue;
            if (y + buttonHeight > bottomY + buttonHeight) break;
            addButton(action, x, y, buttonWidth, buttonHeight);
            y += buttonHeight + 8;
        }
    }

    private void addButton(ActionSpec spec, int x, int y, int width, int height) {
        this.addRenderableWidget(VartaPackButton.of(x, y, width, height, spec.label(), spec.onPress(), spec.style()));
    }

    private static ActionSpec findAction(List<ActionSpec> actions, ActionKind kind) {
        for (ActionSpec action : actions) {
            if (action.kind() == kind) return action;
        }
        return null;
    }

    private int buttonHeight() {
        return 22;
    }

    private int computeContentHeight() {
        int height = 0;
        for (IssueViewModel.Row row : vm.rows()) {
            height += rowHeight(row) + cardGap();
        }
        return Math.max(0, height - cardGap());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected void renderFixed(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBase(g);
        renderHeader(g);
        renderActionPanel(g);
        renderIssueList(g);
        renderActionStatusText(g);
        renderFixedWidgets(g, mouseX, mouseY, partialTick);
    }

    private void renderBase(GuiGraphics g) {
        g.fill(0, 0, uiWidth(), uiHeight(), 0xFF05070B);
        g.fill(contentX - 6, contentY, contentX + contentWidth + 6, contentBottom, 0xFF0B1018);
        g.fill(contentX - 6, contentY, contentX + contentWidth + 6, contentY + 1, 0xFF566477);
        g.fill(contentX - 6, headerBottom, contentX + contentWidth + 6, headerBottom + 1, 0xFF334050);
        g.fill(actionX - 8, listTop, actionX - 7, listBottom, 0xFF252D38);
    }

    private void renderHeader(GuiGraphics g) {
        int titleY = headerTop;
        g.drawString(this.font, Component.translatable(CommonTexts.SCREEN_TITLE), contentX, titleY, 0xFFFFFF, true);

        PackStatus status = vm.packStatus();
        String statusText = "Status: " + status.displayName().toUpperCase();
        int statusColor = colorForStatus(status);
        g.drawString(this.font, trim(statusText, contentWidth), contentX, titleY + 12, statusColor, true);

        String pack = vm.packName().isBlank() ? "" : vm.packName() + " (v" + vm.profileVersion() + ")";
        if (!pack.isEmpty()) {
            g.drawString(this.font, trim(pack, listWidth), contentX, titleY + 25, 0xF0F4FA, true);
        }

        renderCounterPills(g, actionX - 16, headerTop, contentX + 140, false);
    }

    private void renderCounterPills(GuiGraphics g, int right, int y, int minX, boolean compact) {
        int x = right;
        x = drawSummaryPill(g, x, y, Severity.CRITICAL, count(Severity.CRITICAL), minX, compact);
        x = drawSummaryPill(g, x, y, Severity.ERROR, count(Severity.ERROR), minX, compact);
        x = drawSummaryPill(g, x, y, Severity.WARNING, count(Severity.WARNING), minX, compact);
        drawSummaryPill(g, x, y, Severity.INFO, count(Severity.INFO), minX, compact);
    }

    private int drawSummaryPill(GuiGraphics g, int right, int y, Severity severity, int count, int minX, boolean compact) {
        String text = compact ? severity.name().charAt(0) + " " + count : severity.name() + " " + count;
        int width = this.font.width(text) + 12;
        int x = right - width;
        if (x < minX) return right;
        int color = colorFor(severity);
        g.fill(x, y, right, y + 14, 0xFF1B222D);
        g.fill(x, y, x + 2, y + 14, 0xFF000000 | color);
        g.drawString(this.font, text, x + 6, y + 3, 0xFFFFFF, true);
        return x - 4;
    }

    private int count(Severity severity) {
        int count = 0;
        for (IssueViewModel.Row row : vm.rows()) {
            if (row.severity() == severity) count++;
        }
        return count;
    }

    private void renderActionPanel(GuiGraphics g) {
        g.drawString(this.font, "Actions", actionX + 12, actionTop + 4, 0xB8C2D0, true);
        if (actionStatusHeight > 0) {
            renderStatusBlock(g);
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
        int width = Math.max(20, actionStatusWidth);
        int height = actionStatusBlockHeight;
        int textWidth = Math.max(12, width - 18);

        String title = height < 38 && vm.packStatus() == PackStatus.BROKEN
            ? "BROKEN - fix errors"
            : vm.packStatus() == PackStatus.BROKEN ? "Profile BROKEN" : "Fix required";
        g.drawString(this.font, trim(title, textWidth), x + 10, y + 6, 0xFFFF7777, true);

        if (height >= 38) {
            String line = VartaPack.shouldBlockContinue()
                    ? "Fix ERROR/CRITICAL issues before continuing."
                    : "ERROR/CRITICAL issues need attention.";
            int lineY = y + 19;
            for (String wrapped : wrap(line, textWidth, 2)) {
                g.drawString(this.font, wrapped, x + 10, lineY, 0xFFD9E2EC, true);
                lineY += 10;
            }
        }
    }

    private boolean shouldShowStatusBlock() {
        return vm.packStatus() == PackStatus.BROKEN;
    }

    private void renderIssueList(GuiGraphics g) {
        List<IssueViewModel.Row> rows = vm.rows();
        if (rows.isEmpty()) {
            enableFixedScissor(g, listX - 2, listTop, listX + listWidth + 2, listBottom);
            g.drawCenteredString(this.font,
                    Component.translatable(CommonTexts.NO_VISIBLE_ISSUES),
                    listX + listWidth / 2, listTop + 18, 0xFFFFFF);
            disableFixedScissor(g);
            return;
        }

        enableFixedScissor(g, listX - 2, listTop, listX + listWidth + 2, listBottom);

        int y = listTop - scroll;
        for (IssueViewModel.Row row : rows) {
            int height = rowHeight(row);
            if (y + height >= listTop && y <= listBottom) {
                renderIssueCard(g, row, y, height);
            }
            y += height + cardGap();
        }

        disableFixedScissor(g);

        if (maxScroll > 0 && listBottom > listTop) {
            int viewportHeight = listBottom - listTop;
            int barHeight = Math.max(10, viewportHeight * viewportHeight / (maxScroll + viewportHeight));
            int barY = listTop + (int) ((float) scroll / maxScroll * (viewportHeight - barHeight));
            int x = Math.min(listX + listWidth + 5, contentX + contentWidth - 2);
            g.fill(x, listTop, x + 2, listBottom, 0xFF1B222D);
            g.fill(x, barY, x + 2, barY + barHeight, 0xFF7E93AD);
        }
    }

    private void renderIssueCard(GuiGraphics g, IssueViewModel.Row row, int y, int height) {
        int color = colorFor(row.severity());
        int padding = cardPadding();
        int innerX = listX + padding;
        int innerRight = listX + listWidth - padding;
        int textWidth = Math.max(40, innerRight - innerX);
        boolean stackedHeader = shouldStackCardHeader();

        g.fill(listX, y, listX + listWidth, y + height, 0xFF101722);
        g.fill(listX, y, listX + listWidth, y + 1, 0xFF4A596B);
        g.fill(listX, y, listX + 3, y + height, 0xFF000000 | color);

        int lineY = y + padding;
        String severity = row.severity().name();
        int badgeWidth = this.font.width(severity) + 12;
        g.fill(innerX, lineY, innerX + badgeWidth, lineY + 14, 0xFF202633);
        g.fill(innerX, lineY + 13, innerX + badgeWidth, lineY + 14, 0xFF000000 | color);
        g.drawString(this.font, severity, innerX + 6, lineY + 3, color, true);

        if (stackedHeader) {
            lineY += 18;
            for (String line : wrap(row.title(), textWidth, 2)) {
                g.drawString(this.font, line, innerX, lineY, 0xFFFFFF, true);
                lineY += 10;
            }
        } else {
            int titleX = innerX + badgeWidth + 8;
            int titleWidth = Math.max(40, innerRight - titleX);
            for (String line : wrap(row.title(), titleWidth, 2)) {
                g.drawString(this.font, line, titleX, lineY + 3, 0xFFFFFF, true);
                lineY += 10;
            }
            lineY = Math.max(lineY, y + padding + 18);
        }

        lineY += 4;
        lineY = drawWrapped(g, row.message(), innerX, lineY, textWidth, 0xEEF3FA);

        if (row.technicalDetails() != null && !row.technicalDetails().isBlank()) {
            lineY += 2;
            lineY = drawWrapped(g, row.technicalDetails(), innerX, lineY, textWidth, 0xB8C2D0);
        }

        if (row.fix() != null && !row.fix().isBlank()) {
            lineY += 2;
            g.drawString(this.font, "-> Fix: ", innerX, lineY, 0x88DDFF, true);
            int prefixWidth = this.font.width("-> Fix: ");
            List<String> lines = wrap(row.fix(), Math.max(40, textWidth - prefixWidth), 0);
            for (int i = 0; i < lines.size(); i++) {
                int x = i == 0 ? innerX + prefixWidth : innerX;
                g.drawString(this.font, lines.get(i), x, lineY, 0x88DDFF, true);
                lineY += 10;
            }
        }
    }

    private int drawWrapped(GuiGraphics g, String text, int x, int y, int width, int color) {
        for (String line : wrap(text, width, 0)) {
            g.drawString(this.font, line, x, y, color, true);
            y += 10;
        }
        return y;
    }

    private int rowHeight(IssueViewModel.Row row) {
        int padding = cardPadding();
        int textWidth = Math.max(40, listWidth - padding * 2);
        int badgeWidth = this.font.width(row.severity().name()) + 12;
        boolean stackedHeader = shouldStackCardHeader();
        int titleWidth = stackedHeader ? textWidth : Math.max(40, textWidth - badgeWidth - 8);
        int titleLines = Math.max(1, wrap(row.title(), titleWidth, 2).size());

        int height = padding;
        height += stackedHeader ? 18 + titleLines * 10 : Math.max(18, titleLines * 10);
        height += 4 + wrap(row.message(), textWidth, 0).size() * 10;

        if (row.technicalDetails() != null && !row.technicalDetails().isBlank()) {
            height += 2 + wrap(row.technicalDetails(), textWidth, 0).size() * 10;
        }
        if (row.fix() != null && !row.fix().isBlank()) {
            height += 2 + wrap(row.fix(), textWidth, 0).size() * 10;
        }

        return Math.max(58, height + padding);
    }

    private int cardPadding() {
        return 12;
    }

    private int cardGap() {
        return 8;
    }

    private boolean shouldStackCardHeader() {
        return false;
    }

    private String trim(String text, int pxWidth) {
        if (text == null || pxWidth <= 0) return "";
        if (this.font.width(text) <= pxWidth) return text;
        return this.font.plainSubstrByWidth(text, Math.max(12, pxWidth - 12)) + "...";
    }

    private List<String> wrap(String text, int pxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank() || pxWidth <= 0) return lines;

        String remaining = text.trim();
        while (!remaining.isEmpty() && (maxLines <= 0 || lines.size() < maxLines)) {
            if (this.font.width(remaining) <= pxWidth) {
                lines.add(remaining);
                break;
            }

            String slice = this.font.plainSubstrByWidth(remaining, Math.max(8, pxWidth - 8));
            if (slice.isEmpty()) slice = remaining.substring(0, 1);

            int breakAt = slice.lastIndexOf(' ');
            if (breakAt > 8) slice = slice.substring(0, breakAt);

            remaining = remaining.substring(slice.length()).trim();
            boolean lastAllowedLine = maxLines > 0 && lines.size() == maxLines - 1;
            if (lastAllowedLine && !remaining.isEmpty()) {
                lines.add(trim(slice + "...", pxWidth));
                break;
            }
            lines.add(slice);
        }
        return lines;
    }

    private static int colorFor(Severity severity) {
        return switch (severity) {
            case CRITICAL -> 0xFF5555;
            case ERROR -> 0xFFAA00;
            case WARNING -> 0xFFFF55;
            case INFO -> 0xA0A0A0;
        };
    }

    private static int colorForStatus(PackStatus status) {
        return switch (status) {
            case CLEAN -> 0x55FF55;
            case MODIFIED -> 0xFFFF55;
            case UNSUPPORTED -> 0xFFAA00;
            case BROKEN -> 0xFF5555;
        };
    }

    @Override
    protected boolean mouseScrolledFixed(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (mouseY >= listTop && mouseY <= listBottom) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (deltaY * 18)));
            return true;
        }
        return super.mouseScrolledFixed(mouseX, mouseY, deltaX, deltaY);
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
                    Component.translatable(CommonTexts.TOAST_TITLE),
                    Component.translatable(CommonTexts.CONTINUE_BLOCKED),
                    Severity.ERROR);
            return;
        }
        VartaPack.markScreenShown();
        Minecraft.getInstance().setScreen(parent);
    }

    private record ActionSpec(ActionKind kind, Component label, VartaPackButton.OnPress onPress, VartaPackButton.Style style) {}
}
