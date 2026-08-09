package com.koncompanions.client.screen;

import com.koncompanions.menu.FabricRadialCommandMenu;
import com.koncompanions.network.FabricNetworkingClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FabricRadialCommandScreen extends AbstractContainerScreen<FabricRadialCommandMenu> {
    public FabricRadialCommandScreen(FabricRadialCommandMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 220;
        this.imageHeight = 140;
    }

    @Override
    protected void init() {
        super.init();
        int i = 0;
        for (FabricRadialCommandMenu.Command command : FabricRadialCommandMenu.Command.values()) {
            final FabricRadialCommandMenu.Command cmd = command;
            addRenderableWidget(Button.builder(Component.literal(cmd.name()), b -> {
                if (menu.companion() != null) {
                    FabricNetworkingClient.sendRadial(menu.companion().getId(), cmd.name());
                }
                onClose();
            }).bounds(leftPos + 20 + (i % 2) * 90, topPos + 30 + (i / 2) * 24, 80, 20).build());
            i++;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xE0141C28);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 3, 0xFFF2A7C0);
        graphics.drawString(font, title, leftPos + 12, topPos + 10, 0xE8F4FF, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
