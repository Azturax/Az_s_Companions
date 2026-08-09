package com.koncompanions.client.screen;

import com.koncompanions.KonCompanions;
import com.koncompanions.config.ServerConfig;
import com.koncompanions.entity.CompanionDefinition;
import com.koncompanions.entity.CompanionEntity;
import com.koncompanions.entity.CompanionRegistry;
import com.koncompanions.menu.CompanionSelectionMenu;
import com.koncompanions.network.packet.RecruitCompanionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Polished companion selection UI with large cards. Kon is pre-selected for first-time players.
 * Kon's card uses the official portrait derived from the character reference art.
 */
public final class CompanionSelectionScreen extends AbstractContainerScreen<CompanionSelectionMenu> {
    private static final ResourceLocation KON_CARD =
            ResourceLocation.fromNamespaceAndPath(KonCompanions.MOD_ID, "textures/gui/kon_card.png");

    private final List<CompanionDefinition> definitions = new ArrayList<>();
    private int selectedIndex;

    public CompanionSelectionScreen(CompanionSelectionMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 320;
        this.imageHeight = 220;
        definitions.addAll(CompanionRegistry.all());
        if (definitions.isEmpty()) {
            // Datapack not synced to client registry yet — still default to Kon id.
            selectedIndex = 0;
        } else {
            selectedIndex = 0;
            for (int i = 0; i < definitions.size(); i++) {
                if (definitions.get(i).id().equals(CompanionRegistry.KON_ID)) {
                    selectedIndex = i;
                    break;
                }
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos;
        int y = topPos;
        addRenderableWidget(Button.builder(Component.translatable("screen.koncompanions.recruit"), b -> recruit())
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
                        Component.translatable("message.koncompanions.limit_reached"), true);
                return;
            }
        }
        ResourceLocation id = definitions.isEmpty() ? CompanionRegistry.KON_ID : definitions.get(selectedIndex).id();
        PacketDistributor.sendToServer(new RecruitCompanionPacket(id.toString()));
        // Keep this screen open on failure so the server limit message stays visible.
        // On success the server opens the companion creator, replacing this screen.
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        // Vanilla-like dark panel (no cyan chrome).
        graphics.fill(x - 1, y - 1, x + imageWidth + 1, y + imageHeight + 1, 0xFF8B8B8B);
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xC0101010);
        graphics.fill(x + 12, y + 28, x + 150, y + 170, 0xFF2B2B2B);
        graphics.fill(x + 162, y + 28, x + 308, y + 170, 0xFF2B2B2B);

        CompanionDefinition selected = definitions.isEmpty() ? null : definitions.get(selectedIndex);
        String name = selected == null ? "Kon" : selected.displayName();
        String personality = selected == null
                ? "Gentle, loyal, slightly shy, practical, and encouraging."
                : selected.personality();

        graphics.drawString(font, Component.translatable("screen.koncompanions.selection"), x + 12, y + 10, 0xFFFFFF, false);
        graphics.drawString(font, name, x + 172, y + 40, 0xFFFFFF, false);
        graphics.drawWordWrap(font, Component.literal(personality), x + 172, y + 58, 126, 0xA0A0A0);
        graphics.drawString(font, Component.literal("Status: Available"), x + 172, y + 130, 0xA0A0A0, false);

        boolean isKon = selected == null || selected.id().equals(CompanionRegistry.KON_ID);
        if (isKon) {
            graphics.blit(KON_CARD, x + 20, y + 36, 0, 0, 124, 124, 128, 128);
        } else {
            graphics.drawString(font, Component.literal("Preview"), x + 55, y + 90, 0xA0A0A0, false);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Skip fullscreen blur; panel provides chrome.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
