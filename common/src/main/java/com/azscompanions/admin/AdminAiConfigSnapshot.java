package com.azscompanions.admin;

import com.azscompanions.ai.ChatListenMode;
import com.azscompanions.ai.CompanionAiSettings;
import com.azscompanions.ai.LlmProviderMode;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Editable subset of companion AI settings for the in-game admin panel.
 * Save writes to disk only — runtime LLM client is not hot-reloaded.
 */
public final class AdminAiConfigSnapshot {
    public static final int MAX_URL = 256;
    public static final int MAX_MODEL = 128;
    public static final int MAX_LANG = 16;
    public static final int MAX_ENV = 64;
    public static final int MAX_PROVIDER = 32;
    public static final int MAX_PROFILE = 32;
    public static final int MAX_LISTEN = 16;

    private String profileId = LlmProviderProfile.CUSTOM.name().toLowerCase();
    private String provider = "disabled";
    private String baseUrl = CompanionAiSettings.DEFAULT_BASE_URL;
    private String model = CompanionAiSettings.DEFAULT_MODEL;
    private String apiKeyEnv = CompanionAiSettings.DEFAULT_API_KEY_ENV;
    private String inputLanguage = "en";
    private String chatListenMode = "off";
    private boolean enableAiActions;
    private boolean serverLlmOnly = true;
    private boolean nameListen = true;
    private boolean enableChatMessages = true;
    private String mcpUrl = "http://127.0.0.1:3001/mcp";

    public static AdminAiConfigSnapshot fromSettings(CompanionAiSettings s) {
        AdminAiConfigSnapshot snap = new AdminAiConfigSnapshot();
        if (s == null) {
            snap.setProfileId(LlmProviderProfile.DISABLED.name().toLowerCase());
            return snap;
        }
        snap.provider = s.provider().name().toLowerCase();
        snap.baseUrl = s.baseUrl();
        snap.model = s.model();
        snap.apiKeyEnv = s.apiKeyEnv();
        snap.inputLanguage = s.inputLanguage();
        snap.chatListenMode = s.chatListenMode().configName();
        snap.enableAiActions = s.enableAiActions();
        snap.serverLlmOnly = s.serverLlmOnly();
        snap.nameListen = s.nameListen();
        snap.enableChatMessages = s.enableChatMessages();
        snap.mcpUrl = s.mcpUrl();
        snap.profileId = LlmProviderProfile.detect(snap).name().toLowerCase();
        return snap;
    }

    /** Merge editable fields onto a full settings copy (preserves MCP/FTB/etc. not shown in UI). */
    public CompanionAiSettings mergeInto(CompanionAiSettings base) {
        CompanionAiSettings out = base == null ? new CompanionAiSettings() : base.copy();
        out.setProvider(LlmProviderMode.fromConfig(provider));
        out.setBaseUrl(trimOr(baseUrl, CompanionAiSettings.DEFAULT_BASE_URL));
        out.setModel(trimOr(model, CompanionAiSettings.DEFAULT_MODEL));
        out.setApiKeyEnv(trimOr(apiKeyEnv, CompanionAiSettings.DEFAULT_API_KEY_ENV));
        out.setInputLanguage(trimOr(inputLanguage, "en"));
        out.setChatListenMode(ChatListenMode.fromConfig(chatListenMode));
        out.setEnableAiActions(enableAiActions);
        out.setServerLlmOnly(serverLlmOnly);
        out.setNameListen(nameListen);
        out.setEnableChatMessages(enableChatMessages);
        out.setMcpUrl(trimOr(mcpUrl, "http://127.0.0.1:3001/mcp"));
        return out;
    }

    /**
     * Light validation. Returns null when OK, otherwise a short error message.
     */
    public String validate() {
        LlmProviderMode mode = LlmProviderMode.fromConfig(provider);
        if (mode.usesOpenAiCompatibleHttp()) {
            String url = baseUrl == null ? "" : baseUrl.trim();
            if (url.isEmpty() || !(url.startsWith("http://") || url.startsWith("https://"))) {
                return "baseUrl must start with http:// or https://";
            }
            if (url.length() > MAX_URL) {
                return "baseUrl too long";
            }
        }
        if (mode == LlmProviderMode.MCP) {
            String url = mcpUrl == null ? "" : mcpUrl.trim();
            if (url.isEmpty() || !(url.startsWith("http://") || url.startsWith("https://"))) {
                return "mcpUrl must start with http:// or https://";
            }
        }
        if (model != null && model.length() > MAX_MODEL) {
            return "model too long";
        }
        if (inputLanguage != null && inputLanguage.length() > MAX_LANG) {
            return "inputLanguage too long";
        }
        if (apiKeyEnv != null && apiKeyEnv.length() > MAX_ENV) {
            return "apiKeyEnv too long";
        }
        return null;
    }

    public void applyProfile(LlmProviderProfile profile) {
        if (profile == null) {
            profile = LlmProviderProfile.CUSTOM;
        }
        profile.applyTo(this);
    }

    public LlmProviderProfile profile() {
        return LlmProviderProfile.fromId(profileId);
    }

    public String profileId() {
        return profileId;
    }

    public AdminAiConfigSnapshot setProfileId(String profileId) {
        this.profileId = blankTo(profileId, LlmProviderProfile.CUSTOM.name().toLowerCase());
        return this;
    }

    public String provider() {
        return provider;
    }

    public AdminAiConfigSnapshot setProvider(String provider) {
        this.provider = blankTo(provider, "disabled");
        return this;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public AdminAiConfigSnapshot setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        return this;
    }

    public String model() {
        return model;
    }

    public AdminAiConfigSnapshot setModel(String model) {
        this.model = model == null ? "" : model.trim();
        return this;
    }

    public String apiKeyEnv() {
        return apiKeyEnv;
    }

    public AdminAiConfigSnapshot setApiKeyEnv(String apiKeyEnv) {
        this.apiKeyEnv = apiKeyEnv == null ? "" : apiKeyEnv.trim();
        return this;
    }

    public String inputLanguage() {
        return inputLanguage;
    }

    public AdminAiConfigSnapshot setInputLanguage(String inputLanguage) {
        this.inputLanguage = blankTo(inputLanguage, "en");
        return this;
    }

    public String chatListenMode() {
        return chatListenMode;
    }

    public AdminAiConfigSnapshot setChatListenMode(String chatListenMode) {
        this.chatListenMode = ChatListenMode.fromConfig(chatListenMode).configName();
        return this;
    }

    public boolean enableAiActions() {
        return enableAiActions;
    }

    public AdminAiConfigSnapshot setEnableAiActions(boolean enableAiActions) {
        this.enableAiActions = enableAiActions;
        return this;
    }

    public boolean serverLlmOnly() {
        return serverLlmOnly;
    }

    public AdminAiConfigSnapshot setServerLlmOnly(boolean serverLlmOnly) {
        this.serverLlmOnly = serverLlmOnly;
        return this;
    }

    public boolean nameListen() {
        return nameListen;
    }

    public AdminAiConfigSnapshot setNameListen(boolean nameListen) {
        this.nameListen = nameListen;
        return this;
    }

    public boolean enableChatMessages() {
        return enableChatMessages;
    }

    public AdminAiConfigSnapshot setEnableChatMessages(boolean enableChatMessages) {
        this.enableChatMessages = enableChatMessages;
        return this;
    }

    public String mcpUrl() {
        return mcpUrl;
    }

    public AdminAiConfigSnapshot setMcpUrl(String mcpUrl) {
        this.mcpUrl = mcpUrl == null ? "" : mcpUrl.trim();
        return this;
    }

    /** Compact JSON for S2C/C2S admin packets (max ~2k). */
    public String toWireJson() {
        JsonObject o = new JsonObject();
        o.addProperty("profileId", profileId);
        o.addProperty("provider", provider);
        o.addProperty("baseUrl", baseUrl);
        o.addProperty("model", model);
        o.addProperty("apiKeyEnv", apiKeyEnv);
        o.addProperty("inputLanguage", inputLanguage);
        o.addProperty("chatListenMode", chatListenMode);
        o.addProperty("enableAiActions", enableAiActions);
        o.addProperty("serverLlmOnly", serverLlmOnly);
        o.addProperty("nameListen", nameListen);
        o.addProperty("enableChatMessages", enableChatMessages);
        o.addProperty("mcpUrl", mcpUrl);
        return o.toString();
    }

    public static AdminAiConfigSnapshot fromWireJson(String json) {
        AdminAiConfigSnapshot snap = new AdminAiConfigSnapshot();
        if (json == null || json.isBlank()) {
            return snap;
        }
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            if (o.has("profileId")) {
                snap.setProfileId(o.get("profileId").getAsString());
            }
            if (o.has("provider")) {
                snap.setProvider(o.get("provider").getAsString());
            }
            if (o.has("baseUrl")) {
                snap.setBaseUrl(o.get("baseUrl").getAsString());
            }
            if (o.has("model")) {
                snap.setModel(o.get("model").getAsString());
            }
            if (o.has("apiKeyEnv")) {
                snap.setApiKeyEnv(o.get("apiKeyEnv").getAsString());
            }
            if (o.has("inputLanguage")) {
                snap.setInputLanguage(o.get("inputLanguage").getAsString());
            }
            if (o.has("chatListenMode")) {
                snap.setChatListenMode(o.get("chatListenMode").getAsString());
            }
            if (o.has("enableAiActions")) {
                snap.setEnableAiActions(o.get("enableAiActions").getAsBoolean());
            }
            if (o.has("serverLlmOnly")) {
                snap.setServerLlmOnly(o.get("serverLlmOnly").getAsBoolean());
            }
            if (o.has("nameListen")) {
                snap.setNameListen(o.get("nameListen").getAsBoolean());
            }
            if (o.has("enableChatMessages")) {
                snap.setEnableChatMessages(o.get("enableChatMessages").getAsBoolean());
            }
            if (o.has("mcpUrl")) {
                snap.setMcpUrl(o.get("mcpUrl").getAsString());
            }
        } catch (Exception ignored) {
            // keep defaults
        }
        return snap;
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String trimOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
