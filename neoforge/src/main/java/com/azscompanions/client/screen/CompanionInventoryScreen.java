package com.azscompanions.client.screen;

import com.azscompanions.menu.CompanionInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Companion inventory: left armor column, storage grid, companion hotbar on-panel, then player inv.
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
        this.inventoryLabelY = CompanionInventoryMenu.PLAYER_INV_Y - 11;
        this.titleLabelX = CompanionInventoryMenu.STORAGE_X;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        int panelBottom = CompanionInventoryMenu.COMPANION_HOTBAR_Y + 22;

        // Continuous companion panel (title → hotbar) so no slots float in a void
        graphics.fill(x, y, x + imageWidth, y + panelBottom, 0xFFC6C6C6);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + panelBottom - 2, 0xFF8B8B8B);
        graphics.fill(x + 3, y + 3, x + imageWidth - 3, y + panelBottom - 3, 0xFFC6C6C6);
        // Title bar accent from chest GUI
        graphics.blit(CHEST_BG, x + (imageWidth - 176) / 2, y, 0, 0, 176, 17);

        // Storage slot stencils
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                blitSlot(graphics,
                        x + CompanionInventoryMenu.STORAGE_X - 1 + col * 18,
                        y + CompanionInventoryMenu.STORAGE_Y - 1 + row * 18);
            }
        }

        // Armor column stencils (vanilla empty icons drawn by Slot.getNoItemIcon)
        for (int i = 0; i < 4; i++) {
            blitSlot(graphics,
                    x + CompanionInventoryMenu.ARMOR_X - 1,
                    y + CompanionInventoryMenu.STORAGE_Y - 1 + i * 18);
        }

        // Companion hotbar band under storage
        int hbY = y + CompanionInventoryMenu.COMPANION_HOTBAR_Y;
        graphics.fill(x + CompanionInventoryMenu.STORAGE_X - 3, hbY - 3,
                x + CompanionInventoryMenu.STORAGE_X + 6 * 18 + 1, hbY + 19,
                0xFF5A5040);
        for (Slot slot : menu.slots) {
            if (slot.y == CompanionInventoryMenu.COMPANION_HOTBAR_Y) {
                blitSlot(graphics, x + slot.x - 1, y + slot.y - 1);
            }
        }

        // Player inventory (vanilla chest footer alignment under companion panel)
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
