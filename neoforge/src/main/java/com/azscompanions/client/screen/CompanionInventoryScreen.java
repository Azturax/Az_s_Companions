package com.azscompanions.client.screen;

import com.azscompanions.menu.CompanionInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Companion inventory: armor+shield column, storage, 9-slot hotbar, gapped player inv.
 * Plain vanilla slot stencils only — no colored specialty-slot frames.
 */
public final class CompanionInventoryScreen extends AbstractContainerScreen<CompanionInventoryMenu> {
    private static final ResourceLocation CHEST_BG =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final ResourceLocation INV_BG =
            ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");

    public CompanionInventoryScreen(CompanionInventoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = CompanionInventoryMenu.IMAGE_WIDTH;
        this.imageHeight = CompanionInventoryMenu.IMAGE_HEIGHT;
        this.titleLabelX = CompanionInventoryMenu.STORAGE_X;
        this.titleLabelY = 6;
        this.inventoryLabelX = CompanionInventoryMenu.STORAGE_X;
        this.inventoryLabelY = CompanionInventoryMenu.PLAYER_INV_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        int panelBottom = CompanionInventoryMenu.COMPANION_PANEL_BOTTOM;

        // Companion panel (vanilla grey bezel)
        graphics.fill(x, y, x + imageWidth, y + panelBottom, 0xFFC6C6C6);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + panelBottom - 2, 0xFF8B8B8B);
        graphics.fill(x + 3, y + 3, x + imageWidth - 3, y + panelBottom - 3, 0xFFC6C6C6);
        graphics.blit(CHEST_BG, x + (imageWidth - 176) / 2, y, 0, 0, 176, 17);

        // Storage 3×9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                blitSlot(graphics,
                        x + CompanionInventoryMenu.STORAGE_X - 1 + col * 18,
                        y + CompanionInventoryMenu.STORAGE_Y - 1 + row * 18);
            }
        }

        // Armor + shield column (plain slots)
        for (int i = 0; i < 5; i++) {
            blitSlot(graphics,
                    x + CompanionInventoryMenu.ARMOR_X - 1,
                    y + CompanionInventoryMenu.STORAGE_Y - 1 + i * 18);
        }

        // Companion hotbar 9 slots (no colored strip)
        for (int col = 0; col < 9; col++) {
            blitSlot(graphics,
                    x + CompanionInventoryMenu.STORAGE_X - 1 + col * 18,
                    y + CompanionInventoryMenu.COMPANION_HOTBAR_Y - 1);
        }

        // Player inventory panel with clear vertical gap
        int playerPanelX = x + CompanionInventoryMenu.STORAGE_X - 8;
        int playerPanelY = y + CompanionInventoryMenu.PLAYER_INV_Y - 12;
        graphics.blit(CHEST_BG, playerPanelX, playerPanelY, 0, 126, 176, 96);
    }

    private static void blitSlot(GuiGraphics graphics, int sx, int sy) {
        graphics.blit(INV_BG, sx, sy, 7, 7, 18, 18);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
