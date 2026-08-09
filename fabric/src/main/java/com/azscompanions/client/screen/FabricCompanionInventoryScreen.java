package com.azscompanions.client.screen;

import com.azscompanions.menu.FabricCompanionInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FabricCompanionInventoryScreen extends AbstractContainerScreen<FabricCompanionInventoryMenu> {
    public FabricCompanionInventoryScreen(FabricCompanionInventoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 218;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xF0182230);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 3, 0xFF8B8B8B);
        graphics.drawString(font, title, leftPos + 8, topPos + 6, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("screen.azscompanions.cosmetics"),
                leftPos + 62, topPos + 98, 0xA0A0A0, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
