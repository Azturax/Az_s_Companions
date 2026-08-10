package com.azscompanions.client.screen;

import com.azscompanions.admin.AdminAiConfigSnapshot;
import com.azscompanions.admin.LlmProviderProfile;
import com.azscompanions.ai.ChatListenMode;
import com.azscompanions.network.packet.AdminActionPacket;
import com.azscompanions.network.packet.AdminAiSavePacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-gated Az admin panel (NeoForge). */
public final class AzAdminScreen extends Screen {
    private static final int PANEL_BG = 0xC0101010;
    private static final int PANEL_EDGE = 0xFF8B8B8B;

    private enum Tab { OVERVIEW, AI }

    private final AdminAiConfigSnapshot snap;
    private final String aiStatus;
    private final boolean chunkLoading;
    private final boolean teamfight;
    private final String companionSummary;

    private Tab tab = Tab.AI;
    private LlmProviderProfile profile;
    private EditBox baseUrlBox;
    private EditBox modelBox;
    private EditBox apiKeyEnvBox;
    private EditBox languageBox;
    private EditBox providerBox;
    private EditBox mcpUrlBox;
    private Button profileButton;
    private Button listenButton;
    private Button actionsButton;
    private Button serverLlmButton;
    private Button nameListenButton;

    public AzAdminScreen(AdminAiConfigSnapshot snap, String aiStatus, boolean chunkLoading,
                         boolean teamfight, String companionSummary) {
        super(Component.literal("Az Admin"));
        this.snap = snap == null ? new AdminAiConfigSnapshot() : snap;
        this.aiStatus = aiStatus == null ? "" : aiStatus;
        this.chunkLoading = chunkLoading;
        this.teamfight = teamfight;
        this.companionSummary = companionSummary == null ? "" : companionSummary;
        this.profile = this.snap.profile();
        if (this.profile == LlmProviderProfile.CUSTOM) {
            this.profile = LlmProviderProfile.detect(this.snap);
            this.snap.setProfileId(this.profile.name().toLowerCase());
        }
    }

    @Override
    protected void init() {
        clearWidgets();
        int panelW = Math.min(360, width - 20);
        int panelH = Math.min(240, height - 20);
        int px = (width - panelW) / 2;
        int py = (height - panelH) / 2;
        int bx = px + 12;
        int bw = panelW - 24;

        addRenderableWidget(Button.builder(Component.literal(tab == Tab.OVERVIEW ? "[Overview]" : "Overview"),
                b -> {
                    tab = Tab.OVERVIEW;
                    init();
                }).bounds(bx, py + 8, bw / 2 - 4, 18).build());
        addRenderableWidget(Button.builder(Component.literal(tab == Tab.AI ? "[AI Config]" : "AI Config"),
                b -> {
                    tab = Tab.AI;
                    init();
                }).bounds(bx + bw / 2 + 4, py + 8, bw / 2 - 4, 18).build());

        if (tab == Tab.OVERVIEW) {
            initOverview(bx, py, bw, panelH);
        } else {
            initAi(bx, py, bw, panelH);
        }
    }

    private void initOverview(int bx, int py, int bw, int panelH) {
        int y = py + 34;
        addRenderableWidget(Button.builder(Component.literal("Teamfight: " + (teamfight ? "ON → OFF" : "OFF → ON")),
                b -> {
                    ClientPacketDistributor.sendToServer(new AdminActionPacket(teamfight ? "TEAMFIGHT_OFF" : "TEAMFIGHT_ON"));
                    onClose();
                }).bounds(bx, y, bw, 18).build());
        y += 22;
        addRenderableWidget(Button.builder(Component.literal("AI status (chat)"),
                b -> ClientPacketDistributor.sendToServer(new AdminActionPacket("AI_STATUS"))).bounds(bx, y, bw, 18).build());
        y += 22;
        addRenderableWidget(Button.builder(Component.literal("List companions by player"),
                b -> ClientPacketDistributor.sendToServer(new AdminActionPacket("LIST_COMPANIONS"))).bounds(bx, y, bw, 18).build());
        y += 22;
        addRenderableWidget(Button.builder(Component.literal("Dismiss my companions"),
                b -> ClientPacketDistributor.sendToServer(new AdminActionPacket("DISMISS_OWNED"))).bounds(bx, y, bw, 18).build());
        y += 22;
        addRenderableWidget(Button.builder(Component.literal("Chunk loading note"),
                b -> ClientPacketDistributor.sendToServer(new AdminActionPacket("CHUNK_NOTE"))).bounds(bx, y, bw, 18).build());
        y += 22;
        int half = (bw - 6) / 2;
        addRenderableWidget(Button.builder(Component.literal("Clear persona"),
                b -> ClientPacketDistributor.sendToServer(new AdminActionPacket("PERSONA_CLEAR_NEAREST"))).bounds(bx, y, half, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Show armor"),
                b -> ClientPacketDistributor.sendToServer(new AdminActionPacket("SHOW_ARMOR_NEAREST"))).bounds(bx + half + 6, y, half, 18).build());
        y += 22;
        addRenderableWidget(Button.builder(Component.literal("Hide armor"),
                b -> ClientPacketDistributor.sendToServer(new AdminActionPacket("HIDE_ARMOR_NEAREST"))).bounds(bx, y, half, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Reset behavior"),
                b -> ClientPacketDistributor.sendToServer(new AdminActionPacket("BEHAVIOR_RESET_NEAREST"))).bounds(bx + half + 6, y, half, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(bx, py + panelH - 26, bw, 18).build());
    }

    private void initAi(int bx, int py, int bw, int panelH) {
        int y = py + 34;
        profileButton = Button.builder(Component.literal("Profile: " + profile.label()), b -> cycleProfile())
                .bounds(bx, y, bw, 18).build();
        addRenderableWidget(profileButton);
        y += 22;

        boolean free = profile.allowsFreeProviderFields();
        providerBox = new EditBox(font, bx, y, bw, 16, Component.literal("provider"));
        providerBox.setMaxLength(AdminAiConfigSnapshot.MAX_PROVIDER);
        providerBox.setValue(snap.provider());
        providerBox.setEditable(free);
        providerBox.setHint(Component.literal("provider"));
        addRenderableWidget(providerBox);
        y += 20;

        baseUrlBox = new EditBox(font, bx, y, bw, 16, Component.literal("baseUrl"));
        baseUrlBox.setMaxLength(AdminAiConfigSnapshot.MAX_URL);
        baseUrlBox.setValue(snap.baseUrl());
        baseUrlBox.setEditable(free);
        baseUrlBox.setHint(Component.literal("baseUrl"));
        addRenderableWidget(baseUrlBox);
        y += 20;

        modelBox = new EditBox(font, bx, y, bw, 16, Component.literal("model"));
        modelBox.setMaxLength(AdminAiConfigSnapshot.MAX_MODEL);
        modelBox.setValue(snap.model());
        modelBox.setHint(Component.literal("model"));
        addRenderableWidget(modelBox);
        y += 20;

        apiKeyEnvBox = new EditBox(font, bx, y, bw, 16, Component.literal("apiKeyEnv"));
        apiKeyEnvBox.setMaxLength(AdminAiConfigSnapshot.MAX_ENV);
        apiKeyEnvBox.setValue(snap.apiKeyEnv());
        apiKeyEnvBox.setHint(Component.literal("apiKeyEnv"));
        addRenderableWidget(apiKeyEnvBox);
        y += 20;

        languageBox = new EditBox(font, bx, y, (bw - 6) / 2, 16, Component.literal("lang"));
        languageBox.setMaxLength(AdminAiConfigSnapshot.MAX_LANG);
        languageBox.setValue(snap.inputLanguage());
        languageBox.setHint(Component.literal("lang"));
        addRenderableWidget(languageBox);

        mcpUrlBox = new EditBox(font, bx + (bw - 6) / 2 + 6, y, (bw - 6) / 2, 16, Component.literal("mcpUrl"));
        mcpUrlBox.setMaxLength(AdminAiConfigSnapshot.MAX_URL);
        mcpUrlBox.setValue(snap.mcpUrl());
        mcpUrlBox.setHint(Component.literal("mcpUrl"));
        addRenderableWidget(mcpUrlBox);
        y += 20;

        listenButton = Button.builder(Component.literal("Listen: " + snap.chatListenMode()), b -> cycleListen())
                .bounds(bx, y, (bw - 6) / 2, 18).build();
        addRenderableWidget(listenButton);
        actionsButton = Button.builder(Component.literal("AI actions: " + onOff(snap.enableAiActions())),
                b -> {
                    snap.setEnableAiActions(!snap.enableAiActions());
                    actionsButton.setMessage(Component.literal("AI actions: " + onOff(snap.enableAiActions())));
                }).bounds(bx + (bw - 6) / 2 + 6, y, (bw - 6) / 2, 18).build();
        addRenderableWidget(actionsButton);
        y += 22;

        serverLlmButton = Button.builder(Component.literal("serverLlmOnly: " + onOff(snap.serverLlmOnly())),
                b -> {
                    snap.setServerLlmOnly(!snap.serverLlmOnly());
                    serverLlmButton.setMessage(Component.literal("serverLlmOnly: " + onOff(snap.serverLlmOnly())));
                }).bounds(bx, y, (bw - 6) / 2, 18).build();
        addRenderableWidget(serverLlmButton);
        nameListenButton = Button.builder(Component.literal("nameListen: " + onOff(snap.nameListen())),
                b -> {
                    snap.setNameListen(!snap.nameListen());
                    nameListenButton.setMessage(Component.literal("nameListen: " + onOff(snap.nameListen())));
                }).bounds(bx + (bw - 6) / 2 + 6, y, (bw - 6) / 2, 18).build();
        addRenderableWidget(nameListenButton);

        addRenderableWidget(Button.builder(Component.literal("Save (restart required)"), b -> save())
                .bounds(bx, py + panelH - 26, bw, 18).build());
    }

    private void cycleProfile() {
        profile = profile.next();
        profile.applyTo(snap);
        if (profileButton != null) {
            profileButton.setMessage(Component.literal("Profile: " + profile.label()));
        }
        init();
    }

    private void cycleListen() {
        ChatListenMode cur = ChatListenMode.fromConfig(snap.chatListenMode());
        ChatListenMode next = switch (cur) {
            case OFF -> ChatListenMode.PLAYER;
            case PLAYER -> ChatListenMode.GLOBAL;
            case GLOBAL -> ChatListenMode.OFF;
        };
        snap.setChatListenMode(next.configName());
        if (listenButton != null) {
            listenButton.setMessage(Component.literal("Listen: " + snap.chatListenMode()));
        }
    }

    private void save() {
        if (providerBox != null && profile.allowsFreeProviderFields()) {
            snap.setProvider(providerBox.getValue());
        }
        if (baseUrlBox != null && profile.allowsFreeProviderFields()) {
            snap.setBaseUrl(baseUrlBox.getValue());
        } else if (!profile.allowsFreeProviderFields() && profile.baseUrlOrNull() != null) {
            snap.setBaseUrl(profile.baseUrlOrNull());
            if (profile.providerOrNull() != null) {
                snap.setProvider(profile.providerOrNull().name().toLowerCase());
            }
        }
        if (modelBox != null) {
            snap.setModel(modelBox.getValue());
        }
        if (apiKeyEnvBox != null) {
            snap.setApiKeyEnv(apiKeyEnvBox.getValue());
        }
        if (languageBox != null) {
            snap.setInputLanguage(languageBox.getValue());
        }
        if (mcpUrlBox != null) {
            snap.setMcpUrl(mcpUrlBox.getValue());
        }
        snap.setProfileId(profile.name().toLowerCase());
        ClientPacketDistributor.sendToServer(new AdminAiSavePacket(snap.toWireJson()));
        onClose();
    }

    private static String onOff(boolean v) {
        return v ? "ON" : "OFF";
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelW = Math.min(360, width - 20);
        int panelH = Math.min(240, height - 20);
        int px = (width - panelW) / 2;
        int py = (height - panelH) / 2;
        graphics.fill(px - 1, py - 1, px + panelW + 1, py + panelH + 1, PANEL_EDGE);
        graphics.fill(px, py, px + panelW, py + panelH, PANEL_BG);
        graphics.text(font, tab == Tab.OVERVIEW ? "Az Admin" : "AI config → disk (restart)",
                px + 12, py - 12, 0xFFFFFF, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (tab == Tab.OVERVIEW) {
            int panelW = Math.min(360, width - 20);
            int px = (width - panelW) / 2;
            int py = (height - Math.min(240, height - 20)) / 2;
            graphics.text(font, truncate(aiStatus, 48), px + 12, py + panelHSafe() - 48, 0xA0A0A0, false);
            graphics.text(font, "chunks=" + chunkLoading + "  " + truncate(companionSummary, 40),
                    px + 12, py + panelHSafe() - 36, 0xA0A0A0, false);
        }
    }

    private int panelHSafe() {
        return Math.min(240, height - 20);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
