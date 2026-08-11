package com.azscompanions.admin;

import com.azscompanions.ai.CompanionAiSettings;
import com.azscompanions.ai.LlmProviderMode;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Editable subset of companion AI settings for the in-game admin panel.
 * Save writes server config and applies to {@link com.azscompanions.ai.CompanionAiRuntime}.
 * Ask-only: no chatListen / nameListen / enableAiActions (use {@code /ask}).
 *
 * <p>API key security: S2C wire JSON never includes the plaintext key — only
 * {@code apiKeyStatus} ({@code config}/{@code env}/{@code none}). C2S may include
 * {@code apiKeyUpdate} when the admin typed a new key or cleared it; omit to leave unchanged.
 */
public final class AdminAiConfigSnapshot {
    public static final int MAX_URL = 256;
    public static final int MAX_MODEL = 128;
    public static final int MAX_LANG = 16;
    public static final int MAX_ENV = 64;
    public static final int MAX_PROVIDER = 32;
    public static final int MAX_PROFILE = 32;
    public static final int MAX_API_KEY = 512;
    /** Max UTF length for admin AI JSON packets (includes optional apiKeyUpdate). */
    public static final int MAX_WIRE_JSON = 8192;

    public static final String API_KEY_STATUS_NONE = "none";
    public static final String API_KEY_STATUS_CONFIG = "config";
    public static final String API_KEY_STATUS_ENV = "env";

    private String profileId = LlmProviderProfile.CUSTOM.name().toLowerCase();
    private String provider = "disabled";
    private String baseUrl = CompanionAiSettings.DEFAULT_BASE_URL;
    private String model = CompanionAiSettings.DEFAULT_MODEL;
    private String apiKeyEnv = CompanionAiSettings.DEFAULT_API_KEY_ENV;
    /** S2C-only status; never a secret. */
    private String apiKeyStatus = API_KEY_STATUS_NONE;
    /**
     * C2S-only: {@code null} = leave {@code apiKey} unchanged on merge;
     * non-null (including empty) = write that value to config.
     */
    private String apiKeyUpdate = null;
    private String inputLanguage = "en";
    private boolean serverLlmOnly = false;
    private boolean idleChat = true;
    private boolean enableChatMessages = true;
    private String mcpUrl = "http://127.0.0.1:3001/mcp";

    public static AdminAiConfigSnapshot fromSettings(CompanionAiSettings s) {
        AdminAiConfigSnapshot snap = new AdminAiConfigSnapshot();
        if (s == null) {
            snap.setProfileId(LlmProviderProfile.DISABLED.name().toLowerCase());
            snap.apiKeyStatus = API_KEY_STATUS_NONE;
            return snap;
        }
        snap.provider = s.provider().name().toLowerCase();
        snap.baseUrl = s.baseUrl();
        snap.model = s.model();
        snap.apiKeyEnv = s.apiKeyEnv();
        snap.apiKeyStatus = resolveApiKeyStatus(s);
        snap.apiKeyUpdate = null;
        snap.inputLanguage = s.inputLanguage();
        snap.serverLlmOnly = s.serverLlmOnly();
        snap.idleChat = s.idleChat();
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
        if (apiKeyUpdate != null) {
            out.setApiKey(apiKeyUpdate);
        }
        out.setInputLanguage(trimOr(inputLanguage, "en"));
        out.setServerLlmOnly(serverLlmOnly);
        out.setIdleChat(idleChat);
        out.setEnableChatMessages(enableChatMessages);
        out.setMcpUrl(trimOr(mcpUrl, "http://127.0.0.1:3001/mcp"));
        // Retired: always force ask-only defaults
        out.setChatListenMode(com.azscompanions.ai.ChatListenMode.OFF);
        out.setNameListen(false);
        out.setEnableAiActions(false);
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
        if (apiKeyUpdate != null && apiKeyUpdate.length() > MAX_API_KEY) {
            return "apiKey too long";
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

    /** {@link #API_KEY_STATUS_CONFIG}, {@link #API_KEY_STATUS_ENV}, or {@link #API_KEY_STATUS_NONE}. */
    public String apiKeyStatus() {
        return apiKeyStatus == null || apiKeyStatus.isBlank() ? API_KEY_STATUS_NONE : apiKeyStatus;
    }

    public AdminAiConfigSnapshot setApiKeyStatus(String apiKeyStatus) {
        if (API_KEY_STATUS_CONFIG.equals(apiKeyStatus) || API_KEY_STATUS_ENV.equals(apiKeyStatus)) {
            this.apiKeyStatus = apiKeyStatus;
        } else {
            this.apiKeyStatus = API_KEY_STATUS_NONE;
        }
        return this;
    }

    /** Human-readable status for the admin UI (no secret). */
    public String apiKeyStatusLabel() {
        return switch (apiKeyStatus()) {
            case API_KEY_STATUS_CONFIG -> "API key: set (config — wins over env)";
            case API_KEY_STATUS_ENV -> "API key: set (env)";
            default -> "API key: not set";
        };
    }

    /**
     * Pending C2S key write, or {@code null} if unchanged.
     * Empty string means clear the stored config key.
     */
    public String apiKeyUpdate() {
        return apiKeyUpdate;
    }

    public boolean hasApiKeyUpdate() {
        return apiKeyUpdate != null;
    }

    public AdminAiConfigSnapshot setApiKeyUpdate(String apiKeyUpdate) {
        this.apiKeyUpdate = apiKeyUpdate == null ? "" : apiKeyUpdate.trim();
        return this;
    }

    public AdminAiConfigSnapshot clearApiKeyUpdate() {
        this.apiKeyUpdate = null;
        return this;
    }

    public String inputLanguage() {
        return inputLanguage;
    }

    public AdminAiConfigSnapshot setInputLanguage(String inputLanguage) {
        this.inputLanguage = blankTo(inputLanguage, "en");
        return this;
    }

    public boolean serverLlmOnly() {
        return serverLlmOnly;
    }

    public AdminAiConfigSnapshot setServerLlmOnly(boolean serverLlmOnly) {
        this.serverLlmOnly = serverLlmOnly;
        return this;
    }

    public boolean idleChat() {
        return idleChat;
    }

    public AdminAiConfigSnapshot setIdleChat(boolean idleChat) {
        this.idleChat = idleChat;
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

    /** Compact JSON for S2C/C2S admin packets. Never includes stored plaintext key on open. */
    public String toWireJson() {
        JsonObject o = new JsonObject();
        o.addProperty("profileId", profileId);
        o.addProperty("provider", provider);
        o.addProperty("baseUrl", baseUrl);
        o.addProperty("model", model);
        o.addProperty("apiKeyEnv", apiKeyEnv);
        o.addProperty("apiKeyStatus", apiKeyStatus());
        if (apiKeyUpdate != null) {
            o.addProperty("apiKeyUpdate", apiKeyUpdate);
        }
        o.addProperty("inputLanguage", inputLanguage);
        o.addProperty("serverLlmOnly", serverLlmOnly);
        o.addProperty("idleChat", idleChat);
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
            if (o.has("apiKeyStatus")) {
                snap.setApiKeyStatus(o.get("apiKeyStatus").getAsString());
            }
            if (o.has("apiKeyUpdate")) {
                snap.setApiKeyUpdate(o.get("apiKeyUpdate").getAsString());
            }
            if (o.has("inputLanguage")) {
                snap.setInputLanguage(o.get("inputLanguage").getAsString());
            }
            if (o.has("serverLlmOnly")) {
                snap.setServerLlmOnly(o.get("serverLlmOnly").getAsBoolean());
            }
            if (o.has("idleChat")) {
                snap.setIdleChat(o.get("idleChat").getAsBoolean());
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

    static String resolveApiKeyStatus(CompanionAiSettings s) {
        if (s == null) {
            return API_KEY_STATUS_NONE;
        }
        if (s.apiKey() != null && !s.apiKey().isBlank()) {
            return API_KEY_STATUS_CONFIG;
        }
        if (!s.resolveApiKey().isBlank()) {
            return API_KEY_STATUS_ENV;
        }
        return API_KEY_STATUS_NONE;
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String trimOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
