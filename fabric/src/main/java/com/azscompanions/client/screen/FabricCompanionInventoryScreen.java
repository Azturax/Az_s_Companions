package com.azscompanions.client.screen;

import com.azscompanions.menu.FabricCompanionInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Companion inventory: armor+shield, storage, 9 hotbar, gapped player inv; plain slots only. */
public final class FabricCompanionInventoryScreen extends AbstractContainerScreen<FabricCompanionInventoryMenu> {
    private static final ResourceLocation CHEST_BG =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final ResourceLocation INV_BG =
            ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");

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
        int x = leftPos;
        int y = topPos;
        int panelBottom = FabricCompanionInventoryMenu.COMPANION_PANEL_BOTTOM;

        graphics.fill(x, y, x + imageWidth, y + panelBottom, 0xFFC6C6C6);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + panelBottom - 2, 0xFF8B8B8B);
        graphics.fill(x + 3, y + 3, x + imageWidth - 3, y + panelBottom - 3, 0xFFC6C6C6);
        graphics.blit(CHEST_BG, x + (imageWidth - 176) / 2, y, 0, 0, 176, 17);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                blitSlot(graphics,
                        x + FabricCompanionInventoryMenu.STORAGE_X - 1 + col * 18,
                        y + FabricCompanionInventoryMenu.STORAGE_Y - 1 + row * 18);
            }
        }

        for (int i = 0; i < 5; i++) {
            blitSlot(graphics,
                    x + FabricCompanionInventoryMenu.ARMOR_X - 1,
                    y + FabricCompanionInventoryMenu.STORAGE_Y - 1 + i * 18);
        }

        for (int col = 0; col < 9; col++) {
            blitSlot(graphics,
                    x + FabricCompanionInventoryMenu.STORAGE_X - 1 + col * 18,
                    y + FabricCompanionInventoryMenu.COMPANION_HOTBAR_Y - 1);
        }

        int playerPanelX = x + FabricCompanionInventoryMenu.STORAGE_X - 8;
        int playerPanelY = y + FabricCompanionInventoryMenu.PLAYER_INV_Y - 12;
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
