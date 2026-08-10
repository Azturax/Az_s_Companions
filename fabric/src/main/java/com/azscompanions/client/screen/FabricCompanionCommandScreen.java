package com.azscompanions.client.screen;

import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricCompanionMode;
import com.azscompanions.network.FabricNetworkingClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Easy movement commands: Follow, Stay, Wander. */
public final class FabricCompanionCommandScreen extends Screen {
    private static final int PANEL_BG = 0xC0101010;
    private static final int PANEL_EDGE = 0xFF8B8B8B;

    private final FabricCompanionEntity companion;
    private final Screen parent;
    private int panelX;
    private int panelY;
    private final int panelW = 220;
    private final int panelH = 168;

    public FabricCompanionCommandScreen(FabricCompanionEntity companion, Screen parent) {
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
                minecraft.setScreen(parent);
            }
        }).bounds(bx, by + 92, 160, 20).build());
    }

    private Button modeButton(String langKey, String command, int x, int y) {
        return Button.builder(Component.translatable(langKey), b -> {
            FabricNetworkingClient.sendMenuAction(companion.getId(), command);
            if (minecraft != null) {
                minecraft.setScreen(null);
            }
        }).bounds(x, y, 160, 22).build();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, PANEL_EDGE);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG);
        graphics.drawCenteredString(font, title, panelX + panelW / 2, panelY + 12, 0xFFFFFF);
        FabricCompanionMode mode = companion.getMode();
        graphics.drawCenteredString(font,
                Component.translatable("screen.azscompanions.command.current", mode.name().toLowerCase()),
                panelX + panelW / 2, panelY + 24, 0xA0A0A0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
