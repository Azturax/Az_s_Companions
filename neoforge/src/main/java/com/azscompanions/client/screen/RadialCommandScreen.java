package com.azscompanions.client.screen;

import com.azscompanions.menu.RadialCommandMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Container bridge: immediately swaps to the pie radial so legacy openRadial() still works.
 */
public final class RadialCommandScreen extends AbstractContainerScreen<RadialCommandMenu> {
    public RadialCommandScreen(RadialCommandMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        if (menu.companion() != null) {
            int id = menu.companion().getId();
            Minecraft.getInstance().setScreen(new CompanionRadialScreen(id));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }
}
