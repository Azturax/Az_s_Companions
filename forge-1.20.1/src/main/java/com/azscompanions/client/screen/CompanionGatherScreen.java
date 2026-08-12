package com.azscompanions.client.screen;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.network.packet.CompanionGatherAssignPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;

/** Assign a material gather goal without typing {@code /az gather}. */
public final class CompanionGatherScreen extends Screen {
    private static final int PANEL_BG = 0xC0101010;
    private static final int PANEL_EDGE = 0xFF8B8B8B;

    private final CompanionEntity companion;
    private final Screen parent;
    private EditBox itemBox;
    private EditBox countBox;
    private int panelX;
    private int panelY;
    private final int panelW = 260;
    private final int panelH = 160;
    private String depositMode = "chest";

    public CompanionGatherScreen(CompanionEntity companion, Screen parent) {
        super(Component.translatable("screen.azscompanions.gather"));
        this.companion = companion;
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        int bx = panelX + 16;
        int bw = panelW - 32;

        itemBox = new EditBox(font, bx, panelY + 36, bw, 18,
                Component.translatable("screen.azscompanions.gather.item"));
        itemBox.setMaxLength(64);
        itemBox.setValue("minecraft:cobblestone");
        itemBox.setHint(Component.literal("minecraft:cobblestone"));
        addRenderableWidget(itemBox);

        countBox = new EditBox(font, bx, panelY + 70, 80, 18,
                Component.translatable("screen.azscompanions.gather.count"));
        countBox.setMaxLength(8);
        countBox.setValue("64");
        addRenderableWidget(countBox);

        addRenderableWidget(Button.builder(Component.translatable("screen.azscompanions.gather.deposit_chest"), b -> {
            depositMode = "chest";
        }).bounds(bx + 90, panelY + 68, 70, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.azscompanions.gather.deposit_look"), b -> {
            depositMode = "look";
        }).bounds(bx + 164, panelY + 68, 64, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.azscompanions.gather.start"), b -> start())
                .bounds(bx, panelY + panelH - 28, (bw - 8) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> {
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        }).bounds(bx + (bw - 8) / 2 + 8, panelY + panelH - 28, (bw - 8) / 2, 20).build());
    }

    private void start() {
        String item = itemBox != null ? itemBox.getValue().trim() : "";
        int count = 64;
        try {
            count = Integer.parseInt(countBox != null ? countBox.getValue().trim() : "64");
        } catch (NumberFormatException ignored) {
        }
        count = Math.max(1, Math.min(count, 1_000_000));
        com.azscompanions.network.ModNetworking.sendToServer(new CompanionGatherAssignPacket(
                companion.getId(), item, count, depositMode));
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, PANEL_EDGE);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG);
        graphics.drawCenteredString(font, title, panelX + panelW / 2, panelY + 10, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.azscompanions.gather.item"),
                panelX + 16, panelY + 26, 0xA0A0A0, false);
        graphics.drawString(font, Component.translatable("screen.azscompanions.gather.count"),
                panelX + 16, panelY + 58, 0xA0A0A0, false);
        graphics.drawString(font, Component.literal("Deposit: " + depositMode),
                panelX + 16, panelY + 94, 0x80C0FF, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
