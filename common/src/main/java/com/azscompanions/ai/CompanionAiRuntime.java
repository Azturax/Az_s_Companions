package com.azscompanions.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * Process-wide companion AI facade. Safe when provider is {@link LlmProviderMode#DISABLED}.
 */
public final class CompanionAiRuntime {
    private static final Logger LOGGER = LoggerFactory.getLogger("azscompanions/ai");
    private static final CompanionAiRuntime INSTANCE = new CompanionAiRuntime();

    private volatile CompanionAiSettings settings = new CompanionAiSettings();
    private final OpenAiCompatibleClient openAi = new OpenAiCompatibleClient();
    private final McpCompanionClient mcp = new McpCompanionClient();
    private final AtomicBoolean busy = new AtomicBoolean(false);
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

    public void applySettings(CompanionAiSettings next) {
        this.settings = next == null ? new CompanionAiSettings() : next.copy();
        LOGGER.info("Companion AI provider={}", this.settings.provider());
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

    /**
     * Async chat. {@code onComplete} receives (replyOrNull, errorOrNull) on the AI thread —
     * callers must marshal back to the server thread before touching the world.
     */
    public boolean requestChatAsync(CompanionChatContext context, BiConsumer<String, Throwable> onComplete) {
        if (!isEnabled()) {
            onComplete.accept(null, new IllegalStateException("Companion AI is disabled"));
            return false;
        }
        if (context.playerMessage().isBlank()) {
            onComplete.accept(null, new IllegalArgumentException("Empty message"));
            return false;
        }
        if (!busy.compareAndSet(false, true)) {
            onComplete.accept(null, new IllegalStateException("Companion AI is busy — try again in a moment"));
            return false;
        }
        CompanionAiSettings snap = settings.copy();
        CompletableFuture.supplyAsync(() -> {
            try {
                return chatBlocking(snap, context).orElse(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor).whenComplete((reply, err) -> {
            busy.set(false);
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
        });
        return true;
    }

    public Optional<String> chatBlocking(CompanionAiSettings snap, CompanionChatContext context) throws Exception {
        CompanionAiClient client = switch (snap.provider()) {
            case DISABLED -> throw new IllegalStateException("disabled");
            case LOCAL, OPENAI_COMPATIBLE -> openAi;
            case MCP -> mcp;
        };
        return client.chat(snap, context);
    }

    public String statusLine() {
        CompanionAiSettings s = settings;
        return switch (s.provider()) {
            case DISABLED -> "AI: disabled (scripted dialogue only)";
            case LOCAL -> "AI: local OpenAI-compatible @ " + s.baseUrl() + " model=" + s.model()
                    + " lang=" + s.inputLanguage();
            case OPENAI_COMPATIBLE -> "AI: openai_compatible @ " + s.baseUrl() + " model=" + s.model()
                    + " lang=" + s.inputLanguage()
                    + (s.resolveApiKey().isBlank() ? " (no API key)" : " (key set)");
            case MCP -> "AI: mcp " + s.mcpTransport().name().toLowerCase()
                    + (s.mcpTransport() == McpTransportMode.HTTP
                    ? " url=" + s.mcpUrl()
                    : " cmd=" + s.mcpCommand())
                    + " tool=" + s.mcpToolName()
                    + " lang=" + s.inputLanguage();
        };
    }
}
