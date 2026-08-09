package com.koncompanions.client.screen;

import com.koncompanions.menu.RadialCommandMenu;
import com.koncompanions.network.packet.RadialCommandPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RadialCommandScreen extends AbstractContainerScreen<RadialCommandMenu> {
    public RadialCommandScreen(RadialCommandMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 240;
        this.imageHeight = 180;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos + 20;
        int y = topPos + 30;
        int i = 0;
        for (RadialCommandMenu.Command command : RadialCommandMenu.Command.values()) {
            final RadialCommandMenu.Command cmd = command;
            addRenderableWidget(Button.builder(Component.literal(pretty(command)), b -> send(cmd))
                    .bounds(x + (i % 3) * 70, y + (i / 3) * 24, 66, 20).build());
            i++;
        }
    }

    private void send(RadialCommandMenu.Command command) {
        if (menu.companion() != null) {
            PacketDistributor.sendToServer(new RadialCommandPacket(menu.companion().getId(), command.name()));
        }
        onClose();
    }

    private static String pretty(RadialCommandMenu.Command command) {
        String n = command.name().replace('_', ' ');
        return n.charAt(0) + n.substring(1).toLowerCase();
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
