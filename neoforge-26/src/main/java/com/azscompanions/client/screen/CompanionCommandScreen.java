package com.azscompanions.client.screen;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.network.packet.CompanionCommandPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Easy movement commands: Follow, Stay, Wander (server-authoritative via packet).
 */
public final class CompanionCommandScreen extends Screen {
    private static final int PANEL_BG = 0xC0101010;
    private static final int PANEL_EDGE = 0xFF8B8B8B;

    private final CompanionEntity companion;
    private final Screen parent;
    private int panelX;
    private int panelY;
    private final int panelW = 220;
    private final int panelH = 168;

    public CompanionCommandScreen(CompanionEntity companion, Screen parent) {
        super(Component.translatable("screen.azscompanions.command"));
        this.companion = companion;
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        int bx = panelX + 30;
        int by = panelY + 40;
        addRenderableWidget(modeButton("screen.azscompanions.command.follow", "FOLLOW", bx, by));
        addRenderableWidget(modeButton("screen.azscompanions.command.stay", "STAY", bx, by + 28));
        addRenderableWidget(modeButton("screen.azscompanions.command.wander", "WANDER", bx, by + 56));
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> {
            if (minecraft != null) {
                minecraft.gui.setScreen(parent);
            }
        }).bounds(bx, by + 92, 160, 20).build());
    }

    private Button modeButton(String langKey, String command, int x, int y) {
        return Button.builder(Component.translatable(langKey), b -> {
            ClientPacketDistributor.sendToServer(new CompanionCommandPacket(companion.getId(), command));
            if (minecraft != null) {
                minecraft.gui.setScreen(null);
            }
        }).bounds(x, y, 160, 22).build();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, PANEL_EDGE);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG);
        graphics.text(font, title, panelX + panelW / 2, panelY + 12, 0xFFFFFF);
        CompanionMode mode = companion.getMode();
        graphics.text(font, Component.translatable("screen.azscompanions.command.current", mode.getSerializedName()),
                panelX + panelW / 2, panelY + 24, 0xA0A0A0);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
