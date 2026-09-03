package com.azscompanions.client.screen;

import com.azscompanions.ai.ChatListenMode;
import com.azscompanions.entity.CompanionPlayerAiPrefs;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.network.FabricNetworking;
import com.azscompanions.network.FabricNetworkingClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Player-facing general companion settings (charm menu gear). Not LLM / API keys. */
public final class FabricCompanionGeneralSettingsScreen extends Screen {
    private static final int PANEL_BG = 0xC0101010;
    private static final int PANEL_EDGE = 0xFF8B8B8B;

    private final FabricCompanionEntity companion;
    private final Screen parent;
    private boolean showNameTag;
    private boolean teleportEnabled;
    private boolean globalTalk;
    private boolean idleChat;
    private ChatListenMode chatListen;
    private boolean chunkLoading;
    private int panelX;
    private int panelY;
    private final int panelW = 240;
    private final int panelH = 248;

    public FabricCompanionGeneralSettingsScreen(FabricCompanionEntity companion, Screen parent) {
        super(Component.translatable("screen.azscompanions.general_settings"));
        this.companion = companion;
        this.parent = parent;
        this.showNameTag = companion.isNameTagVisible();
        this.teleportEnabled = companion.isTeleportEnabled();
        this.globalTalk = companion.isGlobalTalkEnabled();
        this.idleChat = companion.isIdleChatEnabled();
        this.chatListen = companion.getChatListenMode();
        this.chunkLoading = companion.isChunkLoadingEnabled();
    }

    @Override
    protected void init() {
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        int bx = panelX + 20;
        int by = panelY + 36;
        int bw = panelW - 40;
        int half = (bw - 6) / 2;

        addRenderableWidget(Button.builder(Component.translatable("screen.azscompanions.command.follow"), b ->
                FabricNetworkingClient.sendMenuAction(companion.getId(), "FOLLOW")).bounds(bx, by, half, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.azscompanions.command.stay"), b ->
                FabricNetworkingClient.sendMenuAction(companion.getId(), "STAY")).bounds(bx + half + 6, by, half, 18).build());
        by += 22;
        addRenderableWidget(Button.builder(Component.translatable("screen.azscompanions.command.sit"), b ->
                FabricNetworkingClient.sendMenuAction(companion.getId(), "SIT")).bounds(bx, by, half, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.azscompanions.command.wander"), b ->
                FabricNetworkingClient.sendMenuAction(companion.getId(), "WANDER")).bounds(bx + half + 6, by, half, 18).build());
        by += 24;
        addRenderableWidget(Button.builder(nametagLabel(), b -> {
            showNameTag = !showNameTag;
            b.setMessage(nametagLabel());
            push();
        }).bounds(bx, by, bw, 18).build());
        by += 22;
        addRenderableWidget(Button.builder(teleportLabel(), b -> {
            teleportEnabled = !teleportEnabled;
            b.setMessage(teleportLabel());
            push();
        }).bounds(bx, by, bw, 18).build());
        by += 22;
        addRenderableWidget(Button.builder(idleLabel(), b -> {
            idleChat = !idleChat;
            b.setMessage(idleLabel());
            push();
        }).bounds(bx, by, bw, 18).build());
        by += 22;
        addRenderableWidget(Button.builder(globalTalkLabel(), b -> {
            globalTalk = !globalTalk;
            b.setMessage(globalTalkLabel());
            push();
        }).bounds(bx, by, bw, 18).build());
        by += 22;
        Button listenBtn = Button.builder(listenLabel(), b -> {
            chatListen = CompanionPlayerAiPrefs.cycleChatListen(chatListen);
            b.setMessage(listenLabel());
            push();
        }).bounds(bx, by, bw, 18).build();
        listenBtn.setTooltip(Tooltip.create(Component.translatable("screen.azscompanions.general_settings.reply_chat.desc")));
        addRenderableWidget(listenBtn);
        by += 22;
        addRenderableWidget(Button.builder(chunkLabel(), b -> {
            chunkLoading = !chunkLoading;
            b.setMessage(chunkLabel());
            push();
        }).bounds(bx, by, bw, 18).build());
        by += 24;
        addRenderableWidget(Button.builder(Component.translatable("screen.azscompanions.inventory.adventure"), b ->
                FabricNetworkingClient.sendMenuAction(companion.getId(), "OPEN_INVENTORY")).bounds(bx, by, bw, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> {
            push();
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        }).bounds(bx, panelY + panelH - 26, bw, 18).build());
    }

    private void push() {
        FabricNetworkingClient.sendGeneralSettings(new FabricNetworking.GeneralSettingsPayload(
                companion.getId(), showNameTag, teleportEnabled, globalTalk, idleChat,
                chatListen.configName(), chunkLoading));
    }

    private Component nametagLabel() {
        return Component.translatable(showNameTag
                ? "screen.azscompanions.general_settings.nametag_on"
                : "screen.azscompanions.general_settings.nametag_off");
    }

    private Component teleportLabel() {
        return Component.translatable(teleportEnabled
                ? "screen.azscompanions.general_settings.teleport_on"
                : "screen.azscompanions.general_settings.teleport_off");
    }

    private Component idleLabel() {
        return Component.translatable(idleChat
                ? "screen.azscompanions.general_settings.idle_on"
                : "screen.azscompanions.general_settings.idle_off");
    }

    private Component globalTalkLabel() {
        return Component.translatable(globalTalk
                ? "screen.azscompanions.general_settings.global_talk_on"
                : "screen.azscompanions.general_settings.global_talk_off");
    }

    private Component listenLabel() {
        String key = switch (chatListen) {
            case OFF -> "screen.azscompanions.general_settings.reply_off";
            case PLAYER -> "screen.azscompanions.general_settings.reply_owner";
            case GLOBAL -> "screen.azscompanions.general_settings.reply_global";
        };
        return Component.translatable(key);
    }

    private Component chunkLabel() {
        return Component.translatable(chunkLoading
                ? "screen.azscompanions.general_settings.chunks_on"
                : "screen.azscompanions.general_settings.chunks_off");
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, PANEL_EDGE);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG);
        graphics.drawCenteredString(font, title, panelX + panelW / 2, panelY + 10, 0xFFFFFF);
        graphics.drawCenteredString(font, companion.getDisplayName().getString(),
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
}
