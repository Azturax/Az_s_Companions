package com.azscompanions.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared JSON file loader used by Fabric ({@code config/azscompanions-ai.json}).
 * NeoForge uses a separate ModConfigSpec file ({@code config/azscompanions-ai.toml}).
 */
public final class CompanionAiConfigIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private CompanionAiConfigIO() {
    }

    public static CompanionAiSettings loadOrCreate(Path path) throws IOException {
        if (!Files.exists(path)) {
            CompanionAiSettings defaults = new CompanionAiSettings();
            save(path, defaults);
            return defaults;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            return fromJson(root);
        }
    }

    public static void save(Path path, CompanionAiSettings settings) throws IOException {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(toJson(settings), writer);
        }
    }

    public static CompanionAiSettings fromJson(JsonObject root) {
        CompanionAiSettings s = new CompanionAiSettings();
        if (root.has("provider")) {
            s.setProvider(LlmProviderMode.fromConfig(root.get("provider").getAsString()));
        }
        if (root.has("baseUrl")) {
            s.setBaseUrl(root.get("baseUrl").getAsString());
        }
        if (root.has("model")) {
            s.setModel(root.get("model").getAsString());
        }
        if (root.has("apiKey")) {
            s.setApiKey(root.get("apiKey").getAsString());
        }
        if (root.has("apiKeyEnv")) {
            s.setApiKeyEnv(root.get("apiKeyEnv").getAsString());
        }
        if (root.has("systemPrompt")) {
            s.setSystemPrompt(root.get("systemPrompt").getAsString());
        }
        if (root.has("inputLanguage")) {
            s.setInputLanguage(root.get("inputLanguage").getAsString());
        }
        if (root.has("timeoutSeconds")) {
            s.setTimeoutSeconds(root.get("timeoutSeconds").getAsInt());
        }
        if (root.has("maxTokens")) {
            s.setMaxTokens(root.get("maxTokens").getAsInt());
        }
        if (root.has("enableChatMessages")) {
            s.setEnableChatMessages(root.get("enableChatMessages").getAsBoolean());
        }
        if (root.has("mcp") && root.get("mcp").isJsonObject()) {
            JsonObject mcp = root.getAsJsonObject("mcp");
            if (mcp.has("transport")) {
                s.setMcpTransport(McpTransportMode.fromConfig(mcp.get("transport").getAsString()));
            }
            if (mcp.has("url")) {
                s.setMcpUrl(mcp.get("url").getAsString());
            }
            if (mcp.has("command")) {
                s.setMcpCommand(mcp.get("command").getAsString());
            }
            if (mcp.has("args") && mcp.get("args").isJsonArray()) {
                List<String> args = new ArrayList<>();
                JsonArray arr = mcp.getAsJsonArray("args");
                arr.forEach(e -> args.add(e.getAsString()));
                s.setMcpArgs(args);
            }
            if (mcp.has("toolName")) {
                s.setMcpToolName(mcp.get("toolName").getAsString());
            }
            if (mcp.has("protocolVersion")) {
                s.setMcpProtocolVersion(mcp.get("protocolVersion").getAsString());
            }
            if (mcp.has("toolAllowlist")) {
                s.setMcpAllowlist(mcp.get("toolAllowlist").getAsString());
            }
        }
        return s;
    }

    public static JsonObject toJson(CompanionAiSettings s) {
        JsonObject root = new JsonObject();
        root.addProperty("_comment",
                "Text dialogue AI. provider: disabled | local | openai_compatible | mcp. Prefer env API keys.");
        root.addProperty("provider", s.provider().name().toLowerCase());
        root.addProperty("baseUrl", s.baseUrl());
        root.addProperty("model", s.model());
        root.addProperty("apiKey", s.apiKey());
        root.addProperty("apiKeyEnv", s.apiKeyEnv());
        root.addProperty("systemPrompt", s.systemPrompt());
        root.addProperty("inputLanguage", s.inputLanguage());
        root.addProperty("timeoutSeconds", s.timeoutSeconds());
        root.addProperty("maxTokens", s.maxTokens());
        root.addProperty("enableChatMessages", s.enableChatMessages());
        JsonObject mcp = new JsonObject();
        mcp.addProperty("transport", s.mcpTransport().name().toLowerCase());
        mcp.addProperty("url", s.mcpUrl());
        mcp.addProperty("command", s.mcpCommand());
        JsonArray args = new JsonArray();
        s.mcpArgs().forEach(args::add);
        mcp.add("args", args);
        mcp.addProperty("toolName", s.mcpToolName());
        mcp.addProperty("protocolVersion", s.mcpProtocolVersion());
        mcp.addProperty("toolAllowlist", s.mcpAllowlist());
        root.add("mcp", mcp);
        return root;
    }
}
