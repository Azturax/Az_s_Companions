package com.azscompanions.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Loader-agnostic companion AI settings. Defaults keep LLM/MCP off for offline play.
 * Replies are text dialogue only (owner chat / say lines).
 */
public final class CompanionAiSettings {
    public static final String DEFAULT_BASE_URL = "http://127.0.0.1:11434/v1";
    public static final String DEFAULT_MODEL = "llama3.2";
    public static final String DEFAULT_API_KEY_ENV = "AZS_LLM_API_KEY";
    public static final String DEFAULT_SYSTEM_PROMPT =
            "You are {name}, a wholesome adult Minecraft companion (form: {form}). "
                    + "Stay in character, keep replies short (1-3 sentences), never be sexual or cruel. "
                    + "The player speaks in {language}. Reply in that language unless they ask otherwise.";
    public static final String DEFAULT_MCP_TOOL = "companion_chat";
    public static final String DEFAULT_MCP_PROTOCOL = "2025-03-26";

    private LlmProviderMode provider = LlmProviderMode.DISABLED;
    private String baseUrl = DEFAULT_BASE_URL;
    private String model = DEFAULT_MODEL;
    private String apiKey = "";
    private String apiKeyEnv = DEFAULT_API_KEY_ENV;
    private String systemPrompt = DEFAULT_SYSTEM_PROMPT;
    private String inputLanguage = "en";
    private int timeoutSeconds = 30;
    private int maxTokens = 256;
    private boolean enableChatMessages = true;

    private McpTransportMode mcpTransport = McpTransportMode.HTTP;
    private String mcpUrl = "http://127.0.0.1:3001/mcp";
    private String mcpCommand = "";
    private List<String> mcpArgs = new ArrayList<>();
    private String mcpToolName = DEFAULT_MCP_TOOL;
    private String mcpProtocolVersion = DEFAULT_MCP_PROTOCOL;
    private String mcpAllowlist = "";

    public LlmProviderMode provider() {
        return provider;
    }

    public CompanionAiSettings setProvider(LlmProviderMode provider) {
        this.provider = provider == null ? LlmProviderMode.DISABLED : provider;
        return this;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public CompanionAiSettings setBaseUrl(String baseUrl) {
        this.baseUrl = blankTo(baseUrl, DEFAULT_BASE_URL);
        return this;
    }

    public String model() {
        return model;
    }

    public CompanionAiSettings setModel(String model) {
        this.model = blankTo(model, DEFAULT_MODEL);
        return this;
    }

    public String apiKey() {
        return apiKey;
    }

    public CompanionAiSettings setApiKey(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        return this;
    }

    public String apiKeyEnv() {
        return apiKeyEnv;
    }

    public CompanionAiSettings setApiKeyEnv(String apiKeyEnv) {
        this.apiKeyEnv = blankTo(apiKeyEnv, DEFAULT_API_KEY_ENV);
        return this;
    }

    /** Resolved API key: config value, else environment variable. */
    public String resolveApiKey() {
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }
        String env = System.getenv(apiKeyEnv);
        return env == null ? "" : env.trim();
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public CompanionAiSettings setSystemPrompt(String systemPrompt) {
        this.systemPrompt = blankTo(systemPrompt, DEFAULT_SYSTEM_PROMPT);
        return this;
    }

    public String inputLanguage() {
        return inputLanguage;
    }

    public CompanionAiSettings setInputLanguage(String inputLanguage) {
        this.inputLanguage = blankTo(inputLanguage, "en");
        return this;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }

    public CompanionAiSettings setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = Math.max(5, Math.min(120, timeoutSeconds));
        return this;
    }

    public int maxTokens() {
        return maxTokens;
    }

    public CompanionAiSettings setMaxTokens(int maxTokens) {
        this.maxTokens = Math.max(32, Math.min(2048, maxTokens));
        return this;
    }

    public boolean enableChatMessages() {
        return enableChatMessages;
    }

    public CompanionAiSettings setEnableChatMessages(boolean enableChatMessages) {
        this.enableChatMessages = enableChatMessages;
        return this;
    }

    public McpTransportMode mcpTransport() {
        return mcpTransport;
    }

    public CompanionAiSettings setMcpTransport(McpTransportMode mcpTransport) {
        this.mcpTransport = mcpTransport == null ? McpTransportMode.HTTP : mcpTransport;
        return this;
    }

    public String mcpUrl() {
        return mcpUrl;
    }

    public CompanionAiSettings setMcpUrl(String mcpUrl) {
        this.mcpUrl = blankTo(mcpUrl, "http://127.0.0.1:3001/mcp");
        return this;
    }

    public String mcpCommand() {
        return mcpCommand;
    }

    public CompanionAiSettings setMcpCommand(String mcpCommand) {
        this.mcpCommand = mcpCommand == null ? "" : mcpCommand.trim();
        return this;
    }

    public List<String> mcpArgs() {
        return List.copyOf(mcpArgs);
    }

    public CompanionAiSettings setMcpArgs(List<String> mcpArgs) {
        this.mcpArgs = mcpArgs == null ? new ArrayList<>() : new ArrayList<>(mcpArgs);
        return this;
    }

    public String mcpToolName() {
        return mcpToolName;
    }

    public CompanionAiSettings setMcpToolName(String mcpToolName) {
        this.mcpToolName = blankTo(mcpToolName, DEFAULT_MCP_TOOL);
        return this;
    }

    public String mcpProtocolVersion() {
        return mcpProtocolVersion;
    }

    public CompanionAiSettings setMcpProtocolVersion(String mcpProtocolVersion) {
        this.mcpProtocolVersion = blankTo(mcpProtocolVersion, DEFAULT_MCP_PROTOCOL);
        return this;
    }

    public String mcpAllowlist() {
        return mcpAllowlist;
    }

    public CompanionAiSettings setMcpAllowlist(String mcpAllowlist) {
        this.mcpAllowlist = mcpAllowlist == null ? "" : mcpAllowlist.trim();
        return this;
    }

    public boolean isToolAllowed(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return false;
        }
        if (mcpAllowlist == null || mcpAllowlist.isBlank()) {
            return toolName.equalsIgnoreCase(mcpToolName);
        }
        for (String part : mcpAllowlist.split(",")) {
            if (toolName.equalsIgnoreCase(part.trim())) {
                return true;
            }
        }
        return false;
    }

    public String formatSystemPrompt(String companionName, String form) {
        return systemPrompt
                .replace("{name}", Objects.toString(companionName, "Companion"))
                .replace("{form}", Objects.toString(form, "player"))
                .replace("{language}", Objects.toString(inputLanguage, "en"));
    }

    public CompanionAiSettings copy() {
        return new CompanionAiSettings()
                .setProvider(provider)
                .setBaseUrl(baseUrl)
                .setModel(model)
                .setApiKey(apiKey)
                .setApiKeyEnv(apiKeyEnv)
                .setSystemPrompt(systemPrompt)
                .setInputLanguage(inputLanguage)
                .setTimeoutSeconds(timeoutSeconds)
                .setMaxTokens(maxTokens)
                .setEnableChatMessages(enableChatMessages)
                .setMcpTransport(mcpTransport)
                .setMcpUrl(mcpUrl)
                .setMcpCommand(mcpCommand)
                .setMcpArgs(mcpArgs)
                .setMcpToolName(mcpToolName)
                .setMcpProtocolVersion(mcpProtocolVersion)
                .setMcpAllowlist(mcpAllowlist);
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
