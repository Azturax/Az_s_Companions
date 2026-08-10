package com.azscompanions.client.screen;

import com.azscompanions.entity.inventory.CompanionInventory;
import com.azscompanions.menu.CompanionInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Companion inventory with backpack + distinct armor/tool equipment strip.
 */
public final class CompanionInventoryScreen extends AbstractContainerScreen<CompanionInventoryMenu> {
    private static final ResourceLocation CHEST_BG =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final ResourceLocation INV_BG =
            ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");

    public CompanionInventoryScreen(CompanionInventoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = CompanionInventoryMenu.IMAGE_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.blit(CHEST_BG, x, y, 0, 0, imageWidth, 17 + 3 * 18);
        // Equipment strip tints (armor | tools | misc)
        graphics.fill(x + 7, y + 73, x + 80, y + 97, 0xFF3A4555);
        graphics.fill(x + 96, y + 73, x + 134, y + 97, 0xFF4A4530);
        graphics.fill(x + 133, y + 73, x + 169, y + 97, 0xFF3A3A3A);
        graphics.blit(CHEST_BG, x, y + 122, 0, 126, imageWidth, 96);

        for (Slot slot : menu.slots) {
            if (slot.index >= CompanionInventory.BACKPACK_SIZE && slot.index < CompanionInventory.TOTAL_SIZE) {
                graphics.blit(INV_BG, x + slot.x - 1, y + slot.y - 1, 7, 7, 18, 18);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
