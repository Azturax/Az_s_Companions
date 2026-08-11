package com.azscompanions.ai;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side memory for the join-time "use server LLM?" prompt.
 * Remembers accept/dismiss per server key for the JVM session (no auto-connect).
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

    private ClientAiJoinConsent() {
    }

    public static void beginConnection(String serverKey) {
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
        return DECISIONS.getOrDefault(normalizeKey(serverKey), Decision.UNDECIDED);
    }

    public static boolean shouldPrompt(String serverKey) {
        return decisionFor(serverKey) == Decision.UNDECIDED;
    }

    public static void markAccepted(String serverKey) {
        DECISIONS.put(normalizeKey(serverKey), Decision.ACCEPTED);
    }

    public static void markDismissed(String serverKey) {
        DECISIONS.put(normalizeKey(serverKey), Decision.DISMISSED);
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
}
