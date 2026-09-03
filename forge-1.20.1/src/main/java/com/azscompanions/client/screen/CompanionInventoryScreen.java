package com.azscompanions.client.screen;

import com.azscompanions.AzsCompanionsConstants;
import com.azscompanions.menu.CompanionInventoryMenu;
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
public final class CompanionInventoryScreen extends AbstractContainerScreen<CompanionInventoryMenu> {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(
            AzsCompanionsConstants.MOD_ID, "textures/gui/companion_inventory.png");

    public CompanionInventoryScreen(CompanionInventoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, Component.translatable("screen.azscompanions.inventory.adventure"));
        this.imageWidth = CompanionInventoryMenu.IMAGE_WIDTH;
        this.imageHeight = CompanionInventoryMenu.IMAGE_HEIGHT;
        this.titleLabelX = CompanionInventoryMenu.STORAGE_X;
        this.titleLabelY = 6;
        this.inventoryLabelX = CompanionInventoryMenu.STORAGE_X;
        this.inventoryLabelY = CompanionInventoryMenu.PLAYER_INV_Y - 11;
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
