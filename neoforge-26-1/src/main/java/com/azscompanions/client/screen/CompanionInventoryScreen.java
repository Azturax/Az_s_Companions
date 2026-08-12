package com.azscompanions.client.screen;

import com.azscompanions.AzsCompanionsConstants;
import com.azscompanions.menu.CompanionInventoryMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Companion inventory: armor+shield column, storage, 9-slot hotbar, gapped player inv.
 */
public final class CompanionInventoryScreen extends AbstractContainerScreen<CompanionInventoryMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            AzsCompanionsConstants.MOD_ID, "textures/gui/companion_inventory.png");

    public CompanionInventoryScreen(CompanionInventoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, CompanionInventoryMenu.IMAGE_WIDTH, CompanionInventoryMenu.IMAGE_HEIGHT);
        this.titleLabelX = CompanionInventoryMenu.STORAGE_X;
        this.titleLabelY = 6;
        this.inventoryLabelX = CompanionInventoryMenu.STORAGE_X;
        this.inventoryLabelY = CompanionInventoryMenu.PLAYER_INV_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND,
                this.leftPos,
                this.topPos,
                0.0F,
                0.0F,
                this.imageWidth,
                this.imageHeight,
                256,
                256);
    }
}
