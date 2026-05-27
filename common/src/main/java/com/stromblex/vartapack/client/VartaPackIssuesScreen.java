package com.stromblex.vartapack.client;

import com.stromblex.vartapack.VartaPack;
import com.stromblex.vartapack.api.ClipboardService;
import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.report.SupportReport;
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
public final class VartaPackIssuesScreen extends Screen {

    private final Screen parent;
    private final IssueViewModel vm;
    private final ClipboardService clipboard;
    private int scroll;
    private int maxScroll;
    private int contentX;
    private int contentWidth;
    private int listX;
    private int listWidth;
    private int actionX;
    private int actionWidth;
    private int actionButtonsY;
    private int listTop;
    private int listBottom;

    public VartaPackIssuesScreen(Screen parent, IssueViewModel vm, ClipboardService clipboard) {
        super(Component.translatable(CommonTexts.SCREEN_TITLE));
        this.parent = parent;
        this.vm = vm;
        this.clipboard = clipboard;
    }

    @Override
    protected void init() {
        super.init();
        layoutMetrics();

        List<FooterButton> buttons = new ArrayList<>();

        buttons.add(new FooterButton(
                Component.translatable(VartaPack.shouldBlockContinue()
                        ? CommonTexts.BTN_CONTINUE_BLOCKED
                        : CommonTexts.BTN_CONTINUE),
                b -> onClose(),
            !VartaPack.shouldBlockContinue(),
            VartaPack.shouldBlockContinue() ? VartaPackButton.Style.WARNING : VartaPackButton.Style.PRIMARY));

        buttons.add(new FooterButton(Component.translatable(CommonTexts.BTN_COPY_REPORT), b -> {
                clipboard.copy(vm.buildMarkdownReport());
                VartaPackToast.show(
                        Minecraft.getInstance(),
                        Component.translatable(CommonTexts.TOAST_TITLE),
                        Component.translatable(CommonTexts.REPORT_COPIED),
                        Severity.INFO);
            }, true, VartaPackButton.Style.PRIMARY));

            buttons.add(new FooterButton(Component.translatable(CommonTexts.BTN_COPY_JSON), b -> {
                clipboard.copy(vm.buildJsonReport());
                VartaPackToast.show(
                        Minecraft.getInstance(),
                        Component.translatable(CommonTexts.TOAST_TITLE),
                        Component.translatable(CommonTexts.REPORT_COPIED),
                        Severity.INFO);
            }, true, VartaPackButton.Style.SECONDARY));
        buttons.add(new FooterButton(Component.translatable(CommonTexts.BTN_OPEN_GAME_DIR), b -> {
            if (VartaPack.platform() != null) {
                Util.getPlatform().openFile(VartaPack.platform().getGameDirectory().toFile());
            }
        }, VartaPack.platform() != null, VartaPackButton.Style.SUBTLE));

        boolean hasUrl = UrlUtil.isSafeWebUrl(vm.supportUrl());
        buttons.add(new FooterButton(Component.translatable(CommonTexts.BTN_OPEN_SUPPORT),
                b -> {
                    if (hasUrl) Util.getPlatform().openUri(URI.create(vm.supportUrl()));
            }, hasUrl, VartaPackButton.Style.SUBTLE));

        buttons.add(new FooterButton(Component.translatable(CommonTexts.BTN_SETTINGS),
                b -> Minecraft.getInstance().setScreen(new VartaPackConfigScreen(this, clipboard)), true));

        addActionButtons(buttons);
        maxScroll = Math.max(0, computeContentHeight() - Math.max(1, listBottom - listTop));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
    }

    private void layoutMetrics() {
        contentWidth = Math.min(1040, Math.max(300, this.width - 36));
        contentX = (this.width - contentWidth) / 2;
        actionWidth = this.width >= 820 ? 208 : 0;
        listX = contentX;
        listWidth = actionWidth > 0 ? contentWidth - actionWidth - 16 : contentWidth;
        actionX = contentX + contentWidth - actionWidth;
        listTop = 86;
        listBottom = this.height - (actionWidth > 0 ? 28 : 0); // narrow listBottom set in addActionButtons
    }

    private void addActionButtons(List<FooterButton> buttons) {
        if (actionWidth > 0) {
            int x = actionX + 14;
            int y = listTop;
            actionButtonsY = y;
            int buttonWidth = actionWidth - 28;
            for (FooterButton spec : buttons) {
                VartaPackButton button = VartaPackButton.of(x, y, buttonWidth, 24, spec.label(), spec.onPress(), spec.style());
                button.active = spec.active();
                this.addRenderableWidget(button);
                y += 30;
            }
            return;
        }
        actionButtonsY = 0;

        int gap = 6;
        int buttonWidth = 132;
        int columns = Math.max(1, Math.min(2, (this.width - 24 + gap) / (buttonWidth + gap)));
        int rows = (int) Math.ceil(buttons.size() / (double) columns);
        int startY = this.height - 14 - rows * 28;
        listBottom = startY - 8; // keep list above the button block
        int totalWidth = columns * buttonWidth + (columns - 1) * gap;
        int startX = (this.width - totalWidth) / 2;
        boolean hasOrphan = columns > 1 && buttons.size() % columns == 1;
        for (int i = 0; i < buttons.size(); i++) {
            int row = i / columns;
            int col = i % columns;
            FooterButton spec = buttons.get(i);
            // Last button alone in its row — center it
            int bx = (hasOrphan && i == buttons.size() - 1)
                    ? (this.width - buttonWidth) / 2
                    : startX + col * (buttonWidth + gap);
            VartaPackButton button = VartaPackButton.of(
                    bx, startY + row * 28,
                    buttonWidth, 24, spec.label(), spec.onPress(), spec.style());
            button.active = spec.active();
            this.addRenderableWidget(button);
        }
    }

    private int computeContentHeight() {
        int h = 0;
        for (IssueViewModel.Row row : vm.rows()) {
            h += rowHeight(row) + 8;
        }
        return h;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBase(g);

        renderHeader(g);
        renderIssueList(g);
        renderActionPanel(g);
        renderFooterNote(g);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderBase(GuiGraphics g) {
        g.fill(0, 0, this.width, this.height, 0xFF05070B);
        g.fill(contentX - 10, 10, contentX + contentWidth + 10, this.height - 10, 0xFF0B1018);
        g.fill(contentX - 10, 10, contentX + contentWidth + 10, 11, 0xFF566477);
        g.fill(contentX - 10, 58, contentX + contentWidth + 10, 59, 0xFF334050);
        if (actionWidth > 0) {
            g.fill(actionX - 8, 72, actionX - 7, this.height - 26, 0xFF252D38);
        }
    }

    private void renderHeader(GuiGraphics g) {
        g.drawString(this.font, Component.translatable(CommonTexts.SCREEN_TITLE), contentX, 18, 0xFFFFFF, true);

        // Pack status badge
        PackStatus status = vm.packStatus();
        String statusText = "Status: " + status.displayName().toUpperCase();
        int statusColor = switch (status) {
            case CLEAN -> 0x55FF55;
            case MODIFIED -> 0xFFFF55;
            case UNSUPPORTED -> 0xFFAA00;
            case BROKEN -> 0xFF5555;
        };
        g.drawString(this.font, statusText, contentX, 31, statusColor, true);

        String pack = vm.packName().isBlank() ? "" :
                vm.packName() + " (v" + vm.profileVersion() + ")";
        if (!pack.isEmpty()) {
            g.drawString(this.font, trim(pack, listWidth), contentX, 44, 0xF0F4FA, true);
        }

        int x = actionWidth > 0 ? actionX - 16 : contentX + contentWidth;
        x = drawSummaryPill(g, x, Severity.CRITICAL, count(Severity.CRITICAL));
        x = drawSummaryPill(g, x, Severity.ERROR, count(Severity.ERROR));
        x = drawSummaryPill(g, x, Severity.WARNING, count(Severity.WARNING));
        drawSummaryPill(g, x, Severity.INFO, count(Severity.INFO));
    }

    private int drawSummaryPill(GuiGraphics g, int right, Severity severity, int count) {
        String text = severity.name() + " " + count;
        int w = this.font.width(text) + 12;
        int x = right - w;
        int color = colorFor(severity);
        g.fill(x, 20, right, 34, 0xFF1B222D);
        g.fill(x, 20, x + 2, 34, 0xFF000000 | color);
        g.drawString(this.font, text, x + 6, 23, 0xFFFFFF, true);
        return x - 4;
    }

    private int count(Severity severity) {
        int count = 0;
        for (var row : vm.rows()) {
            if (row.severity() == severity) count++;
        }
        return count;
    }

    private void renderIssueList(GuiGraphics g) {
        List<IssueViewModel.Row> rows = vm.rows();
        if (rows.isEmpty()) {
            int y = listTop + 28;
                g.drawCenteredString(this.font,
                    Component.translatable(CommonTexts.NO_VISIBLE_ISSUES),
                    listX + listWidth / 2, y, 0xFFFFFF);
            return;
        }

            g.enableScissor(listX - 2, listTop, listX + listWidth + 2, listBottom);

        int y = listTop - scroll;
        for (IssueViewModel.Row row : rows) {
            int height = rowHeight(row);
            renderIssueCard(g, row, y, height);
            y += height + 8;
        }

        g.disableScissor();

        if (maxScroll > 0) {
            int barHeight = Math.max(10, (listBottom - listTop) * (listBottom - listTop) / (maxScroll + listBottom - listTop));
            int barY = listTop + (int) ((float) scroll / maxScroll * (listBottom - listTop - barHeight));
            g.fill(listX + listWidth + 5, listTop, listX + listWidth + 7, listBottom, 0xFF1B222D);
            g.fill(listX + listWidth + 5, barY, listX + listWidth + 7, barY + barHeight, 0xFF7E93AD);
        }
    }

    private void renderIssueCard(GuiGraphics g, IssueViewModel.Row row, int y, int height) {
        int color = colorFor(row.severity());
        g.fill(listX, y, listX + listWidth, y + height, 0xFF101722);
        g.fill(listX, y, listX + listWidth, y + 1, 0xFF4A596B);
        g.fill(listX, y, listX + 3, y + height, 0xFF000000 | color);

        String severity = row.severity().name();
        int badgeWidth = this.font.width(severity) + 12;
        g.fill(listX + 12, y + 9, listX + 12 + badgeWidth, y + 23, 0xFF202633);
        g.fill(listX + 12, y + 22, listX + 12 + badgeWidth, y + 23, 0xFF000000 | color);
        g.drawString(this.font, severity, listX + 18, y + 12, color, true);

        int textX = listX + 24;
        int textWidth = listWidth - 36;
        g.drawString(this.font, trim(row.title(), textWidth - badgeWidth - 12),
                listX + 24 + badgeWidth + 8, y + 12, 0xFFFFFF, true);

        int lineY = y + 31;
        for (String line : wrap(row.message(), textWidth, 3)) {
            g.drawString(this.font, line, textX, lineY, 0xEEF3FA, true);
            lineY += 10;
        }
        if (row.technicalDetails() != null && !row.technicalDetails().isBlank()) {
            lineY += 2;
            for (String line : wrap(row.technicalDetails(), textWidth, 2)) {
                g.drawString(this.font, line, textX, lineY, 0xB8C2D0, true);
                lineY += 10;
            }
        }
        if (row.fix() != null && !row.fix().isBlank()) {
            lineY += 2;
            g.drawString(this.font, "\u2192 Fix: ", textX, lineY, 0x88DDFF, true);
            int fixOffset = this.font.width("\u2192 Fix: ");
            for (String line : wrap(row.fix(), textWidth - fixOffset, 2)) {
                g.drawString(this.font, line, textX + fixOffset, lineY, 0x88DDFF, true);
                lineY += 10;
                fixOffset = 0;
            }
        }
    }

    private void renderActionPanel(GuiGraphics g) {
    }

    private void renderFooterNote(GuiGraphics g) {
        if (VartaPack.shouldBlockContinue()) {
            // Wide layout: note below the action panel; narrow layout: note in the header gap above the list
            int infoY = actionWidth > 0 ? this.height - 22 : listTop - 12;
            g.drawCenteredString(this.font,
                    Component.translatable(CommonTexts.CONTINUE_BLOCKED),
                    this.width / 2, infoY, 0xFFDD88);
        }
    }

    private int rowHeight(IssueViewModel.Row row) {
        int textWidth = Math.max(80, listWidth - 36);
        int h = 43 + wrap(row.message(), textWidth, 3).size() * 10;
        if (row.technicalDetails() != null && !row.technicalDetails().isBlank()) {
            h += 2 + wrap(row.technicalDetails(), textWidth, 2).size() * 10;
        }
        if (row.fix() != null && !row.fix().isBlank()) {
            h += 2 + wrap(row.fix(), textWidth, 2).size() * 10;
        }
        return Math.max(56, h);
    }

    private String trim(String s, int pxWidth) {
        if (s == null) return "";
        if (this.font.width(s) <= pxWidth) return s;
        return this.font.plainSubstrByWidth(s, Math.max(12, pxWidth - 12)) + "...";
    }

    private List<String> wrap(String text, int pxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank() || maxLines <= 0) return lines;
        String remaining = text.trim();
        while (!remaining.isEmpty() && lines.size() < maxLines) {
            if (this.font.width(remaining) <= pxWidth) {
                lines.add(remaining);
                break;
            }
            String slice = this.font.plainSubstrByWidth(remaining, Math.max(24, pxWidth - 12));
            int breakAt = slice.lastIndexOf(' ');
            if (breakAt > 16) slice = slice.substring(0, breakAt);
            remaining = remaining.substring(slice.length()).trim();
            if (lines.size() == maxLines - 1 && !remaining.isEmpty()) {
                lines.add(slice + "...");
                break;
            }
            lines.add(slice);
        }
        return lines;
    }

    private static int colorFor(Severity s) {
        return switch (s) {
            case CRITICAL -> 0xFF5555;
            case ERROR -> 0xFFAA00;
            case WARNING -> 0xFFFF55;
            case INFO -> 0xA0A0A0;
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (deltaY * 16)));
        return true;
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

    private record FooterButton(Component label, VartaPackButton.OnPress onPress, boolean active, VartaPackButton.Style style) {
        FooterButton(Component label, VartaPackButton.OnPress onPress, boolean active) {
            this(label, onPress, active, VartaPackButton.Style.SECONDARY);
        }
    }
}
