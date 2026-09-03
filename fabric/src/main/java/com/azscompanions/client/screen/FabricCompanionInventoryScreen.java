package com.azscompanions.client.screen;

import com.azscompanions.AzsCompanionsConstants;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.menu.FabricCompanionInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Companion adventure inventory: armor, storage, hotbar, player inv. */
public final class FabricCompanionInventoryScreen extends AbstractContainerScreen<FabricCompanionInventoryMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
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
    protected void init() {
        super.init();
        FabricCompanionEntity companion = menu.companion();
        if (companion == null) {
            return;
        }
        addRenderableWidget(new GearButton(leftPos + imageWidth - 22, topPos + 4, 16, 16, b -> {
            if (minecraft != null) {
                minecraft.setScreen(new FabricCompanionGeneralSettingsScreen(companion, this));
            }
        }));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private static final class GearButton extends Button {
        GearButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            setTooltip(Tooltip.create(Component.translatable("screen.azscompanions.general_settings")));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int bg = isHoveredOrFocused() ? 0xFF606060 : 0xFF3A3A3A;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, bg);
            int cx = getX() + width / 2;
            int cy = getY() + height / 2;
            graphics.fill(cx - 4, cy - 1, cx + 4, cy + 2, 0xFFC0C0C0);
            graphics.fill(cx - 1, cy - 4, cx + 2, cy + 4, 0xFFC0C0C0);
            graphics.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFFA8A8A8);
            graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFF404040);
        }
    }
}
