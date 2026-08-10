package com.azscompanions.config;

import com.azscompanions.ai.CompanionAiSettings;
import com.azscompanions.ai.LlmProviderMode;
import com.azscompanions.ai.McpTransportMode;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated companion AI config → {@code config/azscompanions-ai.toml}.
 * Separate from {@link ServerConfig} ({@code azscompanions-server.toml}).
 * Default provider is disabled (offline-safe).
 */
public final class AiConfig {
    public static final String FILE_NAME = "azscompanions-ai.toml";
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<String> PROVIDER;
    public static final ModConfigSpec.ConfigValue<String> BASE_URL;
    public static final ModConfigSpec.ConfigValue<String> MODEL;
    public static final ModConfigSpec.ConfigValue<String> API_KEY;
    public static final ModConfigSpec.ConfigValue<String> API_KEY_ENV;
    public static final ModConfigSpec.ConfigValue<String> SYSTEM_PROMPT;
    public static final ModConfigSpec.ConfigValue<String> INPUT_LANGUAGE;
    public static final ModConfigSpec.IntValue TIMEOUT_SECONDS;
    public static final ModConfigSpec.IntValue MAX_TOKENS;
    public static final ModConfigSpec.BooleanValue ENABLE_CHAT_MESSAGES;

    public static final ModConfigSpec.ConfigValue<String> MCP_TRANSPORT;
    public static final ModConfigSpec.ConfigValue<String> MCP_URL;
    public static final ModConfigSpec.ConfigValue<String> MCP_COMMAND;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MCP_ARGS;
    public static final ModConfigSpec.ConfigValue<String> MCP_TOOL_NAME;
    public static final ModConfigSpec.ConfigValue<String> MCP_PROTOCOL_VERSION;
    public static final ModConfigSpec.ConfigValue<String> MCP_TOOL_ALLOWLIST;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment(
                "Az's Companions — companion AI (text dialogue only).",
                "Default provider=disabled. Prefer env AZS_LLM_API_KEY over apiKey.",
                "See docs/COMPANION_AI.md"
        );

        PROVIDER = builder.comment(
                        "disabled | local | openai_compatible | mcp. Default disabled (offline-safe).",
                        "local and openai_compatible share one OpenAI-compatible HTTP client;",
                        "local typically points at Ollama http://127.0.0.1:11434/v1 (API key optional).",
                        "mcp routes chat via an MCP server tool (HTTP or stdio).")
                .define("provider", "disabled");
        BASE_URL = builder.comment("OpenAI-compatible base URL (…/v1). Used by local and openai_compatible.")
                .define("baseUrl", CompanionAiSettings.DEFAULT_BASE_URL);
        MODEL = builder.define("model", CompanionAiSettings.DEFAULT_MODEL);
        API_KEY = builder.comment("Optional. Prefer env var (apiKeyEnv). Storing secrets in config is discouraged.")
                .define("apiKey", "");
        API_KEY_ENV = builder.define("apiKeyEnv", CompanionAiSettings.DEFAULT_API_KEY_ENV);
        SYSTEM_PROMPT = builder.comment("Placeholders: {name} {form} {language}")
                .define("systemPrompt", CompanionAiSettings.DEFAULT_SYSTEM_PROMPT);
        INPUT_LANGUAGE = builder.comment("Player input / preferred reply language code (en, de, ja, …).")
                .define("inputLanguage", "en");
        TIMEOUT_SECONDS = builder.defineInRange("timeoutSeconds", 30, 5, 120);
        MAX_TOKENS = builder.defineInRange("maxTokens", 256, 32, 2048);
        ENABLE_CHAT_MESSAGES = builder.comment("Show LLM/MCP replies as owner chat lines (all forms). Text dialogue only.")
                .define("enableChatMessages", true);

        builder.push("mcp");
        MCP_TRANSPORT = builder.comment("http | stdio").define("transport", "http");
        MCP_URL = builder.comment("MCP Streamable HTTP endpoint URL.").define("url", "http://127.0.0.1:3001/mcp");
        MCP_COMMAND = builder.comment("Executable for stdio transport.").define("command", "");
        MCP_ARGS = builder.comment("Args for stdio command.")
                .defineListAllowEmpty("args", List.of(), () -> "", o -> o instanceof String);
        MCP_TOOL_NAME = builder.comment("Tool invoked for companion chat (default companion_chat).")
                .define("toolName", CompanionAiSettings.DEFAULT_MCP_TOOL);
        MCP_PROTOCOL_VERSION = builder.define("protocolVersion", CompanionAiSettings.DEFAULT_MCP_PROTOCOL);
        MCP_TOOL_ALLOWLIST = builder.comment("Comma-separated allowed tool names. Empty = only toolName.")
                .define("toolAllowlist", "");
        builder.pop();

        SPEC = builder.build();
    }

    public static CompanionAiSettings toAiSettings() {
        List<String> args = new ArrayList<>();
        for (String a : MCP_ARGS.get()) {
            args.add(a);
        }
        return new CompanionAiSettings()
                .setProvider(LlmProviderMode.fromConfig(PROVIDER.get()))
                .setBaseUrl(BASE_URL.get())
                .setModel(MODEL.get())
                .setApiKey(API_KEY.get())
                .setApiKeyEnv(API_KEY_ENV.get())
                .setSystemPrompt(SYSTEM_PROMPT.get())
                .setInputLanguage(INPUT_LANGUAGE.get())
                .setTimeoutSeconds(TIMEOUT_SECONDS.get())
                .setMaxTokens(MAX_TOKENS.get())
                .setEnableChatMessages(ENABLE_CHAT_MESSAGES.get())
                .setMcpTransport(McpTransportMode.fromConfig(MCP_TRANSPORT.get()))
                .setMcpUrl(MCP_URL.get())
                .setMcpCommand(MCP_COMMAND.get())
                .setMcpArgs(args)
                .setMcpToolName(MCP_TOOL_NAME.get())
                .setMcpProtocolVersion(MCP_PROTOCOL_VERSION.get())
                .setMcpAllowlist(MCP_TOOL_ALLOWLIST.get());
    }

    private AiConfig() {
    }
}
