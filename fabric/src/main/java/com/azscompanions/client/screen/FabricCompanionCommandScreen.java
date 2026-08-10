package com.azscompanions.client.screen;

import com.azscompanions.client.GuiScrollbarState;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricCompanionMode;
import com.azscompanions.network.FabricNetworkingClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Scrollable Fabric command menu with Gather/Deposit actions and draggable scrollbar. */
public final class FabricCompanionCommandScreen extends Screen {
    private static final int PANEL_BG = 0xC0101010;
    private static final int PANEL_EDGE = 0xFF8B8B8B;
    private static final int SCROLLBAR_W = 6;
    private static final int BTN_H = 22;
    private static final int BTN_GAP = 4;

    private final FabricCompanionEntity companion;
    private final Screen parent;
    private int panelX;
    private int panelY;
    private final int panelW = 240;
    private final int panelH = 220;
    private int viewTop;
    private int viewBottom;
    private final GuiScrollbarState scrollbar = new GuiScrollbarState();
    private final List<ScrollBtn> scrollBtns = new ArrayList<>();

    public FabricCompanionCommandScreen(FabricCompanionEntity companion, Screen parent) {
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
        addScrollBtn(bx, bw, y, "screen.azscompanions.command.deposit", () -> send("DEPOSIT_SELECT", true));
        y += BTN_H + BTN_GAP;
        addScrollBtn(bx, bw, y, "screen.azscompanions.command.deposit_done", () -> send("DEPOSIT_DONE", true));
        y += BTN_H + BTN_GAP;
        addScrollBtn(bx, bw, y, "screen.azscompanions.command.deposit_clear", () -> send("DEPOSIT_CLEAR", true));
        y += BTN_H + BTN_GAP;
        // Gather collect_material is NeoForge-first; Fabric shows status note via deposit for now.
        addScrollBtn(bx, bw, y, "screen.azscompanions.command.gather", () -> {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal(
                        "Gather goals: use NeoForge /az gather, or CCI on NeoForge. Deposit select works here."), false);
            }
        });
        y += BTN_H + BTN_GAP;

        scrollbar.layout(panelX + panelW - 12, viewTop, viewBottom, SCROLLBAR_W,
                Math.max(0, y - Math.max(1, viewBottom - viewTop)));
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
        FabricNetworkingClient.sendMenuAction(companion.getId(), command);
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
        FabricCompanionMode mode = companion.getMode();
        graphics.drawCenteredString(font,
                Component.translatable("screen.azscompanions.command.current", mode.name().toLowerCase()),
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
        if (scrollbar.maxScroll() > 0) {
            int trackX = panelX + panelW - 12;
            graphics.fill(trackX, viewTop, trackX + SCROLLBAR_W, viewTop + scrollbar.trackH(), 0x66000000);
            int color = scrollbar.isDragging() ? 0xFFFFFFFF : 0xFFC0C0C0;
            graphics.fill(trackX, scrollbar.thumbY(), trackX + SCROLLBAR_W, scrollbar.thumbY() + scrollbar.thumbH(), color);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record ScrollBtn(Button button, int contentY) {
    }
}
