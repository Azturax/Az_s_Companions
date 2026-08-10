package com.azscompanions.client.screen;

import com.azscompanions.admin.AdminAiConfigSnapshot;
import com.azscompanions.admin.LlmProviderProfile;
import com.azscompanions.network.FabricNetworkingClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Server-gated Az admin panel: overview actions + in-game LLM config (save → disk, restart required).
 * Profiles fill defaults; all fields stay editable. Chat is ask-only ({@code /ask} / {@code /az ask}).
 */
public final class FabricAzAdminScreen extends Screen {
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
    private Button serverLlmButton;
    private boolean syncingFields;

    public FabricAzAdminScreen(AdminAiConfigSnapshot snap, String aiStatus, boolean chunkLoading,
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
                    FabricNetworkingClient.sendAdminAction(teamfight ? "TEAMFIGHT_OFF" : "TEAMFIGHT_ON");
                    onClose();
                }).bounds(bx, y, bw, 18).build());
        y += 22;
        addRenderableWidget(Button.builder(Component.literal("AI status (chat)"),
                b -> FabricNetworkingClient.sendAdminAction("AI_STATUS")).bounds(bx, y, bw, 18).build());
        y += 22;
        addRenderableWidget(Button.builder(Component.literal("List companions by player"),
                b -> FabricNetworkingClient.sendAdminAction("LIST_COMPANIONS")).bounds(bx, y, bw, 18).build());
        y += 22;
        addRenderableWidget(Button.builder(Component.literal("Dismiss my companions"),
                b -> FabricNetworkingClient.sendAdminAction("DISMISS_OWNED")).bounds(bx, y, bw, 18).build());
        y += 22;
        addRenderableWidget(Button.builder(Component.literal("Chunk loading note"),
                b -> FabricNetworkingClient.sendAdminAction("CHUNK_NOTE")).bounds(bx, y, bw, 18).build());
        y += 22;
        int half = (bw - 6) / 2;
        addRenderableWidget(Button.builder(Component.literal("Clear persona"),
                b -> FabricNetworkingClient.sendAdminAction("PERSONA_CLEAR_NEAREST")).bounds(bx, y, half, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Show armor"),
                b -> FabricNetworkingClient.sendAdminAction("SHOW_ARMOR_NEAREST")).bounds(bx + half + 6, y, half, 18).build());
        y += 22;
        addRenderableWidget(Button.builder(Component.literal("Hide armor"),
                b -> FabricNetworkingClient.sendAdminAction("HIDE_ARMOR_NEAREST")).bounds(bx, y, half, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Reset behavior"),
                b -> FabricNetworkingClient.sendAdminAction("BEHAVIOR_RESET_NEAREST")).bounds(bx + half + 6, y, half, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(bx, py + panelH - 26, bw, 18).build());
    }

    private void initAi(int bx, int py, int bw, int panelH) {
        int y = py + 34;
        profileButton = Button.builder(Component.literal("Profile: " + profile.label()), b -> cycleProfile())
                .bounds(bx, y, bw, 18).build();
        addRenderableWidget(profileButton);
        y += 22;

        syncingFields = true;
        providerBox = new EditBox(font, bx, y, bw, 16, Component.literal("provider"));
        providerBox.setMaxLength(AdminAiConfigSnapshot.MAX_PROVIDER);
        providerBox.setValue(snap.provider());
        providerBox.setEditable(true);
        providerBox.setHint(Component.literal("provider"));
        providerBox.setResponder(v -> onAiFieldEdited());
        addRenderableWidget(providerBox);
        y += 20;

        baseUrlBox = new EditBox(font, bx, y, bw, 16, Component.literal("baseUrl"));
        baseUrlBox.setMaxLength(AdminAiConfigSnapshot.MAX_URL);
        baseUrlBox.setValue(snap.baseUrl());
        baseUrlBox.setEditable(true);
        baseUrlBox.setHint(Component.literal("baseUrl"));
        baseUrlBox.setResponder(v -> onAiFieldEdited());
        addRenderableWidget(baseUrlBox);
        y += 20;

        modelBox = new EditBox(font, bx, y, bw, 16, Component.literal("model"));
        modelBox.setMaxLength(AdminAiConfigSnapshot.MAX_MODEL);
        modelBox.setValue(snap.model());
        modelBox.setEditable(true);
        modelBox.setHint(Component.literal("model"));
        addRenderableWidget(modelBox);
        y += 20;

        apiKeyEnvBox = new EditBox(font, bx, y, bw, 16, Component.literal("apiKeyEnv"));
        apiKeyEnvBox.setMaxLength(AdminAiConfigSnapshot.MAX_ENV);
        apiKeyEnvBox.setValue(snap.apiKeyEnv());
        apiKeyEnvBox.setEditable(true);
        apiKeyEnvBox.setHint(Component.literal("apiKeyEnv"));
        addRenderableWidget(apiKeyEnvBox);
        y += 20;

        languageBox = new EditBox(font, bx, y, (bw - 6) / 2, 16, Component.literal("lang"));
        languageBox.setMaxLength(AdminAiConfigSnapshot.MAX_LANG);
        languageBox.setValue(snap.inputLanguage());
        languageBox.setEditable(true);
        languageBox.setHint(Component.literal("lang"));
        addRenderableWidget(languageBox);

        mcpUrlBox = new EditBox(font, bx + (bw - 6) / 2 + 6, y, (bw - 6) / 2, 16, Component.literal("mcpUrl"));
        mcpUrlBox.setMaxLength(AdminAiConfigSnapshot.MAX_URL);
        mcpUrlBox.setValue(snap.mcpUrl());
        mcpUrlBox.setEditable(true);
        mcpUrlBox.setHint(Component.literal("mcpUrl"));
        mcpUrlBox.setResponder(v -> onAiFieldEdited());
        addRenderableWidget(mcpUrlBox);
        y += 20;
        syncingFields = false;

        serverLlmButton = Button.builder(Component.literal("serverLlmOnly: " + onOff(snap.serverLlmOnly())),
                b -> {
                    snap.setServerLlmOnly(!snap.serverLlmOnly());
                    serverLlmButton.setMessage(Component.literal("serverLlmOnly: " + onOff(snap.serverLlmOnly())));
                }).bounds(bx, y, bw, 18).build();
        addRenderableWidget(serverLlmButton);

        addRenderableWidget(Button.builder(Component.literal("Save (restart required)"), b -> save())
                .bounds(bx, py + panelH - 26, bw, 18).build());
    }

    private void onAiFieldEdited() {
        if (syncingFields) {
            return;
        }
        if (providerBox != null) {
            snap.setProvider(providerBox.getValue());
        }
        if (baseUrlBox != null) {
            snap.setBaseUrl(baseUrlBox.getValue());
        }
        if (mcpUrlBox != null) {
            snap.setMcpUrl(mcpUrlBox.getValue());
        }
        LlmProviderProfile detected = LlmProviderProfile.detect(snap);
        if (detected != profile) {
            profile = detected;
            snap.setProfileId(profile.name().toLowerCase());
            if (profileButton != null) {
                profileButton.setMessage(Component.literal("Profile: " + profile.label()));
            }
        }
    }

    private void cycleProfile() {
        profile = profile.next();
        profile.applyTo(snap);
        if (profileButton != null) {
            profileButton.setMessage(Component.literal("Profile: " + profile.label()));
        }
        init();
    }

    private void save() {
        if (providerBox != null) {
            snap.setProvider(providerBox.getValue());
        }
        if (baseUrlBox != null) {
            snap.setBaseUrl(baseUrlBox.getValue());
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
        profile = LlmProviderProfile.detect(snap);
        snap.setProfileId(profile.name().toLowerCase());
        FabricNetworkingClient.sendAdminAiSave(snap);
        onClose();
    }

    private static String onOff(boolean v) {
        return v ? "ON" : "OFF";
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int panelW = Math.min(360, width - 20);
        int panelH = Math.min(240, height - 20);
        int px = (width - panelW) / 2;
        int py = (height - panelH) / 2;
        graphics.fill(px - 1, py - 1, px + panelW + 1, py + panelH + 1, PANEL_EDGE);
        graphics.fill(px, py, px + panelW, py + panelH, PANEL_BG);
        if (tab == Tab.OVERVIEW) {
            graphics.drawString(font, "Az Admin", px + 12, py - 12, 0xFFFFFF, false);
        } else {
            graphics.drawString(font, "AI config → disk (restart) · /ask only", px + 12, py - 12, 0xFFFFFF, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (tab == Tab.OVERVIEW) {
            int panelW = Math.min(360, width - 20);
            int px = (width - panelW) / 2;
            int py = (height - Math.min(240, height - 20)) / 2;
            graphics.drawString(font, truncate(aiStatus, 48), px + 12, py + panelHSafe() - 48, 0xA0A0A0, false);
            graphics.drawString(font, "chunks=" + chunkLoading + "  " + truncate(companionSummary, 40),
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
