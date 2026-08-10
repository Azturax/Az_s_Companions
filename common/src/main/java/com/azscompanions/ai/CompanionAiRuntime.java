package com.azscompanions.ai;

import com.azscompanions.compat.hosted.IntegratedMultiplayerCompat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * Process-wide companion AI facade. Safe when provider is {@link LlmProviderMode#DISABLED}.
 * Requests are serialized on one worker thread; overflow is queued (multi-input) instead of dropped.
 */
public final class CompanionAiRuntime {
    private static final Logger LOGGER = LoggerFactory.getLogger("azscompanions/ai");
    private static final CompanionAiRuntime INSTANCE = new CompanionAiRuntime();

    private volatile CompanionAiSettings settings = new CompanionAiSettings();
    private final OpenAiCompatibleClient openAi = new OpenAiCompatibleClient();
    private final McpCompanionClient mcp = new McpCompanionClient();
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final ConcurrentLinkedQueue<QueuedChat> pending = new ConcurrentLinkedQueue<>();
    private final AtomicInteger pendingCount = new AtomicInteger(0);
    /** True while a Minecraft server (dedicated or integrated) is running in this process. */
    private final AtomicBoolean serverHostActive = new AtomicBoolean(false);
    private volatile boolean dedicatedServerHost = false;
    private final Map<UUID, Long> lastChatReactMs = new ConcurrentHashMap<>();
    /** Per-owner throttle for auto chat-listen (dedicated multiplayer spam guard). */
    private final Map<UUID, Long> lastOwnerChatReactMs = new ConcurrentHashMap<>();
    /** Per-companion chat history (independent minds on a shared LLM endpoint). */
    private final CompanionChatMemory chatMemory = new CompanionChatMemory();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "azscompanions-ai");
            t.setDaemon(true);
            return t;
        }
    });

    private CompanionAiRuntime() {
    }

    public static CompanionAiRuntime get() {
        return INSTANCE;
    }

    public CompanionAiSettings settings() {
        return settings;
    }

    /**
     * Called when a Minecraft server starts in this JVM (dedicated or integrated/LAN host).
     * LLM ask / listen / idle / CCI all use this process's config for every companion.
     */
    public void markServerContext(boolean dedicatedServer) {
        dedicatedServerHost = dedicatedServer;
        serverHostActive.set(true);
        if (usesSharedServerLlm()) {
            LOGGER.info(
                    "Companion AI: shared server LLM endpoint (serverLlmOnly={}, dedicated={}); "
                            + "separate minds per companion (perCompanionMemory={}). "
                            + "Joining clients do not need local LM Studio or API keys.",
                    settings.serverLlmOnly(), dedicatedServer, settings.perCompanionMemory());
        }
    }

    /** Called when the Minecraft server stops in this JVM. */
    public void clearServerContext() {
        serverHostActive.set(false);
        dedicatedServerHost = false;
        chatMemory.clearAll();
        pending.clear();
        pendingCount.set(0);
        busy.set(false);
        IntegratedMultiplayerCompat.clear();
    }

    /** In-process per-companion chat buffers (not shared across companions). */
    public CompanionChatMemory chatMemory() {
        return chatMemory;
    }

    public void clearCompanionMemory(UUID companionId) {
        chatMemory.clear(companionId);
    }

    public boolean isServerHostActive() {
        return serverHostActive.get();
    }

    public boolean isDedicatedServerHost() {
        return dedicatedServerHost;
    }

    /**
     * True when this host's AI config is authoritative for all companions
     * ({@code serverLlmOnly}, a dedicated server, or integrated multiplayer with
     * Essential/e4mc/LAN friends when {@code integratedMultiplayerSharedLlm} is on).
     */
    public boolean usesSharedServerLlm() {
        if (!serverHostActive.get()) {
            return false;
        }
        if (settings.serverLlmOnly() || dedicatedServerHost) {
            return true;
        }
        return IntegratedMultiplayerCompat.shouldForceSharedHostLlm(settings);
    }

    public void applySettings(CompanionAiSettings next) {
        this.settings = next == null ? new CompanionAiSettings() : next.copy();
        LOGGER.info("Companion AI provider={} serverLlmOnly={} perCompanionMemory={} memoryMaxMessages={} maxInputChars={} queueMaxDepth={}",
                this.settings.provider(), this.settings.serverLlmOnly(),
                this.settings.perCompanionMemory(), this.settings.memoryMaxMessages(),
                this.settings.maxInputChars(), this.settings.queueMaxDepth());
        if (this.settings.provider().usesOpenAiCompatibleHttp()
                && this.settings.provider() == LlmProviderMode.OPENAI_COMPATIBLE
                && this.settings.resolveApiKey().isBlank()) {
            LOGGER.warn("openai_compatible provider has no API key (config or env {}). Remote calls will likely fail.",
                    this.settings.apiKeyEnv());
        }
        if (this.settings.provider() == LlmProviderMode.MCP
                && this.settings.mcpTransport() == McpTransportMode.STDIO
                && this.settings.mcpCommand().isBlank()) {
            LOGGER.warn("MCP stdio transport configured but mcpCommand is empty");
        }
        if (!this.settings.apiKey().isBlank()) {
            LOGGER.warn("LLM API key is stored in config — prefer env {} for secrets", this.settings.apiKeyEnv());
        }
    }

    public boolean isEnabled() {
        return settings.provider().isEnabled();
    }

    /** True while an LLM call is in flight (queued messages may still be accepted). */
    public boolean isBusy() {
        return busy.get();
    }

    public int pendingQueueSize() {
        return pendingCount.get();
    }

    public boolean canAcceptMoreRequests() {
        if (!busy.get()) {
            return true;
        }
        return pendingCount.get() < settings.queueMaxDepth();
    }

    /**
     * True if this companion may auto-react to chat now (per-companion cooldown).
     * Prefer {@link #canChatReact(UUID, UUID)} on dedicated servers.
     */
    public boolean canChatReact(UUID companionId) {
        return canChatReact(companionId, null);
    }

    /**
     * Companion + owner cooldowns. Owner throttle stops one streamer's listen spam
     * from queuing many companions when {@code chatListenMode=global}.
     */
    public boolean canChatReact(UUID companionId, UUID ownerId) {
        if (companionId == null) {
            return false;
        }
        long cooldownMs = settings.chatReactCooldownSeconds() * 1000L;
        long now = System.currentTimeMillis();
        Long last = lastChatReactMs.get(companionId);
        if (last != null && (now - last) < cooldownMs) {
            return false;
        }
        if (ownerId != null) {
            Long lastOwner = lastOwnerChatReactMs.get(ownerId);
            if (lastOwner != null && (now - lastOwner) < cooldownMs) {
                return false;
            }
        }
        return true;
    }

    public void markChatReact(UUID companionId) {
        markChatReact(companionId, null);
    }

    public void markChatReact(UUID companionId, UUID ownerId) {
        long now = System.currentTimeMillis();
        if (companionId != null) {
            lastChatReactMs.put(companionId, now);
        }
        if (ownerId != null) {
            lastOwnerChatReactMs.put(ownerId, now);
        }
    }

    /**
     * Async chat. {@code onComplete} receives (replyOrNull, errorOrNull) on the AI thread —
     * callers must marshal back to the server thread before touching the world.
     * Full multi-sentence messages are kept; when busy, requests queue up to {@code queueMaxDepth}.
     */
    public boolean requestChatAsync(CompanionChatContext context, BiConsumer<String, Throwable> onComplete) {
        if (!isEnabled()) {
            onComplete.accept(null, new IllegalStateException("Companion AI is disabled"));
            return false;
        }
        CompanionChatContext normalized = normalizeContext(context);
        if (normalized.playerMessage().isBlank()) {
            onComplete.accept(null, new IllegalArgumentException("Empty message"));
            return false;
        }
        if (busy.compareAndSet(false, true)) {
            dispatch(normalized, onComplete);
            return true;
        }
        int maxDepth = settings.queueMaxDepth();
        if (maxDepth <= 0 || pendingCount.get() >= maxDepth) {
            onComplete.accept(null, new IllegalStateException("Companion AI is busy — try again in a moment"));
            return false;
        }
        pending.offer(new QueuedChat(normalized, onComplete));
        pendingCount.incrementAndGet();
        LOGGER.debug("Companion AI queued message (depth={})", pendingCount.get());
        return true;
    }

    private CompanionChatContext normalizeContext(CompanionChatContext context) {
        if (context == null) {
            return new CompanionChatContext("Companion", "player", "Player", "", settings.inputLanguage());
        }
        String msg = CompanionAiInput.normalize(context.playerMessage(), settings);
        if (msg.equals(context.playerMessage())) {
            return context;
        }
        return new CompanionChatContext(
                context.companionId(),
                context.companionName(),
                context.form(),
                context.attitude(),
                context.playerName(),
                msg,
                context.inputLanguage(),
                context.parentName(),
                context.child(),
                context.speakerIsOwner(),
                context.priorTurns(),
                context.persona(),
                context.aiPlayMode());
    }

    private void dispatch(CompanionChatContext context, BiConsumer<String, Throwable> onComplete) {
        CompanionAiSettings snap = settings.copy();
        CompletableFuture.supplyAsync(() -> {
            try {
                return chatBlocking(snap, context).orElse(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor).whenComplete((reply, err) -> {
            Throwable cause = err;
            if (err instanceof RuntimeException re && re.getCause() != null) {
                cause = re.getCause();
            }
            if (cause != null) {
                LOGGER.warn("Companion AI chat failed: {}", cause.toString());
                onComplete.accept(null, cause);
            } else {
                onComplete.accept(reply, null);
            }
            drainQueue();
        });
    }

    private void drainQueue() {
        QueuedChat next = pending.poll();
        if (next == null) {
            busy.set(false);
            return;
        }
        pendingCount.updateAndGet(v -> Math.max(0, v - 1));
        dispatch(next.context(), next.onComplete());
    }

    public Optional<String> chatBlocking(CompanionAiSettings snap, CompanionChatContext context) throws Exception {
        CompanionAiClient client = switch (snap.provider()) {
            case DISABLED -> throw new IllegalStateException("disabled");
            case LOCAL, OPENAI_COMPATIBLE -> openAi;
            case MCP -> mcp;
        };
        CompanionChatContext enriched = enrichWithMemory(snap, context);
        Optional<String> reply = client.chat(snap, enriched);
        recordMemoryTurn(snap, enriched, reply.orElse(null));
        return reply;
    }

    private CompanionChatContext enrichWithMemory(CompanionAiSettings snap, CompanionChatContext context) {
        if (!snap.perCompanionMemory() || context.companionId() == null) {
            return context.withPriorTurns(List.of());
        }
        return context.withPriorTurns(chatMemory.snapshot(context.companionId(), snap.memoryMaxMessages()));
    }

    private void recordMemoryTurn(CompanionAiSettings snap, CompanionChatContext context, String rawReply) {
        if (!snap.perCompanionMemory() || context.companionId() == null || rawReply == null || rawReply.isBlank()) {
            return;
        }
        String speak = CompanionAiActionParser.parse(rawReply).speakText();
        if (speak.isBlank()) {
            speak = rawReply.trim();
        }
        chatMemory.recordExchange(
                context.companionId(),
                context.formattedUserContent(),
                speak,
                snap.memoryMaxMessages());
    }

    public String statusLine() {
        CompanionAiSettings s = settings;
        String listen = " chatListen=" + s.chatListenMode().configName()
                + " nameListen=" + (s.nameListen() ? "on" : "off");
        String idle = s.idleChat() ? " idleChat=on" : "";
        String call = s.callPlayerWhenAway() ? " callAway=on" : "";
        String shared = usesSharedServerLlm() ? " [server LLM shared]" : "";
        String hosted = IntegratedMultiplayerCompat.isIntegratedMultiplayerActive() ? " [hosted MP]" : "";
        String minds = settings.perCompanionMemory() ? " [separate minds]" : "";
        String q = busy.get() ? " [thinking" + (pendingCount.get() > 0 ? "+" + pendingCount.get() : "") + "]" : "";
        return switch (s.provider()) {
            case DISABLED -> "AI: disabled (scripted dialogue only)" + shared + hosted + minds;
            case LOCAL -> "AI: local OpenAI-compatible @ " + s.baseUrl() + " model=" + s.model()
                    + " lang=" + s.inputLanguage() + listen + idle + call + shared + hosted + minds + q;
            case OPENAI_COMPATIBLE -> "AI: openai_compatible @ " + s.baseUrl() + " model=" + s.model()
                    + " lang=" + s.inputLanguage()
                    + (s.resolveApiKey().isBlank() ? " (no API key)" : " (key set)")
                    + listen + idle + call + shared + hosted + minds + q;
            case MCP -> "AI: mcp " + s.mcpTransport().name().toLowerCase()
                    + (s.mcpTransport() == McpTransportMode.HTTP
                    ? " url=" + s.mcpUrl()
                    : " cmd=" + s.mcpCommand())
                    + " tool=" + s.mcpToolName()
                    + " lang=" + s.inputLanguage()
                    + listen + idle + call + shared + hosted + minds + q;
        };
    }

    private record QueuedChat(CompanionChatContext context, BiConsumer<String, Throwable> onComplete) {
    }
}
