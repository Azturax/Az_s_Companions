package com.azscompanions.client.screen;

import com.azscompanions.config.ServerConfig;
import com.azscompanions.entity.CompanionDefinition;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionRegistry;
import com.azscompanions.menu.CompanionSelectionMenu;
import com.azscompanions.network.packet.RecruitCompanionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Companion selection UI. Defaults to the first registered definition (built-in Kon is an optional easter egg).
 */
public final class CompanionSelectionScreen extends AbstractContainerScreen<CompanionSelectionMenu> {
    private final List<CompanionDefinition> definitions = new ArrayList<>();
    private int selectedIndex;

    public CompanionSelectionScreen(CompanionSelectionMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 320;
        this.imageHeight = 220;
        definitions.addAll(CompanionRegistry.all());
        selectedIndex = 0;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos;
        int y = topPos;
        addRenderableWidget(Button.builder(Component.translatable("screen.azscompanions.recruit"), b -> recruit())
                .bounds(x + 210, y + 180, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("<"), b -> cycle(-1))
                .bounds(x + 20, y + 180, 30, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> cycle(1))
                .bounds(x + 60, y + 180, 30, 20).build());
    }

    private void cycle(int delta) {
        if (definitions.isEmpty()) {
            return;
        }
        selectedIndex = Math.floorMod(selectedIndex + delta, definitions.size());
    }

    private void recruit() {
        // Soft client check so the recruit button does not silently fail on limit.
        if (minecraft != null && minecraft.player != null && minecraft.level != null) {
            long owned = minecraft.level.getEntitiesOfClass(
                            CompanionEntity.class,
                            minecraft.player.getBoundingBox().inflate(512),
                            c -> minecraft.player.getUUID().equals(c.getOwnerUuid()))
                    .size();
            if (owned >= ServerConfig.MAX_COMPANIONS_PER_PLAYER.get()) {
                minecraft.player.displayClientMessage(
                        Component.translatable("message.azscompanions.limit_reached"), true);
                return;
            }
        }
        ResourceLocation id = definitions.isEmpty() ? CompanionRegistry.KON_ID : definitions.get(selectedIndex).id();
        com.azscompanions.network.ModNetworking.sendToServer(new RecruitCompanionPacket(id.toString()));
        // Keep this screen open on failure so the server limit message stays visible.
        // On success the server opens the companion creator, replacing this screen.
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x - 1, y - 1, x + imageWidth + 1, y + imageHeight + 1, 0xFF8B8B8B);
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xC0101010);
        graphics.fill(x + 12, y + 28, x + 150, y + 170, 0xFF2B2B2B);
        graphics.fill(x + 162, y + 28, x + 308, y + 170, 0xFF2B2B2B);

        CompanionDefinition selected = definitions.isEmpty() ? null : definitions.get(selectedIndex);
        String name = selected == null ? "Companion" : selected.displayName();
        String personality = selected == null
                ? "A loyal companion who stays close, follows commands, and watches your back."
                : selected.personality();

        graphics.drawString(font, Component.translatable("screen.azscompanions.selection"), x + 12, y + 10, 0xFFFFFF, false);
        graphics.drawString(font, name, x + 172, y + 40, 0xFFFFFF, false);
        graphics.drawWordWrap(font, Component.literal(personality), x + 172, y + 58, 126, 0xA0A0A0);
        graphics.drawString(font, Component.literal("Status: Available"), x + 172, y + 130, 0xA0A0A0, false);
        graphics.drawCenteredString(font, Component.literal(name.substring(0, 1).toUpperCase()), x + 81, y + 90, 0xE0E0E0);
        graphics.drawCenteredString(font, Component.literal("Preview"), x + 81, y + 110, 0x808080);
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // Skip fullscreen blur; panel provides chrome.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
