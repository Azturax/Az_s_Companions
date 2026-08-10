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
        if (root.has("maxInputChars")) {
            s.setMaxInputChars(root.get("maxInputChars").getAsInt());
        }
        if (root.has("queueMaxDepth")) {
            s.setQueueMaxDepth(root.get("queueMaxDepth").getAsInt());
        }
        if (root.has("enableChatMessages")) {
            s.setEnableChatMessages(root.get("enableChatMessages").getAsBoolean());
        }
        if (root.has("serverLlmOnly")) {
            s.setServerLlmOnly(root.get("serverLlmOnly").getAsBoolean());
        }
        if (root.has("integratedMultiplayerSharedLlm")) {
            s.setIntegratedMultiplayerSharedLlm(root.get("integratedMultiplayerSharedLlm").getAsBoolean());
        }
        if (root.has("ownerNameFallback")) {
            s.setOwnerNameFallback(root.get("ownerNameFallback").getAsBoolean());
        }
        if (root.has("perCompanionMemory")) {
            s.setPerCompanionMemory(root.get("perCompanionMemory").getAsBoolean());
        }
        if (root.has("memoryMaxMessages")) {
            s.setMemoryMaxMessages(root.get("memoryMaxMessages").getAsInt());
        }
        if (root.has("censorChat")) {
            s.setCensorChat(root.get("censorChat").getAsBoolean());
        } else if (root.has("filterProfanity")) {
            s.setCensorChat(root.get("filterProfanity").getAsBoolean());
        }
        if (root.has("chatListenMode")) {
            s.setChatListenMode(ChatListenMode.fromConfig(root.get("chatListenMode").getAsString()));
        } else if (root.has("chatReaction")) {
            s.setChatListenMode(ChatListenMode.fromConfig(root.get("chatReaction").getAsString()));
        }
        if (root.has("nameListen")) {
            s.setNameListen(root.get("nameListen").getAsBoolean());
        }
        if (root.has("chatReactRange")) {
            s.setChatReactRange(root.get("chatReactRange").getAsDouble());
        }
        if (root.has("chatReactCooldownSeconds")) {
            s.setChatReactCooldownSeconds(root.get("chatReactCooldownSeconds").getAsInt());
        }
        if (root.has("censorExtraWords")) {
            List<String> extra = new ArrayList<>();
            if (root.get("censorExtraWords").isJsonArray()) {
                root.getAsJsonArray("censorExtraWords").forEach(e -> extra.add(e.getAsString()));
            } else {
                for (String part : root.get("censorExtraWords").getAsString().split(",")) {
                    if (!part.isBlank()) {
                        extra.add(part.trim());
                    }
                }
            }
            s.setCensorExtraWords(extra);
        }
        if (root.has("idleChat")) {
            s.setIdleChat(root.get("idleChat").getAsBoolean());
        }
        if (root.has("idleChatSecondsMin")) {
            s.setIdleChatSecondsMin(root.get("idleChatSecondsMin").getAsInt());
        }
        if (root.has("idleChatSecondsMax")) {
            s.setIdleChatSecondsMax(root.get("idleChatSecondsMax").getAsInt());
        }
        if (root.has("callPlayerWhenAway")) {
            s.setCallPlayerWhenAway(root.get("callPlayerWhenAway").getAsBoolean());
        }
        if (root.has("callPlayerAfterSeconds")) {
            s.setCallPlayerAfterSeconds(root.get("callPlayerAfterSeconds").getAsInt());
        }
        if (root.has("callPlayerDistance")) {
            s.setCallPlayerDistance(root.get("callPlayerDistance").getAsDouble());
        }
        if (root.has("callPlayerCooldownSeconds")) {
            s.setCallPlayerCooldownSeconds(root.get("callPlayerCooldownSeconds").getAsInt());
        }
        if (root.has("enableAiActions")) {
            s.setEnableAiActions(root.get("enableAiActions").getAsBoolean());
        }
        if (root.has("aiActionReach")) {
            s.setAiActionReach(root.get("aiActionReach").getAsInt());
        }
        if (root.has("aiActionCooldownTicks")) {
            s.setAiActionCooldownTicks(root.get("aiActionCooldownTicks").getAsInt());
        }
        if (root.has("childAutonomy")) {
            s.setChildAutonomy(ChildAutonomyMode.fromConfig(root.get("childAutonomy").getAsString()));
        }
        if (root.has("childLeashRadius")) {
            s.setChildLeashRadius(root.get("childLeashRadius").getAsDouble());
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
        applyFtbFromJson(s, root);
        return s;
    }

    private static void applyFtbFromJson(CompanionAiSettings s, JsonObject root) {
        JsonObject ftb = root.has("ftb") && root.get("ftb").isJsonObject()
                ? root.getAsJsonObject("ftb")
                : root;
        if (ftb.has("ftbTeamsCompat")) {
            s.setFtbTeamsCompat(ftb.get("ftbTeamsCompat").getAsBoolean());
        }
        if (ftb.has("ftbChunksAllowPresence")) {
            s.setFtbChunksAllowPresence(ftb.get("ftbChunksAllowPresence").getAsBoolean());
        }
        if (ftb.has("ftbChunksBlockInteraction")) {
            s.setFtbChunksBlockInteraction(ftb.get("ftbChunksBlockInteraction").getAsBoolean());
        } else if (ftb.has("ftbChunksProtect")) {
            // legacy alias
            s.setFtbChunksBlockInteraction(ftb.get("ftbChunksProtect").getAsBoolean());
        }
        if (ftb.has("ftbChunksAiClaim")) {
            s.setFtbChunksAiClaim(ftb.get("ftbChunksAiClaim").getAsBoolean());
        } else if (ftb.has("enableAiClaim")) {
            s.setFtbChunksAiClaim(ftb.get("enableAiClaim").getAsBoolean());
        }
        if (ftb.has("ftbRanksCompat")) {
            s.setFtbRanksCompat(ftb.get("ftbRanksCompat").getAsBoolean());
        }
        if (ftb.has("trustSameTeamAsOwner")) {
            s.setTrustSameTeamAsOwner(ftb.get("trustSameTeamAsOwner").getAsBoolean());
        }
        if (ftb.has("permAiAsk")) {
            s.setFtbPermAiAsk(ftb.get("permAiAsk").getAsString());
        }
        if (ftb.has("permAiActions")) {
            s.setFtbPermAiActions(ftb.get("permAiActions").getAsString());
        }
        if (ftb.has("permCci")) {
            s.setFtbPermCci(ftb.get("permCci").getAsString());
        }
        if (ftb.has("permTeamfight")) {
            s.setFtbPermTeamfight(ftb.get("permTeamfight").getAsString());
        }
        if (ftb.has("permSpawn")) {
            s.setFtbPermSpawn(ftb.get("permSpawn").getAsString());
        }
    }

    public static JsonObject toJson(CompanionAiSettings s) {
        JsonObject root = new JsonObject();
        root.addProperty("_comment",
                "Text dialogue AI. provider: disabled|local|openai_compatible|mcp (aliases: litellm, openrouter, …). "
                        + "chatListenMode: off|player|global. Multiplayer: shared server LLM endpoint "
                        + "(serverLlmOnly), separate minds per companion (perCompanionMemory). Prefer env API keys.");
        root.addProperty("provider", s.provider().name().toLowerCase());
        root.addProperty("baseUrl", s.baseUrl());
        root.addProperty("model", s.model());
        root.addProperty("apiKey", s.apiKey());
        root.addProperty("apiKeyEnv", s.apiKeyEnv());
        root.addProperty("systemPrompt", s.systemPrompt());
        root.addProperty("inputLanguage", s.inputLanguage());
        root.addProperty("timeoutSeconds", s.timeoutSeconds());
        root.addProperty("maxTokens", s.maxTokens());
        root.addProperty("maxInputChars", s.maxInputChars());
        root.addProperty("queueMaxDepth", s.queueMaxDepth());
        root.addProperty("enableChatMessages", s.enableChatMessages());
        root.addProperty("serverLlmOnly", s.serverLlmOnly());
        root.addProperty("integratedMultiplayerSharedLlm", s.integratedMultiplayerSharedLlm());
        root.addProperty("ownerNameFallback", s.ownerNameFallback());
        root.addProperty("perCompanionMemory", s.perCompanionMemory());
        root.addProperty("memoryMaxMessages", s.memoryMaxMessages());
        root.addProperty("censorChat", s.censorChat());
        root.addProperty("chatListenMode", s.chatListenMode().configName());
        root.addProperty("nameListen", s.nameListen());
        root.addProperty("chatReactRange", s.chatReactRange());
        root.addProperty("chatReactCooldownSeconds", s.chatReactCooldownSeconds());
        JsonArray censorExtra = new JsonArray();
        s.censorExtraWords().forEach(censorExtra::add);
        root.add("censorExtraWords", censorExtra);
        root.addProperty("idleChat", s.idleChat());
        root.addProperty("idleChatSecondsMin", s.idleChatSecondsMin());
        root.addProperty("idleChatSecondsMax", s.idleChatSecondsMax());
        root.addProperty("callPlayerWhenAway", s.callPlayerWhenAway());
        root.addProperty("callPlayerAfterSeconds", s.callPlayerAfterSeconds());
        root.addProperty("callPlayerDistance", s.callPlayerDistance());
        root.addProperty("callPlayerCooldownSeconds", s.callPlayerCooldownSeconds());
        root.addProperty("enableAiActions", s.enableAiActions());
        root.addProperty("aiActionReach", s.aiActionReach());
        root.addProperty("aiActionCooldownTicks", s.aiActionCooldownTicks());
        root.addProperty("childAutonomy", s.childAutonomy().configName());
        root.addProperty("childLeashRadius", s.childLeashRadius());
        JsonObject ftb = new JsonObject();
        ftb.addProperty("ftbTeamsCompat", s.ftbTeamsCompat());
        ftb.addProperty("ftbChunksAllowPresence", s.ftbChunksAllowPresence());
        ftb.addProperty("ftbChunksBlockInteraction", s.ftbChunksBlockInteraction());
        ftb.addProperty("ftbChunksAiClaim", s.ftbChunksAiClaim());
        ftb.addProperty("ftbRanksCompat", s.ftbRanksCompat());
        ftb.addProperty("trustSameTeamAsOwner", s.trustSameTeamAsOwner());
        ftb.addProperty("permAiAsk", s.ftbPermAiAsk());
        ftb.addProperty("permAiActions", s.ftbPermAiActions());
        ftb.addProperty("permCci", s.ftbPermCci());
        ftb.addProperty("permTeamfight", s.ftbPermTeamfight());
        ftb.addProperty("permSpawn", s.ftbPermSpawn());
        root.add("ftb", ftb);
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
