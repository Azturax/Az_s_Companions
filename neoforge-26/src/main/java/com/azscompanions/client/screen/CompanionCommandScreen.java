package com.azscompanions.client.screen;

import com.azscompanions.AzsCompanions;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.network.packet.CompanionCommandPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Companion movement commands with icon buttons + tooltips:
 * Follow / Stay / Sit / Wander. Close with ESC.
 */
public final class CompanionCommandScreen extends Screen {
    private static final int PANEL_BG = 0xC0101010;
    private static final int PANEL_EDGE = 0xFF8B8B8B;
    private static final int ICON = 36;
    private static final int GAP = 8;

    private final CompanionEntity companion;
    private final Screen parent;
    private int panelX;
    private int panelY;
    private final int panelW = 220;
    private final int panelH = 96;

    public CompanionCommandScreen(CompanionEntity companion, Screen parent) {
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
                    ClientPacketDistributor.sendToServer(new CompanionCommandPacket(companion.getId(), command));
                    if (minecraft != null) {
                        minecraft.gui.setScreen(null);
                    }
                }));
    }

    private static Identifier icon(String name) {
        return Identifier.fromNamespaceAndPath(
                AzsCompanions.MOD_ID, "textures/gui/commands/" + name + ".png");
    }

    private static Tooltip tooltip(String titleKey, String descKey) {
        return Tooltip.create(Component.translatable(titleKey)
                .append(CommonComponents.NEW_LINE)
                .append(Component.translatable(descKey).withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, PANEL_EDGE);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG);
        graphics.text(font, title, panelX + panelW / 2, panelY + 12, 0xFFFFFF);
        CompanionMode mode = companion.getMode();
        graphics.text(font, Component.translatable("screen.azscompanions.command.current", mode.getSerializedName()),
                panelX + panelW / 2, panelY + 24, 0xA0A0A0);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class CommandIconButton extends Button {
        private final Identifier icon;

        CommandIconButton(int x, int y, int width, int height, Identifier icon,
                Tooltip tooltip, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.icon = icon;
            setTooltip(tooltip);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int bg = isHoveredOrFocused() ? 0xFF606060 : 0xFF404040;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, bg);
            int pad = 2;
            int size = width - pad * 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, icon, getX() + pad, getY() + pad, 0, 0, size, size, size, size);
        }
    }
}
