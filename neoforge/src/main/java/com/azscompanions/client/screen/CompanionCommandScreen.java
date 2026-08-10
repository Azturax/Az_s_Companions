package com.azscompanions.client.screen;

import com.azscompanions.client.GuiScrollbarState;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.network.packet.CompanionCommandPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Scrollable companion commands: Follow/Stay/Wander plus Gather / Deposit actions.
 * Scrollbar thumb is click-draggable.
 */
public final class CompanionCommandScreen extends Screen {
    private static final int PANEL_BG = 0xC0101010;
    private static final int PANEL_EDGE = 0xFF8B8B8B;
    private static final int SCROLLBAR_W = 6;
    private static final int BTN_H = 22;
    private static final int BTN_GAP = 4;

    private final CompanionEntity companion;
    private final Screen parent;
    private int panelX;
    private int panelY;
    private final int panelW = 240;
    private final int panelH = 220;
    private int viewTop;
    private int viewBottom;
    private final GuiScrollbarState scrollbar = new GuiScrollbarState();
    private final List<ScrollBtn> scrollBtns = new ArrayList<>();

    public CompanionCommandScreen(CompanionEntity companion, Screen parent) {
        super(Component.translatable("screen.azscompanions.command"));
        this.companion = companion;
        this.parent = parent;
    }

    @Override
    protected void init() {
        scrollBtns.clear();
        clearWidgets();
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        viewTop = panelY + 36;
        viewBottom = panelY + panelH - 32;
        int bx = panelX + 16;
        int bw = panelW - 32 - SCROLLBAR_W - 6;
        int y = 0;

        addScrollBtn(bx, bw, y, "screen.azscompanions.command.follow", () -> send("FOLLOW", true));
        y += BTN_H + BTN_GAP;
        addScrollBtn(bx, bw, y, "screen.azscompanions.command.stay", () -> send("STAY", true));
        y += BTN_H + BTN_GAP;
        addScrollBtn(bx, bw, y, "screen.azscompanions.command.wander", () -> send("WANDER", true));
        y += BTN_H + BTN_GAP + 6;
        addScrollBtn(bx, bw, y, "screen.azscompanions.command.gather", () -> {
            if (minecraft != null) {
                minecraft.setScreen(new CompanionGatherScreen(companion, this));
            }
        });
        y += BTN_H + BTN_GAP;
        addScrollBtn(bx, bw, y, "screen.azscompanions.command.gather_status", () -> send("GATHER_STATUS", false));
        y += BTN_H + BTN_GAP;
        addScrollBtn(bx, bw, y, "screen.azscompanions.command.gather_cancel", () -> send("GATHER_CANCEL", true));
        y += BTN_H + BTN_GAP + 6;
        addScrollBtn(bx, bw, y, "screen.azscompanions.command.deposit", () -> send("DEPOSIT_SELECT", true));
        y += BTN_H + BTN_GAP;
        addScrollBtn(bx, bw, y, "screen.azscompanions.command.deposit_done", () -> send("DEPOSIT_DONE", true));
        y += BTN_H + BTN_GAP;
        addScrollBtn(bx, bw, y, "screen.azscompanions.command.deposit_clear", () -> send("DEPOSIT_CLEAR", true));
        y += BTN_H + BTN_GAP;

        int contentH = y;
        int viewH = Math.max(1, viewBottom - viewTop);
        scrollbar.layout(panelX + panelW - 12, viewTop, viewBottom, SCROLLBAR_W,
                Math.max(0, contentH - viewH));
        applyScroll();

        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> {
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        }).bounds(bx, panelY + panelH - 26, bw, 20).build());
    }

    private void addScrollBtn(int x, int w, int contentY, String langKey, Runnable action) {
        Button btn = Button.builder(Component.translatable(langKey), b -> action.run())
                .bounds(x, 0, w, BTN_H).build();
        scrollBtns.add(new ScrollBtn(btn, contentY));
        addRenderableWidget(btn);
    }

    private void send(String command, boolean close) {
        PacketDistributor.sendToServer(new CompanionCommandPacket(companion.getId(), command));
        if (close && minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    private void applyScroll() {
        int scroll = scrollbar.scroll();
        for (ScrollBtn entry : scrollBtns) {
            int screenY = viewTop + entry.contentY - scroll;
            entry.button.setY(screenY);
            boolean visible = screenY + BTN_H > viewTop && screenY < viewBottom;
            entry.button.visible = visible;
            entry.button.active = visible;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollbar.maxScroll() > 0
                && mouseX >= panelX && mouseX <= panelX + panelW
                && mouseY >= viewTop && mouseY <= viewBottom) {
            scrollbar.scrollBy(-(int) (scrollY * 12));
            applyScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scrollbar.mouseClicked(mouseX, mouseY, button)) {
            applyScroll();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbar.mouseDragged(mouseY)) {
            applyScroll();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scrollbar.mouseReleased(button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, PANEL_EDGE);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG);
        graphics.drawCenteredString(font, title, panelX + panelW / 2, panelY + 10, 0xFFFFFF);
        CompanionMode mode = companion.getMode();
        graphics.drawCenteredString(font,
                Component.translatable("screen.azscompanions.command.current", mode.getSerializedName()),
                panelX + panelW / 2, panelY + 22, 0xA0A0A0);

        for (ScrollBtn entry : scrollBtns) {
            entry.button.visible = false;
        }
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.enableScissor(panelX + 8, viewTop, panelX + panelW - 14, viewBottom);
        for (ScrollBtn entry : scrollBtns) {
            int screenY = viewTop + entry.contentY - scrollbar.scroll();
            boolean visible = screenY + BTN_H > viewTop && screenY < viewBottom;
            entry.button.visible = visible;
            entry.button.active = visible;
            if (visible) {
                entry.button.render(graphics, mouseX, mouseY, partialTick);
            }
        }
        graphics.disableScissor();
        drawScrollbar(graphics);
    }

    private void drawScrollbar(GuiGraphics graphics) {
        if (scrollbar.maxScroll() <= 0) {
            return;
        }
        int trackX = panelX + panelW - 12;
        int trackH = scrollbar.trackH();
        graphics.fill(trackX, viewTop, trackX + SCROLLBAR_W, viewTop + trackH, 0x66000000);
        int thumbY = scrollbar.thumbY();
        int thumbH = scrollbar.thumbH();
        int color = scrollbar.isDragging() ? 0xFFFFFFFF : 0xFFC0C0C0;
        graphics.fill(trackX, thumbY, trackX + SCROLLBAR_W, thumbY + thumbH, color);
        graphics.fill(trackX, thumbY, trackX + SCROLLBAR_W, thumbY + 1, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record ScrollBtn(Button button, int contentY) {
    }
}
