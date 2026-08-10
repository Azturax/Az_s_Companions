package com.azscompanions.ai;

/**
 * How companion chat replies are produced.
 * <ul>
 *   <li>{@link #DISABLED} — scripted dialogue only (default, offline-safe)</li>
 *   <li>{@link #LOCAL} — OpenAI-compatible HTTP to a local server (Ollama, LM Studio, llama.cpp)</li>
 *   <li>{@link #OPENAI_COMPATIBLE} — same client, remote base URL + API key</li>
 *   <li>{@link #MCP} — route chat through an MCP server (HTTP or stdio)</li>
 * </ul>
 */
public enum LlmProviderMode {
    DISABLED,
    LOCAL,
    OPENAI_COMPATIBLE,
    MCP;

    public static LlmProviderMode fromConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return DISABLED;
        }
        String key = raw.trim().toLowerCase().replace('-', '_');
        return switch (key) {
            case "local", "ollama", "lmstudio", "llama_cpp" -> LOCAL;
            case "openai_compatible", "openai", "openrouter", "groq", "together", "azure_openai" -> OPENAI_COMPATIBLE;
            case "mcp" -> MCP;
            default -> DISABLED;
        };
    }

    public boolean usesOpenAiCompatibleHttp() {
        return this == LOCAL || this == OPENAI_COMPATIBLE;
    }

    public boolean isEnabled() {
        return this != DISABLED;
    }
}
