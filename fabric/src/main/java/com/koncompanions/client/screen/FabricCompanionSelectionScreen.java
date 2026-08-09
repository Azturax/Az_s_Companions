package com.koncompanions.client.screen;

import com.koncompanions.KonCompanionsFabric;
import com.koncompanions.config.FabricServerConfig;
import com.koncompanions.entity.FabricCompanionDefinition;
import com.koncompanions.entity.FabricCompanionEntity;
import com.koncompanions.entity.FabricCompanionRegistry;
import com.koncompanions.menu.FabricCompanionSelectionMenu;
import com.koncompanions.network.FabricNetworkingClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public final class FabricCompanionSelectionScreen extends AbstractContainerScreen<FabricCompanionSelectionMenu> {
    private static final ResourceLocation KON_CARD =
            ResourceLocation.fromNamespaceAndPath(KonCompanionsFabric.MOD_ID, "textures/gui/kon_card.png");

    private final List<FabricCompanionDefinition> definitions = new ArrayList<>();
    private int selectedIndex;

    public FabricCompanionSelectionScreen(FabricCompanionSelectionMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 320;
        this.imageHeight = 220;
        definitions.addAll(FabricCompanionRegistry.all());
        selectedIndex = 0;
        for (int i = 0; i < definitions.size(); i++) {
            if (definitions.get(i).id().equals(FabricCompanionRegistry.KON_ID)) {
                selectedIndex = i;
                break;
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("screen.koncompanions.recruit"), b -> recruit())
                .bounds(leftPos + 210, topPos + 180, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("<"), b -> cycle(-1))
                .bounds(leftPos + 20, topPos + 180, 30, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> cycle(1))
                .bounds(leftPos + 60, topPos + 180, 30, 20).build());
    }

    private void cycle(int delta) {
        if (!definitions.isEmpty()) {
            selectedIndex = Math.floorMod(selectedIndex + delta, definitions.size());
        }
    }

    private void recruit() {
        ResourceLocation id = definitions.isEmpty() ? FabricCompanionRegistry.KON_ID : definitions.get(selectedIndex).id();
        // Soft client check so the recruit button does not silently dismiss on limit.
        if (minecraft != null && minecraft.player != null && minecraft.level != null) {
            long owned = minecraft.level.getEntitiesOfClass(
                            FabricCompanionEntity.class,
                            minecraft.player.getBoundingBox().inflate(512),
                            c -> minecraft.player.getUUID().equals(c.getOwnerUuid()))
                    .size();
            if (owned >= FabricServerConfig.MAX_COMPANIONS_PER_PLAYER) {
                minecraft.player.displayClientMessage(
                        Component.translatable("message.koncompanions.limit_reached"), true);
                return;
            }
        }
        FabricNetworkingClient.sendRecruit(id.toString());
        onClose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x - 1, y - 1, x + imageWidth + 1, y + imageHeight + 1, 0xFF8B8B8B);
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xC0101010);
        graphics.fill(x + 12, y + 28, x + 150, y + 170, 0xFF2B2B2B);
        graphics.fill(x + 162, y + 28, x + 308, y + 170, 0xFF2B2B2B);
        FabricCompanionDefinition selected = definitions.isEmpty() ? null : definitions.get(selectedIndex);
        String name = selected == null ? "Kon" : selected.displayName();
        String personality = selected == null
                ? "Gentle, loyal, slightly shy, practical, and encouraging."
                : selected.personality();
        graphics.drawString(font, title, x + 12, y + 10, 0xFFFFFF, false);
        graphics.drawString(font, name, x + 172, y + 40, 0xFFFFFF, false);
        graphics.drawWordWrap(font, Component.literal(personality), x + 172, y + 58, 126, 0xA0A0A0);
        graphics.drawString(font, "Status: Available", x + 172, y + 130, 0xA0A0A0, false);
        if (selected == null || selected.id().equals(FabricCompanionRegistry.KON_ID)) {
            graphics.blit(KON_CARD, x + 20, y + 36, 0, 0, 124, 124, 128, 128);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
