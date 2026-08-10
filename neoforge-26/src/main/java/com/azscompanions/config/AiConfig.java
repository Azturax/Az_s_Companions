package com.azscompanions.config;

import com.azscompanions.ai.ChatListenMode;
import com.azscompanions.ai.ChildAutonomyMode;
import com.azscompanions.ai.CompanionAiChatSupport;
import com.azscompanions.ai.CompanionAiInput;
import com.azscompanions.ai.CompanionAiSettings;
import com.azscompanions.ai.LlmProviderMode;
import com.azscompanions.ai.McpTransportMode;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated companion AI config → {@code config/azscompanions-ai.toml}.
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
    public static final ModConfigSpec.IntValue MAX_INPUT_CHARS;
    public static final ModConfigSpec.IntValue QUEUE_MAX_DEPTH;
    public static final ModConfigSpec.BooleanValue ENABLE_CHAT_MESSAGES;
    public static final ModConfigSpec.BooleanValue SERVER_LLM_ONLY;
    public static final ModConfigSpec.BooleanValue INTEGRATED_MULTIPLAYER_SHARED_LLM;
    public static final ModConfigSpec.BooleanValue OWNER_NAME_FALLBACK;
    public static final ModConfigSpec.BooleanValue PER_COMPANION_MEMORY;
    public static final ModConfigSpec.IntValue MEMORY_MAX_MESSAGES;
    public static final ModConfigSpec.BooleanValue CENSOR_CHAT;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CENSOR_EXTRA_WORDS;
    public static final ModConfigSpec.ConfigValue<String> CHAT_LISTEN_MODE;
    public static final ModConfigSpec.BooleanValue NAME_LISTEN;
    public static final ModConfigSpec.DoubleValue CHAT_REACT_RANGE;
    public static final ModConfigSpec.IntValue CHAT_REACT_COOLDOWN_SECONDS;
    public static final ModConfigSpec.BooleanValue IDLE_CHAT;
    public static final ModConfigSpec.IntValue IDLE_CHAT_SECONDS_MIN;
    public static final ModConfigSpec.IntValue IDLE_CHAT_SECONDS_MAX;
    public static final ModConfigSpec.BooleanValue CALL_PLAYER_WHEN_AWAY;
    public static final ModConfigSpec.IntValue CALL_PLAYER_AFTER_SECONDS;
    public static final ModConfigSpec.DoubleValue CALL_PLAYER_DISTANCE;
    public static final ModConfigSpec.IntValue CALL_PLAYER_COOLDOWN_SECONDS;
    public static final ModConfigSpec.BooleanValue ENABLE_AI_ACTIONS;
    public static final ModConfigSpec.IntValue AI_ACTION_REACH;
    public static final ModConfigSpec.IntValue AI_ACTION_COOLDOWN_TICKS;
    public static final ModConfigSpec.ConfigValue<String> CHILD_AUTONOMY;
    public static final ModConfigSpec.DoubleValue CHILD_LEASH_RADIUS;

    public static final ModConfigSpec.BooleanValue FTB_TEAMS_COMPAT;
    public static final ModConfigSpec.BooleanValue FTB_CHUNKS_ALLOW_PRESENCE;
    public static final ModConfigSpec.BooleanValue FTB_CHUNKS_BLOCK_INTERACTION;
    public static final ModConfigSpec.BooleanValue FTB_CHUNKS_AI_CLAIM;
    public static final ModConfigSpec.BooleanValue FTB_RANKS_COMPAT;
    public static final ModConfigSpec.BooleanValue TRUST_SAME_TEAM_AS_OWNER;
    public static final ModConfigSpec.ConfigValue<String> FTB_PERM_AI_ASK;
    public static final ModConfigSpec.ConfigValue<String> FTB_PERM_AI_ACTIONS;
    public static final ModConfigSpec.ConfigValue<String> FTB_PERM_CCI;
    public static final ModConfigSpec.ConfigValue<String> FTB_PERM_TEAMFIGHT;
    public static final ModConfigSpec.ConfigValue<String> FTB_PERM_SPAWN;

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
                        "disabled | local | openai_compatible | mcp. Default disabled (offline-safe).")
                .define("provider", "disabled");
        BASE_URL = builder.comment("OpenAI-compatible base URL (…/v1).")
                .define("baseUrl", CompanionAiSettings.DEFAULT_BASE_URL);
        MODEL = builder.define("model", CompanionAiSettings.DEFAULT_MODEL);
        API_KEY = builder.comment("Optional. Prefer env var (apiKeyEnv).")
                .define("apiKey", "");
        API_KEY_ENV = builder.define("apiKeyEnv", CompanionAiSettings.DEFAULT_API_KEY_ENV);
        SYSTEM_PROMPT = builder.comment("Placeholders: {name} {form} {language} {attitude}")
                .define("systemPrompt", CompanionAiSettings.DEFAULT_SYSTEM_PROMPT);
        INPUT_LANGUAGE = builder.define("inputLanguage", "en");
        TIMEOUT_SECONDS = builder.defineInRange("timeoutSeconds", 30, 5, 120);
        MAX_TOKENS = builder.defineInRange("maxTokens", 256, 32, 2048);
        MAX_INPUT_CHARS = builder.comment(
                        "Max characters of one player chat/ask message kept for the LLM.",
                        "Full multi-sentence messages are preserved (no first-sentence trim).")
                .defineInRange("maxInputChars", CompanionAiInput.DEFAULT_MAX_INPUT_CHARS, 64, 8000);
        QUEUE_MAX_DEPTH = builder.comment(
                        "When AI is busy, queue up to this many extra requests instead of dropping them.",
                        "0 = reject while busy. Name-mention / ask can stack briefly.")
                .defineInRange("queueMaxDepth", CompanionAiInput.DEFAULT_QUEUE_MAX_DEPTH, 0, 16);
        ENABLE_CHAT_MESSAGES = builder.comment("Show LLM replies as owner chat lines.")
                .define("enableChatMessages", true);
        SERVER_LLM_ONLY = builder.comment(
                        "When true (default), this server's AI config is used for all companions.",
                        "Joining clients do not need local LM Studio or API keys — ask/listen/idle/CCI run server-side.",
                        "Dedicated and LAN hosts: configure provider/baseUrl/model once here (or env AZS_LLM_API_KEY).",
                        "Singleplayer: this same file applies to the integrated server.",
                        "Shared endpoint only — each companion still has its own mind (see perCompanionMemory).")
                .define("serverLlmOnly", true);
        INTEGRATED_MULTIPLAYER_SHARED_LLM = builder.comment(
                        "When true (default), Essential / e4mc / World Host / Open-to-LAN integrated multiplayer",
                        "forces the host LLM to stay authoritative even if serverLlmOnly=false.",
                        "No effect on dedicated servers (always shared there). See docs/COMPAT.md.")
                .define("integratedMultiplayerSharedLlm", true);
        OWNER_NAME_FALLBACK = builder.comment(
                        "When true (default), on integrated hosted multiplayer only, matching player names",
                        "count as companion owner if UUIDs diverge (offline↔online remap).",
                        "Always off on dedicated servers.")
                .define("ownerNameFallback", true);
        PER_COMPANION_MEMORY = builder.comment(
                        "When true (default), each companion keeps a separate rolling chat history keyed by UUID.",
                        "Companion A never sees companion B's transcript. Children have their own buffers.",
                        "Shared server LLM endpoint; separate minds.")
                .define("perCompanionMemory", true);
        MEMORY_MAX_MESSAGES = builder.comment(
                        "Max prior user+assistant messages kept per companion when perCompanionMemory is on.",
                        "Clamped 2–64.")
                .defineInRange("memoryMaxMessages", 16, 2, 64);
        CENSOR_CHAT = builder.comment(
                        "Star-out common swears in AI prompts and companion speak lines. Default true.",
                        "Fabric JSON alias: filterProfanity.")
                .define("censorChat", true);
        CENSOR_EXTRA_WORDS = builder.comment("Extra whole words to censor (case-insensitive).")
                .defineListAllowEmpty("censorExtraWords", List.of(), () -> "", o -> o instanceof String);
        CHAT_LISTEN_MODE = builder.comment(
                        "Non-mention auto-reply: off | player | global.",
                        "Name mentions are controlled by nameListen (default true).")
                .define("chatListenMode", "off");
        NAME_LISTEN = builder.comment(
                        "PRIMARY chat path (default true): say the companion's display name in normal chat",
                        "(Kon, how are you? / Bit come here please) — no /ask slash required.",
                        "Works even if chatListenMode is off. Owner vs stranger mode follows ownership.")
                .define("nameListen", true);
        CHAT_REACT_RANGE = builder.comment("Max blocks for chat auto-react / name mentions.")
                .defineInRange("chatReactRange", CompanionAiChatSupport.DEFAULT_CHAT_REACT_RANGE, 8.0d, 128.0d);
        CHAT_REACT_COOLDOWN_SECONDS = builder.comment("Per-companion and per-owner cooldown between auto-replies.")
                .defineInRange("chatReactCooldownSeconds",
                        CompanionAiChatSupport.DEFAULT_CHAT_REACT_COOLDOWN_SECONDS, 5, 600);
        IDLE_CHAT = builder.define("idleChat", false);
        IDLE_CHAT_SECONDS_MIN = builder.defineInRange("idleChatSecondsMin",
                CompanionAiChatSupport.DEFAULT_IDLE_CHAT_SECONDS_MIN, 30, 3600);
        IDLE_CHAT_SECONDS_MAX = builder.defineInRange("idleChatSecondsMax",
                CompanionAiChatSupport.DEFAULT_IDLE_CHAT_SECONDS_MAX, 30, 3600);
        CALL_PLAYER_WHEN_AWAY = builder.define("callPlayerWhenAway", false);
        CALL_PLAYER_AFTER_SECONDS = builder.defineInRange("callPlayerAfterSeconds",
                CompanionAiChatSupport.DEFAULT_CALL_PLAYER_AFTER_SECONDS, 30, 3600);
        CALL_PLAYER_DISTANCE = builder.defineInRange("callPlayerDistance",
                CompanionAiChatSupport.DEFAULT_CALL_PLAYER_DISTANCE, 8.0d, 128.0d);
        CALL_PLAYER_COOLDOWN_SECONDS = builder.defineInRange("callPlayerCooldownSeconds",
                CompanionAiChatSupport.DEFAULT_CALL_PLAYER_COOLDOWN_SECONDS, 5, 600);
        ENABLE_AI_ACTIONS = builder.comment(
                        "Allow LLM actions. Owners get full tools; strangers get limited social actions only.")
                .define("enableAiActions", false);
        AI_ACTION_REACH = builder.defineInRange("aiActionReach", 5, 2, 16);
        AI_ACTION_COOLDOWN_TICKS = builder.defineInRange("aiActionCooldownTicks", 10, 0, 100);
        CHILD_AUTONOMY = builder.define("childAutonomy", "balanced");
        CHILD_LEASH_RADIUS = builder.defineInRange("childLeashRadius", 0.0d, 0.0d, 48.0d);

        builder.push("ftb");
        FTB_TEAMS_COMPAT = builder.comment(
                        "When FTB Teams is loaded, same-team players count as owner-adjacent trusted",
                        "(helpful interact / combat trust). Soft-dep — ignored if FTB Teams absent.")
                .define("ftbTeamsCompat", true);
        FTB_CHUNKS_ALLOW_PRESENCE = builder.comment(
                        "Companions may walk / pathfind into FTB claimed chunks (presence always allowed).",
                        "Does not grant mine/build/container rights.")
                .define("ftbChunksAllowPresence", true);
        FTB_CHUNKS_BLOCK_INTERACTION = builder.comment(
                        "When FTB Chunks is loaded, block AI/task mine/place/build/container/use",
                        "in claims the owner cannot edit/interact. Walking into claims stays allowed.",
                        "Also requires common respectClaimMods=true. Soft-dep.",
                        "Legacy alias: ftbChunksProtect.")
                .define("ftbChunksBlockInteraction", true);
        FTB_CHUNKS_AI_CLAIM = builder.comment(
                        "When true + FTB Chunks loaded + enableAiActions, owner AI may claim_chunk / unclaim_chunk",
                        "using the owner's claim quota (never steals others' claims). Alias: enableAiClaim.")
                .define("ftbChunksAiClaim", false);
        FTB_RANKS_COMPAT = builder.comment(
                        "When FTB Ranks is loaded, gate ask / AI actions / CCI / teamfight / spawn",
                        "via permission nodes below. Missing nodes default to allow.")
                .define("ftbRanksCompat", false);
        TRUST_SAME_TEAM_AS_OWNER = builder.comment(
                        "When true + ftbTeamsCompat, same FTB team as owner gets full AI action trust",
                        "(OWNER tools). When false, teammates still get stranger-safe social actions only.")
                .define("trustSameTeamAsOwner", false);
        FTB_PERM_AI_ASK = builder.define("permAiAsk", "azscompanions.ai.ask");
        FTB_PERM_AI_ACTIONS = builder.define("permAiActions", "azscompanions.ai.actions");
        FTB_PERM_CCI = builder.define("permCci", "azscompanions.cci");
        FTB_PERM_TEAMFIGHT = builder.define("permTeamfight", "azscompanions.teamfight");
        FTB_PERM_SPAWN = builder.define("permSpawn", "azscompanions.spawn");
        builder.pop();

        builder.push("mcp");
        MCP_TRANSPORT = builder.define("transport", "http");
        MCP_URL = builder.define("url", "http://127.0.0.1:3001/mcp");
        MCP_COMMAND = builder.define("command", "");
        MCP_ARGS = builder.defineListAllowEmpty("args", List.of(), () -> "", o -> o instanceof String);
        MCP_TOOL_NAME = builder.define("toolName", CompanionAiSettings.DEFAULT_MCP_TOOL);
        MCP_PROTOCOL_VERSION = builder.define("protocolVersion", CompanionAiSettings.DEFAULT_MCP_PROTOCOL);
        MCP_TOOL_ALLOWLIST = builder.define("toolAllowlist", "");
        builder.pop();

        SPEC = builder.build();
    }

    public static CompanionAiSettings toAiSettings() {
        List<String> args = new ArrayList<>();
        for (String a : MCP_ARGS.get()) {
            args.add(a);
        }
        List<String> censorExtra = new ArrayList<>();
        for (String w : CENSOR_EXTRA_WORDS.get()) {
            censorExtra.add(w);
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
                .setMaxInputChars(MAX_INPUT_CHARS.get())
                .setQueueMaxDepth(QUEUE_MAX_DEPTH.get())
                .setEnableChatMessages(ENABLE_CHAT_MESSAGES.get())
                .setServerLlmOnly(SERVER_LLM_ONLY.get())
                .setIntegratedMultiplayerSharedLlm(INTEGRATED_MULTIPLAYER_SHARED_LLM.get())
                .setOwnerNameFallback(OWNER_NAME_FALLBACK.get())
                .setPerCompanionMemory(PER_COMPANION_MEMORY.get())
                .setMemoryMaxMessages(MEMORY_MAX_MESSAGES.get())
                .setCensorChat(CENSOR_CHAT.get())
                .setCensorExtraWords(censorExtra)
                .setChatListenMode(ChatListenMode.fromConfig(CHAT_LISTEN_MODE.get()))
                .setNameListen(NAME_LISTEN.get())
                .setChatReactRange(CHAT_REACT_RANGE.get())
                .setChatReactCooldownSeconds(CHAT_REACT_COOLDOWN_SECONDS.get())
                .setIdleChat(IDLE_CHAT.get())
                .setIdleChatSecondsMin(IDLE_CHAT_SECONDS_MIN.get())
                .setIdleChatSecondsMax(IDLE_CHAT_SECONDS_MAX.get())
                .setCallPlayerWhenAway(CALL_PLAYER_WHEN_AWAY.get())
                .setCallPlayerAfterSeconds(CALL_PLAYER_AFTER_SECONDS.get())
                .setCallPlayerDistance(CALL_PLAYER_DISTANCE.get())
                .setCallPlayerCooldownSeconds(CALL_PLAYER_COOLDOWN_SECONDS.get())
                .setEnableAiActions(ENABLE_AI_ACTIONS.get())
                .setAiActionReach(AI_ACTION_REACH.get())
                .setAiActionCooldownTicks(AI_ACTION_COOLDOWN_TICKS.get())
                .setChildAutonomy(ChildAutonomyMode.fromConfig(CHILD_AUTONOMY.get()))
                .setChildLeashRadius(CHILD_LEASH_RADIUS.get())
                .setFtbTeamsCompat(FTB_TEAMS_COMPAT.get())
                .setFtbChunksAllowPresence(FTB_CHUNKS_ALLOW_PRESENCE.get())
                .setFtbChunksBlockInteraction(FTB_CHUNKS_BLOCK_INTERACTION.get())
                .setFtbChunksAiClaim(FTB_CHUNKS_AI_CLAIM.get())
                .setFtbRanksCompat(FTB_RANKS_COMPAT.get())
                .setTrustSameTeamAsOwner(TRUST_SAME_TEAM_AS_OWNER.get())
                .setFtbPermAiAsk(FTB_PERM_AI_ASK.get())
                .setFtbPermAiActions(FTB_PERM_AI_ACTIONS.get())
                .setFtbPermCci(FTB_PERM_CCI.get())
                .setFtbPermTeamfight(FTB_PERM_TEAMFIGHT.get())
                .setFtbPermSpawn(FTB_PERM_SPAWN.get())
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

    /**
     * Persist AI settings to {@link #FILE_NAME} on disk without applying them to the live runtime.
     * Restart required for LLM client changes.
     */
    public static void saveSettingsToDiskWithoutReload(CompanionAiSettings s) throws java.io.IOException {
        java.nio.file.Path path = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        java.nio.file.Files.createDirectories(path.getParent());
        StringBuilder toml = new StringBuilder();
        toml.append("# Az's Companions — companion AI (written by /az admin). Restart to apply.\n");
        toml.append("provider = \"").append(esc(s.provider().name().toLowerCase())).append("\"\n");
        toml.append("baseUrl = \"").append(esc(s.baseUrl())).append("\"\n");
        toml.append("model = \"").append(esc(s.model())).append("\"\n");
        toml.append("apiKey = \"").append(esc(s.apiKey())).append("\"\n");
        toml.append("apiKeyEnv = \"").append(esc(s.apiKeyEnv())).append("\"\n");
        toml.append("systemPrompt = \"").append(esc(s.systemPrompt())).append("\"\n");
        toml.append("inputLanguage = \"").append(esc(s.inputLanguage())).append("\"\n");
        toml.append("timeoutSeconds = ").append(s.timeoutSeconds()).append("\n");
        toml.append("maxTokens = ").append(s.maxTokens()).append("\n");
        toml.append("maxInputChars = ").append(s.maxInputChars()).append("\n");
        toml.append("queueMaxDepth = ").append(s.queueMaxDepth()).append("\n");
        toml.append("enableChatMessages = ").append(s.enableChatMessages()).append("\n");
        toml.append("serverLlmOnly = ").append(s.serverLlmOnly()).append("\n");
        toml.append("integratedMultiplayerSharedLlm = ").append(s.integratedMultiplayerSharedLlm()).append("\n");
        toml.append("ownerNameFallback = ").append(s.ownerNameFallback()).append("\n");
        toml.append("perCompanionMemory = ").append(s.perCompanionMemory()).append("\n");
        toml.append("memoryMaxMessages = ").append(s.memoryMaxMessages()).append("\n");
        toml.append("censorChat = ").append(s.censorChat()).append("\n");
        toml.append("censorExtraWords = ").append(stringListToml(s.censorExtraWords())).append("\n");
        toml.append("chatListenMode = \"").append(esc(s.chatListenMode().configName())).append("\"\n");
        toml.append("nameListen = ").append(s.nameListen()).append("\n");
        toml.append("chatReactRange = ").append(s.chatReactRange()).append("\n");
        toml.append("chatReactCooldownSeconds = ").append(s.chatReactCooldownSeconds()).append("\n");
        toml.append("idleChat = ").append(s.idleChat()).append("\n");
        toml.append("idleChatSecondsMin = ").append(s.idleChatSecondsMin()).append("\n");
        toml.append("idleChatSecondsMax = ").append(s.idleChatSecondsMax()).append("\n");
        toml.append("callPlayerWhenAway = ").append(s.callPlayerWhenAway()).append("\n");
        toml.append("callPlayerAfterSeconds = ").append(s.callPlayerAfterSeconds()).append("\n");
        toml.append("callPlayerDistance = ").append(s.callPlayerDistance()).append("\n");
        toml.append("callPlayerCooldownSeconds = ").append(s.callPlayerCooldownSeconds()).append("\n");
        toml.append("enableAiActions = ").append(s.enableAiActions()).append("\n");
        toml.append("aiActionReach = ").append(s.aiActionReach()).append("\n");
        toml.append("aiActionCooldownTicks = ").append(s.aiActionCooldownTicks()).append("\n");
        toml.append("childAutonomy = \"").append(esc(s.childAutonomy().configName())).append("\"\n");
        toml.append("childLeashRadius = ").append(s.childLeashRadius()).append("\n");
        toml.append("\n[ftb]\n");
        toml.append("ftbTeamsCompat = ").append(s.ftbTeamsCompat()).append("\n");
        toml.append("ftbChunksAllowPresence = ").append(s.ftbChunksAllowPresence()).append("\n");
        toml.append("ftbChunksBlockInteraction = ").append(s.ftbChunksBlockInteraction()).append("\n");
        toml.append("ftbChunksAiClaim = ").append(s.ftbChunksAiClaim()).append("\n");
        toml.append("ftbRanksCompat = ").append(s.ftbRanksCompat()).append("\n");
        toml.append("trustSameTeamAsOwner = ").append(s.trustSameTeamAsOwner()).append("\n");
        toml.append("permAiAsk = \"").append(esc(s.ftbPermAiAsk())).append("\"\n");
        toml.append("permAiActions = \"").append(esc(s.ftbPermAiActions())).append("\"\n");
        toml.append("permCci = \"").append(esc(s.ftbPermCci())).append("\"\n");
        toml.append("permTeamfight = \"").append(esc(s.ftbPermTeamfight())).append("\"\n");
        toml.append("permSpawn = \"").append(esc(s.ftbPermSpawn())).append("\"\n");
        toml.append("\n[mcp]\n");
        toml.append("transport = \"").append(esc(s.mcpTransport().name().toLowerCase())).append("\"\n");
        toml.append("url = \"").append(esc(s.mcpUrl())).append("\"\n");
        toml.append("command = \"").append(esc(s.mcpCommand())).append("\"\n");
        toml.append("args = ").append(stringListToml(s.mcpArgs())).append("\n");
        toml.append("toolName = \"").append(esc(s.mcpToolName())).append("\"\n");
        toml.append("protocolVersion = \"").append(esc(s.mcpProtocolVersion())).append("\"\n");
        toml.append("toolAllowlist = \"").append(esc(s.mcpAllowlist())).append("\"\n");
        java.nio.file.Files.writeString(path, toml.toString(), java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String esc(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String stringListToml(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"').append(esc(values.get(i))).append('"');
        }
        return sb.append(']').toString();
    }
}
