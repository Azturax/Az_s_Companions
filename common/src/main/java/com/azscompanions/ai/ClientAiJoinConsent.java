package com.azscompanions.ai;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side memory for the join-time "use server LLM?" prompt.
 * Remembers accept/dismiss per server key persistently (config file) so the prompt
 * asks at most once; later changes go through {@code /az admin} → AI Config.
 */
public final class ClientAiJoinConsent {
    public enum Decision {
        UNDECIDED,
        ACCEPTED,
        DISMISSED
    }

    private static final Map<String, Decision> DECISIONS = new ConcurrentHashMap<>();
    private static volatile String currentServerKey = "unknown";
    private static volatile AiJoinOffer pendingOffer;
    private static volatile Path storePath;
    private static volatile boolean loaded;

    private ClientAiJoinConsent() {
    }

    /**
     * Point at {@code config/azscompanions-ai-join-consent.json} (client only).
     * Loads existing decisions immediately when the path is set.
     */
    public static void configureStore(Path path) {
        storePath = path;
        loaded = false;
        ensureLoaded();
    }

    public static void beginConnection(String serverKey) {
        ensureLoaded();
        currentServerKey = normalizeKey(serverKey);
    }

    public static void endConnection() {
        currentServerKey = "unknown";
        pendingOffer = null;
    }

    public static String currentServerKey() {
        return currentServerKey;
    }

    public static Decision decisionFor(String serverKey) {
        ensureLoaded();
        return DECISIONS.getOrDefault(normalizeKey(serverKey), Decision.UNDECIDED);
    }

    public static boolean shouldPrompt(String serverKey) {
        return decisionFor(serverKey) == Decision.UNDECIDED;
    }

    public static void markAccepted(String serverKey) {
        putPersistent(serverKey, Decision.ACCEPTED);
    }

    public static void markDismissed(String serverKey) {
        putPersistent(serverKey, Decision.DISMISSED);
    }

    public static void setPendingOffer(AiJoinOffer offer) {
        pendingOffer = offer;
    }

    public static AiJoinOffer pendingOffer() {
        return pendingOffer;
    }

    public static void clearPendingOffer() {
        pendingOffer = null;
    }

    public static String normalizeKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "unknown";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static void putPersistent(String serverKey, Decision decision) {
        ensureLoaded();
        String key = normalizeKey(serverKey);
        if ("unknown".equals(key) || decision == null || decision == Decision.UNDECIDED) {
            return;
        }
        DECISIONS.put(key, decision);
        persist();
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path path = storePath;
        if (path == null) {
            return;
        }
        try {
            Map<String, Decision> fromDisk = ClientAiJoinConsentStore.load(path);
            DECISIONS.putAll(fromDisk);
        } catch (Exception ignored) {
            // Keep in-memory defaults; next save may recreate the file.
        }
    }

    private static synchronized void persist() {
        Path path = storePath;
        if (path == null) {
            return;
        }
        try {
            ClientAiJoinConsentStore.save(path, DECISIONS);
        } catch (Exception ignored) {
            // Non-fatal — prompt still skipped for this JVM session via DECISIONS.
        }
    }

    /** Test helper: clear memory + optional reload from a fresh path. */
    static void resetForTests() {
        DECISIONS.clear();
        pendingOffer = null;
        currentServerKey = "unknown";
        storePath = null;
        loaded = false;
    }
}
