package com.azscompanions.client.screen;

import com.azscompanions.AzsCompanionsFabric;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.network.FabricNetworkingClient;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared Shift+RMB companion menu: Customize, Command, Behavior, Inventory, Stats,
 * Remove/Dismiss child (store), stored-Bits badge, Donate.
 */
public final class FabricCompanionMenuScreen extends Screen {
    private static final int PANEL_BG = 0xC0101010;
    private static final int PANEL_EDGE = 0xFF8B8B8B;
    private static final ResourceLocation DONATE_ICON =
            ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "textures/gui/donate.png");
    private static final String DONATE_URL = "https://paypal.me/azturax";

    private final FabricCompanionEntity companion;
    private int panelX;
    private int panelY;
    private final int panelW = 220;
    private final int panelH = 230;

    public FabricCompanionMenuScreen(FabricCompanionEntity companion) {
        super(Component.translatable("screen.azscompanions.menu"));
        this.companion = companion;
    }

    @Override
    protected void init() {
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        int bx = panelX + 30;
        int by = panelY + 40;
        boolean child = companion.isChildCompanion();

        addRenderableWidget(Button.builder(Component.translatable("screen.azscompanions.customize"), b -> {
            if (minecraft != null) {
                minecraft.setScreen(new FabricCompanionCreatorScreen(companion, this));
            }
        }).bounds(bx, by, 160, 22).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.azscompanions.command"), b -> {
            if (minecraft != null) {
                minecraft.setScreen(new FabricCompanionCommandScreen(companion, this));
            }
        }).bounds(bx, by + 28, 160, 22).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.azscompanions.behavior"), b -> {
            if (minecraft != null) {
                minecraft.setScreen(new FabricCompanionBehaviorScreen(companion, this));
            }
        }).bounds(bx, by + 56, 160, 22).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.azscompanions.inventory.adventure"), b -> {
            FabricNetworkingClient.sendMenuAction(companion.getId(), "OPEN_INVENTORY");
        }).bounds(bx, by + 84, 160, 22).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.azscompanions.stats"), b -> {
            FabricNetworkingClient.sendMenuAction(companion.getId(), "OPEN_STATS");
        }).bounds(bx, by + 112, 160, 22).build());

        String removeKey = child
                ? "screen.azscompanions.dismiss_child"
                : "screen.azscompanions.remove_child";
        addRenderableWidget(Button.builder(Component.translatable(removeKey), b -> {
            FabricNetworkingClient.sendMenuAction(companion.getId(), "REMOVE_CHILD");
            onClose();
        }).bounds(bx, by + 140, 160, 22).build());

        if (!child) {
            addRenderableWidget(new StoredChildrenBadge(
                    panelX + 8, panelY + 8, 48, 20, companion,
                    b -> {
                        FabricNetworkingClient.sendMenuAction(companion.getId(), "CALL_STORED_CHILD");
                        onClose();
                    }));
        }

        addRenderableWidget(new IconButton(
                panelX + panelW - 28, panelY + 8, 20, 20, DONATE_ICON,
                b -> Util.getPlatform().openUri(DONATE_URL)));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, PANEL_EDGE);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG);
        graphics.drawCenteredString(font, title, panelX + panelW / 2, panelY + 12, 0xFFFFFF);
        String name = companion.getDisplayName().getString();
        graphics.drawCenteredString(font, name, panelX + panelW / 2, panelY + 24, 0xA0A0A0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class IconButton extends Button {
        private final ResourceLocation icon;

        IconButton(int x, int y, int width, int height, ResourceLocation icon, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.icon = icon;
            setTooltip(Tooltip.create(Component.translatable("screen.azscompanions.donate")));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int bg = isHoveredOrFocused() ? 0xFF606060 : 0xFF404040;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, bg);
            graphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, icon,
                    getX() + 2, getY() + 2, 0, 0, width - 4, height - 4, width - 4, height - 4);
        }
    }

    private static final class StoredChildrenBadge extends Button {
        private final FabricCompanionEntity companion;

        StoredChildrenBadge(int x, int y, int width, int height, FabricCompanionEntity companion, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.companion = companion;
            refreshTooltip();
        }

        private void refreshTooltip() {
            setTooltip(Tooltip.create(Component.translatable(
                    "screen.azscompanions.stored_children_tooltip",
                    companion.getStoredChildCount(),
                    companion.getMaxChildren())));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            refreshTooltip();
            int count = companion.getStoredChildCount();
            int max = companion.getMaxChildren();
            int bg = isHoveredOrFocused() ? 0xFF5A7088 : 0xFF3A4A5A;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, bg);
            int cx = getX() + 8;
            int cy = getY() + height / 2;
            graphics.fill(cx - 3, cy - 6, cx + 3, cy - 1, 0xFFE8C070);
            graphics.fill(cx - 2, cy - 1, cx + 2, cy + 6, 0xFFE8C070);
            graphics.drawString(
                    Minecraft.getInstance().font,
                    count + "/" + max,
                    getX() + 16,
                    getY() + (height - 8) / 2,
                    count > 0 ? 0xFFFFFF : 0xA0A0A0,
                    false);
        }
    }
}
