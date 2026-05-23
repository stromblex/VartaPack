package com.stromblex.vartapack.client;

import com.stromblex.vartapack.VartaPack;
import com.stromblex.vartapack.api.ClipboardService;
import com.stromblex.vartapack.check.Severity;
import com.stromblex.vartapack.report.SupportReport;
import com.stromblex.vartapack.ui.CommonTexts;
import com.stromblex.vartapack.ui.IssueViewModel;
import com.stromblex.vartapack.util.UrlUtil;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.net.URI;
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

    public VartaPackIssuesScreen(Screen parent, IssueViewModel vm, ClipboardService clipboard) {
        super(Component.translatable(CommonTexts.SCREEN_TITLE));
        this.parent = parent;
        this.vm = vm;
        this.clipboard = clipboard;
    }

    @Override
    protected void init() {
        super.init();
        int btnW = 140;
        int gap = 4;
        int btnY = this.height - 28;
        int totalW = btnW * 4 + gap * 3;
        int startX = (this.width - totalW) / 2;

        this.addRenderableWidget(Button.builder(
                Component.translatable(CommonTexts.BTN_COPY_REPORT),
                b -> {
                    SupportReport report = vm.buildReport();
                    clipboard.copy(report.markdown());
                    VartaPackToast.show(
                            Minecraft.getInstance(),
                            Component.translatable(CommonTexts.TOAST_TITLE),
                            Component.translatable(CommonTexts.REPORT_COPIED),
                            Severity.INFO);
                }
        ).bounds(startX, btnY, btnW, 20).build());

        boolean hasUrl = UrlUtil.isSafeWebUrl(vm.supportUrl());
        this.addRenderableWidget(Button.builder(
                Component.translatable(CommonTexts.BTN_OPEN_SUPPORT),
                b -> {
                    if (hasUrl) Util.getPlatform().openUri(URI.create(vm.supportUrl()));
                }
        ).bounds(startX + btnW + gap, btnY, btnW, 20).build()).active = hasUrl;

        this.addRenderableWidget(Button.builder(
                Component.translatable(CommonTexts.BTN_OPEN_GAME_DIR),
                b -> {
                    if (VartaPack.platform() != null) {
                        Util.getPlatform().openFile(VartaPack.platform().getGameDirectory().toFile());
                    }
                }
        ).bounds(startX + (btnW + gap) * 2, btnY, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable(CommonTexts.BTN_CONTINUE),
                b -> onClose()
        ).bounds(startX + (btnW + gap) * 3, btnY, btnW, 20).build());

        maxScroll = Math.max(0, computeContentHeight() - (this.height - 90));
    }

    private int computeContentHeight() {
        int h = 0;
        for (IssueViewModel.Row row : vm.rows()) {
            h += 12; // title line
            h += 11; // message line
            if (row.technicalDetails() != null && !row.technicalDetails().isBlank()) {
                h += 11; // details line
            }
            h += 6; // gap between issues
        }
        return h;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        g.drawCenteredString(this.font,
                Component.translatable(CommonTexts.SCREEN_TITLE), this.width / 2, 8, 0xFFFFFF);
        g.drawCenteredString(this.font,
                Component.translatable(CommonTexts.SCREEN_SUBTITLE), this.width / 2, 20, 0xA0A0A0);

        String pack = vm.packName().isBlank() ? "" :
                vm.packName() + " (v" + vm.profileVersion() + ")";
        if (!pack.isEmpty()) {
            g.drawCenteredString(this.font, Component.literal(pack), this.width / 2, 32, 0xC0C0C0);
        }

        int listTop = 46;
        int listBottom = this.height - 50;

        g.enableScissor(0, listTop, this.width, listBottom);

        int y = listTop - scroll;
        List<IssueViewModel.Row> rows = vm.rows();
        for (IssueViewModel.Row row : rows) {
            int color = colorFor(row.severity());
            String prefix = "[" + row.severity().name() + "] ";
            g.drawString(this.font, prefix + row.title(), 12, y, color, false);
            y += 12;
            g.drawString(this.font, trim(row.message(), this.width - 24), 20, y, 0xD0D0D0, false);
            y += 11;
            if (row.technicalDetails() != null && !row.technicalDetails().isBlank()) {
                g.drawString(this.font, trim(row.technicalDetails(), this.width - 32), 24, y, 0x808080, false);
                y += 11;
            }
            y += 6;
        }

        g.disableScissor();

        if (maxScroll > 0) {
            int barHeight = Math.max(10, (listBottom - listTop) * (listBottom - listTop) / (maxScroll + listBottom - listTop));
            int barY = listTop + (int) ((float) scroll / maxScroll * (listBottom - listTop - barHeight));
            g.fill(this.width - 4, barY, this.width - 1, barY + barHeight, 0x60FFFFFF);
        }

        int infoY = this.height - 42;
        if (vm.packPingInstalled()) {
            g.drawCenteredString(this.font,
                    Component.translatable(CommonTexts.PACKPING_DETECTED),
                    this.width / 2, infoY, 0x80FF80);
        } else if (vm.packPingRecommended()) {
            g.drawCenteredString(this.font,
                    Component.translatable(CommonTexts.PACKPING_RECOMMENDED),
                    this.width / 2, infoY, 0xA0A0A0);
        }
    }

    private String trim(String s, int pxWidth) {
        if (s == null) return "";
        if (this.font.width(s) <= pxWidth) return s;
        return this.font.plainSubstrByWidth(s, pxWidth - 12) + "...";
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
    public void onClose() {
        VartaPack.markScreenShown();
        Minecraft.getInstance().setScreen(parent);
    }
}
