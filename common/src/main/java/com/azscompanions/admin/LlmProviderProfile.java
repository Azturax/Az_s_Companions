package com.azscompanions.admin;

import com.azscompanions.ai.LlmProviderMode;

import java.util.Locale;

/**
 * In-game admin presets for companion AI provider / base URL.
 * {@link #CUSTOM} leaves fields free; other entries fill provider + baseUrl (+ model placeholder).
 */
public enum LlmProviderProfile {
    DISABLED(
            "Disabled",
            LlmProviderMode.DISABLED,
            CompanionAiDefaults.OLLAMA_BASE,
            CompanionAiDefaults.MODEL_LOCAL,
            null),
    LOCAL_LM_STUDIO(
            "Local (LM Studio)",
            LlmProviderMode.LOCAL,
            "http://127.0.0.1:1234/v1",
            "local-model",
            null),
    LOCAL_OLLAMA(
            "Local (Ollama)",
            LlmProviderMode.LOCAL,
            CompanionAiDefaults.OLLAMA_BASE,
            CompanionAiDefaults.MODEL_LOCAL,
            null),
    OPENROUTER(
            "OpenRouter",
            LlmProviderMode.OPENAI_COMPATIBLE,
            "https://openrouter.ai/api/v1",
            "openai/gpt-4o-mini",
            null),
    OPENAI(
            "OpenAI",
            LlmProviderMode.OPENAI_COMPATIBLE,
            "https://api.openai.com/v1",
            "gpt-4o-mini",
            null),
    GROQ(
            "Groq",
            LlmProviderMode.OPENAI_COMPATIBLE,
            "https://api.groq.com/openai/v1",
            "llama-3.3-70b-versatile",
            null),
    MCP_HTTP(
            "MCP (HTTP)",
            LlmProviderMode.MCP,
            CompanionAiDefaults.OLLAMA_BASE,
            CompanionAiDefaults.MODEL_LOCAL,
            CompanionAiDefaults.MCP_URL),
    CUSTOM(
            "Custom...",
            null,
            null,
            null,
            null);

    private final String label;
    private final LlmProviderMode provider;
    private final String baseUrl;
    private final String modelPlaceholder;
    private final String mcpUrl;

    LlmProviderProfile(String label, LlmProviderMode provider, String baseUrl,
                       String modelPlaceholder, String mcpUrl) {
        this.label = label;
        this.provider = provider;
        this.baseUrl = baseUrl;
        this.modelPlaceholder = modelPlaceholder;
        this.mcpUrl = mcpUrl;
    }

    public String label() {
        return label;
    }

    public boolean isCustom() {
        return this == CUSTOM;
    }

    /** When false, admin UI should treat provider/baseUrl as preset-locked. */
    public boolean allowsFreeProviderFields() {
        return this == CUSTOM;
    }

    public LlmProviderMode providerOrNull() {
        return provider;
    }

    public String baseUrlOrNull() {
        return baseUrl;
    }

    public String modelPlaceholderOrNull() {
        return modelPlaceholder;
    }

    /** Apply preset into the snapshot (Custom is a no-op besides marking profile). */
    public void applyTo(AdminAiConfigSnapshot snap) {
        if (snap == null) {
            return;
        }
        snap.setProfileId(name().toLowerCase(Locale.ROOT));
        if (this == CUSTOM) {
            return;
        }
        snap.setProvider(provider.name().toLowerCase(Locale.ROOT));
        if (baseUrl != null && !baseUrl.isBlank()) {
            snap.setBaseUrl(baseUrl);
        }
        if (modelPlaceholder != null && !modelPlaceholder.isBlank()) {
            snap.setModel(modelPlaceholder);
        }
        if (this == MCP_HTTP && mcpUrl != null) {
            snap.setMcpUrl(mcpUrl);
        }
        if (this == DISABLED) {
            // keep model/base as placeholders but provider disabled
            snap.setProvider("disabled");
        }
    }

    public static LlmProviderProfile fromId(String raw) {
        if (raw == null || raw.isBlank()) {
            return CUSTOM;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (LlmProviderProfile p : values()) {
            if (p.name().equalsIgnoreCase(key) || p.name().toLowerCase(Locale.ROOT).equals(key)) {
                return p;
            }
        }
        return CUSTOM;
    }

    /**
     * Best-effort match from current provider + baseUrl (+ mcp). Falls back to {@link #CUSTOM}.
     */
    public static LlmProviderProfile detect(AdminAiConfigSnapshot snap) {
        if (snap == null) {
            return CUSTOM;
        }
        LlmProviderMode mode = LlmProviderMode.fromConfig(snap.provider());
        String base = normalizeUrl(snap.baseUrl());
        String mcp = normalizeUrl(snap.mcpUrl());
        if (mode == LlmProviderMode.DISABLED) {
            return DISABLED;
        }
        if (mode == LlmProviderMode.MCP) {
            if (mcp.isEmpty() || mcp.equals(normalizeUrl(CompanionAiDefaults.MCP_URL))) {
                return MCP_HTTP;
            }
            return CUSTOM;
        }
        for (LlmProviderProfile p : values()) {
            if (p == CUSTOM || p == DISABLED || p == MCP_HTTP) {
                continue;
            }
            if (p.provider == mode && normalizeUrl(p.baseUrl).equals(base)) {
                return p;
            }
        }
        return CUSTOM;
    }

    public LlmProviderProfile next() {
        LlmProviderProfile[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    private static String normalizeUrl(String url) {
        if (url == null) {
            return "";
        }
        String u = url.trim().toLowerCase(Locale.ROOT);
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }

    /** Tiny nest so we don't pull CompanionAiSettings constants into every call site. */
    private static final class CompanionAiDefaults {
        static final String OLLAMA_BASE = "http://127.0.0.1:11434/v1";
        static final String MODEL_LOCAL = "llama3.2";
        static final String MCP_URL = "http://127.0.0.1:3001/mcp";

        private CompanionAiDefaults() {
        }
    }
}
