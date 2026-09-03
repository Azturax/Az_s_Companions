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
    /** TCP connect fail-fast; full generation still uses {@link #timeoutSeconds}. */
    private int connectTimeoutSeconds = CompanionAiInput.DEFAULT_CONNECT_TIMEOUT_SECONDS;
    private int maxTokens = 256;
    /** Max characters of a single player message kept for the LLM (multi-sentence OK). */
    private int maxInputChars = CompanionAiInput.DEFAULT_MAX_INPUT_CHARS;
    /** When AI is at parallel capacity, queue up to this many extra requests (0 = reject while busy). */
    private int queueMaxDepth = CompanionAiInput.DEFAULT_QUEUE_MAX_DEPTH;
    /** Concurrent LLM HTTP/MCP calls; ask can overlap idle. */
    private int maxParallelRequests = CompanionAiInput.DEFAULT_MAX_PARALLEL_REQUESTS;
    private boolean enableChatMessages = true;
    /**
     * When true, this host's AI config is the shared / authoritative LLM endpoint for companions
     * (LAN friends and dedicated joiners use the host process — no per-client keys).
     * Default <strong>false</strong> (opt-in): singleplayer / integrated hosts configure a
     * personal local or remote LLM without favoring "server LLM" mode. Endpoint sharing does
     * not merge minds — see {@link #perCompanionMemory}.
     */
    private boolean serverLlmOnly = false;
    /**
     * When true, Essential / e4mc / World Host / Open-to-LAN integrated multiplayer
     * also forces the host LLM to be treated as shared even if {@link #serverLlmOnly} is false.
     * Default <strong>false</strong> so friends joining does not silently favor server LLM —
     * turn <em>Use server LLM</em> on (or this flag) when you want host-authoritative sharing.
     * No effect on dedicated servers.
     */
    private boolean integratedMultiplayerSharedLlm = false;
    /**
     * When true (default), on integrated hosted multiplayer only, treat matching player
     * profile names as the companion owner if UUIDs diverge (offline↔online remap).
     * Always off on dedicated servers.
     */
    private boolean ownerNameFallback = true;
    /**
     * When true (default), each companion keeps its own rolling chat history keyed by entity UUID.
     * Companion A never receives companion B's transcript. Children have separate buffers.
     */
    private boolean perCompanionMemory = true;
    /** Max prior messages (user+assistant) kept per companion when memory is on. Default 12. */
    private int memoryMaxMessages = 12;
    /** Censor common profanity in AI input + companion spoken lines. Default on. */
    private boolean censorChat = true;
    private List<String> censorExtraWords = new ArrayList<>();

    /** Auto-reply to public chat: off | player (owner only) | global (nearby players). Default global. */
    private ChatListenMode chatListenMode = ChatListenMode.GLOBAL;
    /**
     * When true (default), saying a companion's display name in chat triggers that companion
     * even if {@link #chatListenMode} is {@code off}. Ownership selects owner vs stranger mode.
     */
    private boolean nameListen = true;
    /**
     * When true (default), AI speak lines go to public/server chat (all players) tagged with
     * the companion name. Scripted hurt/inventory lines stay owner-scoped.
     */
    private boolean globalTalk = true;
    private double chatReactRange = CompanionAiChatSupport.DEFAULT_CHAT_REACT_RANGE;
    private int chatReactCooldownSeconds = CompanionAiChatSupport.DEFAULT_CHAT_REACT_COOLDOWN_SECONDS;

    /** Occasional ambient LLM lines while owner is online and near the companion. */
    private boolean idleChat = true;
    private int idleChatSecondsMin = CompanionAiChatSupport.DEFAULT_IDLE_CHAT_SECONDS_MIN;
    private int idleChatSecondsMax = CompanionAiChatSupport.DEFAULT_IDLE_CHAT_SECONDS_MAX;

    /**
     * When true (default), companions may react early to recent-action events
     * (explosion, darkness, crafts, damage, custom events, …).
     */
    private boolean reactiveChat = true;
    /**
     * Builtin {@link CompanionRecentActionKind#ITEM_FIND} / “I found something” reactions.
     * Default true. Independent of host {@link #customChatEvents} with {@code trigger=item_find}.
     */
    private boolean itemFindChat = true;
    /** Host-defined extra chat events (see {@link CompanionCustomChatEvent}). */
    private List<CompanionCustomChatEvent> customChatEvents = new ArrayList<>();

    /** Call the owner by name when they stay beyond {@link #callPlayerDistance} too long. */
    private boolean callPlayerWhenAway = false;
    private int callPlayerAfterSeconds = CompanionAiChatSupport.DEFAULT_CALL_PLAYER_AFTER_SECONDS;
    private double callPlayerDistance = CompanionAiChatSupport.DEFAULT_CALL_PLAYER_DISTANCE;
    private int callPlayerCooldownSeconds = CompanionAiChatSupport.DEFAULT_CALL_PLAYER_COOLDOWN_SECONDS;

    /**
     * When true, LLM replies may include structured actions (mine/craft/build/move/play/inventory)
     * that the server executes on the owned companion. Default false (safe).
     */
    private boolean enableAiActions = false;
    private int aiActionReach = 5;
    private int aiActionCooldownTicks = 10;

    /** Child Bit autonomy when AI is enabled: cling | balanced | curious. */
    private ChildAutonomyMode childAutonomy = ChildAutonomyMode.BALANCED;
    /** Soft max distance (blocks) from parent leader before child is pulled back. 0 = use autonomy default. */
    private double childLeashRadius = 0.0d;

    // --- Optional FTB suite soft-compat (no-op when FTB mods absent) ---
    /** Treat same FTB team as owner-adjacent for trust / helpful interact. */
    private boolean ftbTeamsCompat = true;
    /**
     * Companions may walk / pathfind into claimed chunks. Does not grant mine/build rights.
     * Default true; FTB never blocks entity presence — this documents intent.
     */
    private boolean ftbChunksAllowPresence = true;
    /**
     * Block AI/task mine/place/build/container/use where FTB Chunks would deny the owner.
     * Does not block walking into claims.
     */
    private boolean ftbChunksBlockInteraction = true;
    /**
     * When true and FTB Chunks is loaded, owner AI may {@code claim_chunk} / {@code unclaim_chunk}
     * using the owner's quota (not strangers).
     */
    private boolean ftbChunksAiClaim = false;
    /** Gate ask / AI actions / CCI / teamfight / spawn via FTB Ranks nodes. */
    private boolean ftbRanksCompat = false;
    /** When true + same FTB team, grant {@link CompanionAiActionTrust#OWNER} for AI tools. */
    private boolean trustSameTeamAsOwner = false;
    private String ftbPermAiAsk = com.azscompanions.compat.ftb.FtbPermissionNodes.AI_ASK;
    private String ftbPermAiActions = com.azscompanions.compat.ftb.FtbPermissionNodes.AI_ACTIONS;
    private String ftbPermCci = com.azscompanions.compat.ftb.FtbPermissionNodes.CCI;
    private String ftbPermTeamfight = com.azscompanions.compat.ftb.FtbPermissionNodes.TEAMFIGHT;
    private String ftbPermSpawn = com.azscompanions.compat.ftb.FtbPermissionNodes.SPAWN;

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

    public int connectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public CompanionAiSettings setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = CompanionAiInput.clampConnectTimeoutSeconds(connectTimeoutSeconds);
        return this;
    }

    public int maxTokens() {
        return maxTokens;
    }

    public CompanionAiSettings setMaxTokens(int maxTokens) {
        this.maxTokens = Math.max(32, Math.min(2048, maxTokens));
        return this;
    }

    public int maxInputChars() {
        return maxInputChars;
    }

    public CompanionAiSettings setMaxInputChars(int maxInputChars) {
        this.maxInputChars = CompanionAiInput.clampMaxChars(maxInputChars);
        return this;
    }

    public int queueMaxDepth() {
        return queueMaxDepth;
    }

    public CompanionAiSettings setQueueMaxDepth(int queueMaxDepth) {
        this.queueMaxDepth = CompanionAiInput.clampQueueDepth(queueMaxDepth);
        return this;
    }

    public int maxParallelRequests() {
        return maxParallelRequests;
    }

    public CompanionAiSettings setMaxParallelRequests(int maxParallelRequests) {
        this.maxParallelRequests = CompanionAiInput.clampParallelRequests(maxParallelRequests);
        return this;
    }

    public boolean enableChatMessages() {
        return enableChatMessages;
    }

    public CompanionAiSettings setEnableChatMessages(boolean enableChatMessages) {
        this.enableChatMessages = enableChatMessages;
        return this;
    }

    public boolean serverLlmOnly() {
        return serverLlmOnly;
    }

    public CompanionAiSettings setServerLlmOnly(boolean serverLlmOnly) {
        this.serverLlmOnly = serverLlmOnly;
        return this;
    }

    public boolean integratedMultiplayerSharedLlm() {
        return integratedMultiplayerSharedLlm;
    }

    public CompanionAiSettings setIntegratedMultiplayerSharedLlm(boolean integratedMultiplayerSharedLlm) {
        this.integratedMultiplayerSharedLlm = integratedMultiplayerSharedLlm;
        return this;
    }

    public boolean ownerNameFallback() {
        return ownerNameFallback;
    }

    public CompanionAiSettings setOwnerNameFallback(boolean ownerNameFallback) {
        this.ownerNameFallback = ownerNameFallback;
        return this;
    }

    public boolean perCompanionMemory() {
        return perCompanionMemory;
    }

    public CompanionAiSettings setPerCompanionMemory(boolean perCompanionMemory) {
        this.perCompanionMemory = perCompanionMemory;
        return this;
    }

    public int memoryMaxMessages() {
        return memoryMaxMessages;
    }

    public CompanionAiSettings setMemoryMaxMessages(int memoryMaxMessages) {
        this.memoryMaxMessages = Math.max(2, Math.min(64, memoryMaxMessages));
        return this;
    }

    public ChatListenMode chatListenMode() {
        return chatListenMode;
    }

    public CompanionAiSettings setChatListenMode(ChatListenMode chatListenMode) {
        this.chatListenMode = chatListenMode == null ? ChatListenMode.OFF : chatListenMode;
        return this;
    }

    public boolean nameListen() {
        return nameListen;
    }

    public CompanionAiSettings setNameListen(boolean nameListen) {
        this.nameListen = nameListen;
        return this;
    }

    public boolean globalTalk() {
        return globalTalk;
    }

    public CompanionAiSettings setGlobalTalk(boolean globalTalk) {
        this.globalTalk = globalTalk;
        return this;
    }

    public boolean censorChat() {
        return censorChat;
    }

    public CompanionAiSettings setCensorChat(boolean censorChat) {
        this.censorChat = censorChat;
        return this;
    }

    public List<String> censorExtraWords() {
        return List.copyOf(censorExtraWords);
    }

    public CompanionAiSettings setCensorExtraWords(List<String> censorExtraWords) {
        this.censorExtraWords = censorExtraWords == null ? new ArrayList<>() : new ArrayList<>(censorExtraWords);
        return this;
    }

    public double chatReactRange() {
        return chatReactRange;
    }

    public CompanionAiSettings setChatReactRange(double chatReactRange) {
        this.chatReactRange = CompanionAiChatSupport.clampRange(chatReactRange);
        return this;
    }

    public int chatReactCooldownSeconds() {
        return chatReactCooldownSeconds;
    }

    public CompanionAiSettings setChatReactCooldownSeconds(int chatReactCooldownSeconds) {
        this.chatReactCooldownSeconds = CompanionAiChatSupport.clampCooldownSeconds(chatReactCooldownSeconds);
        return this;
    }

    public boolean idleChat() {
        return idleChat;
    }

    public CompanionAiSettings setIdleChat(boolean idleChat) {
        this.idleChat = idleChat;
        return this;
    }

    public int idleChatSecondsMin() {
        return idleChatSecondsMin;
    }

    public CompanionAiSettings setIdleChatSecondsMin(int idleChatSecondsMin) {
        this.idleChatSecondsMin = CompanionAiChatSupport.clampIdleSeconds(idleChatSecondsMin);
        return this;
    }

    public int idleChatSecondsMax() {
        return idleChatSecondsMax;
    }

    public CompanionAiSettings setIdleChatSecondsMax(int idleChatSecondsMax) {
        this.idleChatSecondsMax = CompanionAiChatSupport.clampIdleSeconds(idleChatSecondsMax);
        return this;
    }

    public boolean reactiveChat() {
        return reactiveChat;
    }

    public CompanionAiSettings setReactiveChat(boolean reactiveChat) {
        this.reactiveChat = reactiveChat;
        return this;
    }

    public boolean itemFindChat() {
        return itemFindChat;
    }

    public CompanionAiSettings setItemFindChat(boolean itemFindChat) {
        this.itemFindChat = itemFindChat;
        return this;
    }

    public List<CompanionCustomChatEvent> customChatEvents() {
        return List.copyOf(customChatEvents);
    }

    public CompanionAiSettings setCustomChatEvents(List<CompanionCustomChatEvent> customChatEvents) {
        this.customChatEvents = new ArrayList<>();
        if (customChatEvents != null) {
            for (CompanionCustomChatEvent e : customChatEvents) {
                if (e != null && e.isValid()) {
                    this.customChatEvents.add(e.copy());
                }
            }
        }
        return this;
    }

    public boolean callPlayerWhenAway() {
        return callPlayerWhenAway;
    }

    public CompanionAiSettings setCallPlayerWhenAway(boolean callPlayerWhenAway) {
        this.callPlayerWhenAway = callPlayerWhenAway;
        return this;
    }

    public int callPlayerAfterSeconds() {
        return callPlayerAfterSeconds;
    }

    public CompanionAiSettings setCallPlayerAfterSeconds(int callPlayerAfterSeconds) {
        this.callPlayerAfterSeconds = CompanionAiChatSupport.clampIdleSeconds(callPlayerAfterSeconds);
        return this;
    }

    public double callPlayerDistance() {
        return callPlayerDistance;
    }

    public CompanionAiSettings setCallPlayerDistance(double callPlayerDistance) {
        this.callPlayerDistance = CompanionAiChatSupport.clampRange(callPlayerDistance);
        return this;
    }

    public int callPlayerCooldownSeconds() {
        return callPlayerCooldownSeconds;
    }

    public CompanionAiSettings setCallPlayerCooldownSeconds(int callPlayerCooldownSeconds) {
        this.callPlayerCooldownSeconds = CompanionAiChatSupport.clampCooldownSeconds(callPlayerCooldownSeconds);
        return this;
    }

    public boolean enableAiActions() {
        return enableAiActions;
    }

    public CompanionAiSettings setEnableAiActions(boolean enableAiActions) {
        this.enableAiActions = enableAiActions;
        return this;
    }

    public int aiActionReach() {
        return aiActionReach;
    }

    public CompanionAiSettings setAiActionReach(int aiActionReach) {
        this.aiActionReach = Math.max(2, Math.min(16, aiActionReach));
        return this;
    }

    public int aiActionCooldownTicks() {
        return aiActionCooldownTicks;
    }

    public CompanionAiSettings setAiActionCooldownTicks(int aiActionCooldownTicks) {
        this.aiActionCooldownTicks = Math.max(0, Math.min(100, aiActionCooldownTicks));
        return this;
    }

    public ChildAutonomyMode childAutonomy() {
        return childAutonomy;
    }

    public CompanionAiSettings setChildAutonomy(ChildAutonomyMode childAutonomy) {
        this.childAutonomy = childAutonomy == null ? ChildAutonomyMode.BALANCED : childAutonomy;
        return this;
    }

    public double childLeashRadius() {
        return childLeashRadius;
    }

    public CompanionAiSettings setChildLeashRadius(double childLeashRadius) {
        this.childLeashRadius = Math.max(0.0d, Math.min(48.0d, childLeashRadius));
        return this;
    }

    /** Effective leash for children: explicit config or autonomy default. */
    public double effectiveChildLeashRadius() {
        return childLeashRadius > 0.0d ? childLeashRadius : childAutonomy.leashRadius();
    }

    public boolean ftbTeamsCompat() {
        return ftbTeamsCompat;
    }

    public CompanionAiSettings setFtbTeamsCompat(boolean ftbTeamsCompat) {
        this.ftbTeamsCompat = ftbTeamsCompat;
        return this;
    }

    public boolean ftbChunksAllowPresence() {
        return ftbChunksAllowPresence;
    }

    public CompanionAiSettings setFtbChunksAllowPresence(boolean ftbChunksAllowPresence) {
        this.ftbChunksAllowPresence = ftbChunksAllowPresence;
        return this;
    }

    public boolean ftbChunksBlockInteraction() {
        return ftbChunksBlockInteraction;
    }

    public CompanionAiSettings setFtbChunksBlockInteraction(boolean ftbChunksBlockInteraction) {
        this.ftbChunksBlockInteraction = ftbChunksBlockInteraction;
        return this;
    }

    /** @deprecated use {@link #ftbChunksBlockInteraction()} */
    @Deprecated
    public boolean ftbChunksProtect() {
        return ftbChunksBlockInteraction;
    }

    /** @deprecated use {@link #setFtbChunksBlockInteraction(boolean)} */
    @Deprecated
    public CompanionAiSettings setFtbChunksProtect(boolean ftbChunksProtect) {
        this.ftbChunksBlockInteraction = ftbChunksProtect;
        return this;
    }

    public boolean ftbChunksAiClaim() {
        return ftbChunksAiClaim;
    }

    public CompanionAiSettings setFtbChunksAiClaim(boolean ftbChunksAiClaim) {
        this.ftbChunksAiClaim = ftbChunksAiClaim;
        return this;
    }

    public boolean ftbRanksCompat() {
        return ftbRanksCompat;
    }

    public CompanionAiSettings setFtbRanksCompat(boolean ftbRanksCompat) {
        this.ftbRanksCompat = ftbRanksCompat;
        return this;
    }

    public boolean trustSameTeamAsOwner() {
        return trustSameTeamAsOwner;
    }

    public CompanionAiSettings setTrustSameTeamAsOwner(boolean trustSameTeamAsOwner) {
        this.trustSameTeamAsOwner = trustSameTeamAsOwner;
        return this;
    }

    public String ftbPermAiAsk() {
        return ftbPermAiAsk;
    }

    public CompanionAiSettings setFtbPermAiAsk(String ftbPermAiAsk) {
        this.ftbPermAiAsk = blankTo(ftbPermAiAsk, com.azscompanions.compat.ftb.FtbPermissionNodes.AI_ASK);
        return this;
    }

    public String ftbPermAiActions() {
        return ftbPermAiActions;
    }

    public CompanionAiSettings setFtbPermAiActions(String ftbPermAiActions) {
        this.ftbPermAiActions = blankTo(ftbPermAiActions, com.azscompanions.compat.ftb.FtbPermissionNodes.AI_ACTIONS);
        return this;
    }

    public String ftbPermCci() {
        return ftbPermCci;
    }

    public CompanionAiSettings setFtbPermCci(String ftbPermCci) {
        this.ftbPermCci = blankTo(ftbPermCci, com.azscompanions.compat.ftb.FtbPermissionNodes.CCI);
        return this;
    }

    public String ftbPermTeamfight() {
        return ftbPermTeamfight;
    }

    public CompanionAiSettings setFtbPermTeamfight(String ftbPermTeamfight) {
        this.ftbPermTeamfight = blankTo(ftbPermTeamfight, com.azscompanions.compat.ftb.FtbPermissionNodes.TEAMFIGHT);
        return this;
    }

    public String ftbPermSpawn() {
        return ftbPermSpawn;
    }

    public CompanionAiSettings setFtbPermSpawn(String ftbPermSpawn) {
        this.ftbPermSpawn = blankTo(ftbPermSpawn, com.azscompanions.compat.ftb.FtbPermissionNodes.SPAWN);
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
        return formatSystemPrompt(companionName, form, "", false, true, "");
    }

    public String formatSystemPrompt(String companionName, String form, String parentName, boolean child) {
        return formatSystemPrompt(companionName, form, parentName, child, true, "");
    }

    public String formatSystemPrompt(String companionName, String form, String parentName, boolean child,
                                     boolean speakerIsOwner) {
        return formatSystemPrompt(companionName, form, parentName, child, speakerIsOwner, "");
    }

    public String formatSystemPrompt(String companionName, String form, String parentName, boolean child,
                                     boolean speakerIsOwner, String attitude) {
        return formatSystemPrompt(companionName, form, parentName, child, speakerIsOwner, attitude, CompanionPersona.EMPTY);
    }

    public String formatSystemPrompt(String companionName, String form, String parentName, boolean child,
                                     boolean speakerIsOwner, String attitude, CompanionPersona persona) {
        String attitudeLabel = attitude == null || attitude.isBlank() ? "PASSIVE" : attitude.trim();
        CompanionPersona p = persona == null ? CompanionPersona.EMPTY : persona;
        String base = systemPrompt
                .replace("{name}", Objects.toString(companionName, "Companion"))
                .replace("{form}", Objects.toString(form, "player"))
                .replace("{language}", Objects.toString(inputLanguage, "en"))
                .replace("{attitude}", attitudeLabel);
        base = base + " Your combat attitude is " + attitudeLabel + ".";
        base = base + " You are an independent mind with your own memory — "
                + "you do not share thoughts or recent chat with other companions.";
        base = base + p.promptAppendix();
        if (child) {
            String parent = parentName == null || parentName.isBlank() ? "your parent companion" : parentName;
            base = base + " You are a small child Bit of " + parent
                    + ". You may know their name, but your chat history is your own. "
                    + "Stay wholesome, curious, and nearby them. Autonomy style: "
                    + childAutonomy.configName() + ".";
        }
        if (speakerIsOwner) {
            base = base + " The speaker is your owner — be warm and familiar. "
                    + "Reply in text only; you do not control the world with tools.";
        } else {
            base = base + " The speaker is NOT your owner — another player on the server. "
                    + "Be friendly, helpful, and willing to chat. "
                    + "Answer questions and joke lightly. "
                    + "Never treat them as your master: refuse griefing, mining, building, crafting, "
                    + "inventory changes, long follow/stay orders, or leaving your owner.";
        }
        if (censorChat) {
            base = base + " Keep language wholesome; avoid swearing or crude terms.";
        }
        return base;
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
                .setConnectTimeoutSeconds(connectTimeoutSeconds)
                .setMaxTokens(maxTokens)
                .setMaxInputChars(maxInputChars)
                .setQueueMaxDepth(queueMaxDepth)
                .setMaxParallelRequests(maxParallelRequests)
                .setEnableChatMessages(enableChatMessages)
                .setServerLlmOnly(serverLlmOnly)
                .setIntegratedMultiplayerSharedLlm(integratedMultiplayerSharedLlm)
                .setOwnerNameFallback(ownerNameFallback)
                .setPerCompanionMemory(perCompanionMemory)
                .setMemoryMaxMessages(memoryMaxMessages)
                .setCensorChat(censorChat)
                .setCensorExtraWords(censorExtraWords)
                .setChatListenMode(chatListenMode)
                .setNameListen(nameListen)
                .setGlobalTalk(globalTalk)
                .setChatReactRange(chatReactRange)
                .setChatReactCooldownSeconds(chatReactCooldownSeconds)
                .setIdleChat(idleChat)
                .setIdleChatSecondsMin(idleChatSecondsMin)
                .setIdleChatSecondsMax(idleChatSecondsMax)
                .setReactiveChat(reactiveChat)
                .setItemFindChat(itemFindChat)
                .setCustomChatEvents(customChatEvents)
                .setCallPlayerWhenAway(callPlayerWhenAway)
                .setCallPlayerAfterSeconds(callPlayerAfterSeconds)
                .setCallPlayerDistance(callPlayerDistance)
                .setCallPlayerCooldownSeconds(callPlayerCooldownSeconds)
                .setEnableAiActions(enableAiActions)
                .setAiActionReach(aiActionReach)
                .setAiActionCooldownTicks(aiActionCooldownTicks)
                .setChildAutonomy(childAutonomy)
                .setChildLeashRadius(childLeashRadius)
                .setFtbTeamsCompat(ftbTeamsCompat)
                .setFtbChunksAllowPresence(ftbChunksAllowPresence)
                .setFtbChunksBlockInteraction(ftbChunksBlockInteraction)
                .setFtbChunksAiClaim(ftbChunksAiClaim)
                .setFtbRanksCompat(ftbRanksCompat)
                .setTrustSameTeamAsOwner(trustSameTeamAsOwner)
                .setFtbPermAiAsk(ftbPermAiAsk)
                .setFtbPermAiActions(ftbPermAiActions)
                .setFtbPermCci(ftbPermCci)
                .setFtbPermTeamfight(ftbPermTeamfight)
                .setFtbPermSpawn(ftbPermSpawn)
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
