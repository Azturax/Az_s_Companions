package com.azscompanions.client.screen;

import com.azscompanions.AzsCompanionsConstants;
import com.azscompanions.menu.FabricCompanionInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Companion inventory: armor+shield column, storage, 9-slot hotbar, gapped player inv.
 * Background texture matches slot coordinates exactly; vanilla armor/shield empty icons remain.
 */
public final class FabricCompanionInventoryScreen extends AbstractContainerScreen<FabricCompanionInventoryMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            AzsCompanionsConstants.MOD_ID, "textures/gui/companion_inventory.png");

    public FabricCompanionInventoryScreen(FabricCompanionInventoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = FabricCompanionInventoryMenu.IMAGE_WIDTH;
        this.imageHeight = FabricCompanionInventoryMenu.IMAGE_HEIGHT;
        this.titleLabelX = FabricCompanionInventoryMenu.STORAGE_X;
        this.titleLabelY = 6;
        this.inventoryLabelX = FabricCompanionInventoryMenu.STORAGE_X;
        this.inventoryLabelY = FabricCompanionInventoryMenu.PLAYER_INV_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
