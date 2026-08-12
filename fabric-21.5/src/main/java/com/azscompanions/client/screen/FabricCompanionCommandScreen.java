package com.azscompanions.client.screen;

import com.azscompanions.AzsCompanionsFabric;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricCompanionMode;
import com.azscompanions.network.FabricNetworkingClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Companion movement commands with icon buttons + tooltips:
 * Follow / Stay / Sit / Wander. Close with ESC.
 */
public final class FabricCompanionCommandScreen extends Screen {
    private static final int PANEL_BG = 0xC0101010;
    private static final int PANEL_EDGE = 0xFF8B8B8B;
    private static final int ICON = 36;
    private static final int GAP = 8;

    private final FabricCompanionEntity companion;
    private final Screen parent;
    private int panelX;
    private int panelY;
    private final int panelW = 220;
    private final int panelH = 96;

    public FabricCompanionCommandScreen(FabricCompanionEntity companion, Screen parent) {
        super(Component.translatable("screen.azscompanions.command"));
        this.companion = companion;
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        int totalW = 4 * ICON + 3 * GAP;
        int bx = panelX + (panelW - totalW) / 2;
        int by = panelY + 44;

        addCommandIcon(bx, by, "follow", "FOLLOW",
                "screen.azscompanions.command.follow",
                "screen.azscompanions.command.follow.desc");
        addCommandIcon(bx + ICON + GAP, by, "stay", "STAY",
                "screen.azscompanions.command.stay",
                "screen.azscompanions.command.stay.desc");
        addCommandIcon(bx + 2 * (ICON + GAP), by, "sit", "SIT",
                "screen.azscompanions.command.sit",
                "screen.azscompanions.command.sit.desc");
        addCommandIcon(bx + 3 * (ICON + GAP), by, "wander", "WANDER",
                "screen.azscompanions.command.wander",
                "screen.azscompanions.command.wander.desc");
    }

    private void addCommandIcon(int x, int y, String texture, String command, String titleKey, String descKey) {
        addRenderableWidget(new CommandIconButton(
                x, y, ICON, ICON,
                icon(texture),
                tooltip(titleKey, descKey),
                b -> {
                    FabricNetworkingClient.sendMenuAction(companion.getId(), command);
                    if (minecraft != null) {
                        minecraft.setScreen(null);
                    }
                }));
    }

    private static ResourceLocation icon(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                AzsCompanionsFabric.MOD_ID, "textures/gui/commands/" + name + ".png");
    }

    private static Tooltip tooltip(String titleKey, String descKey) {
        return Tooltip.create(Component.translatable(titleKey)
                .append(CommonComponents.NEW_LINE)
                .append(Component.translatable(descKey).withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, PANEL_EDGE);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG);
        graphics.drawCenteredString(font, title, panelX + panelW / 2, panelY + 10, 0xFFFFFF);
        FabricCompanionMode mode = companion.getMode();
        graphics.drawCenteredString(font,
                Component.translatable("screen.azscompanions.command.current", mode.name().toLowerCase()),
                panelX + panelW / 2, panelY + 22, 0xA0A0A0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class CommandIconButton extends Button {
        private final ResourceLocation icon;

        CommandIconButton(int x, int y, int width, int height, ResourceLocation icon,
                Tooltip tooltip, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.icon = icon;
            setTooltip(tooltip);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int bg = isHoveredOrFocused() ? 0xFF606060 : 0xFF404040;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, bg);
            int pad = 2;
            int size = width - pad * 2;
            graphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, icon,
                    getX() + pad, getY() + pad, 0, 0, size, size, size, size);
        }
    }
}
