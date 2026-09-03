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
 * Background: light-grey beveled panels + light slot grid ({@code companion_inventory.png}).
 * Vanilla armor/shield empty icons remain.
 */
public final class FabricCompanionInventoryScreen extends AbstractContainerScreen<FabricCompanionInventoryMenu> {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(
            AzsCompanionsConstants.MOD_ID, "textures/gui/companion_inventory.png");

    public FabricCompanionInventoryScreen(FabricCompanionInventoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, Component.translatable("screen.azscompanions.inventory.adventure"));
        this.imageWidth = FabricCompanionInventoryMenu.IMAGE_WIDTH;
        this.imageHeight = FabricCompanionInventoryMenu.IMAGE_HEIGHT;
        this.titleLabelX = FabricCompanionInventoryMenu.STORAGE_X;
        this.titleLabelY = 6;
        this.inventoryLabelX = FabricCompanionInventoryMenu.STORAGE_X;
        this.inventoryLabelY = FabricCompanionInventoryMenu.PLAYER_INV_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Explicit 256×256 atlas size so UV mapping matches companion_inventory.png.
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
