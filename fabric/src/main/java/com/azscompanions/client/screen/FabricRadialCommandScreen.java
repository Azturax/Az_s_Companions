package com.azscompanions.client.screen;

import com.azscompanions.menu.FabricRadialCommandMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Container bridge → pie radial. */
public final class FabricRadialCommandScreen extends AbstractContainerScreen<FabricRadialCommandMenu> {
    public FabricRadialCommandScreen(FabricRadialCommandMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        if (menu.companion() != null) {
            Minecraft.getInstance().setScreen(new FabricCompanionRadialScreen(menu.companion().getId()));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }
}
