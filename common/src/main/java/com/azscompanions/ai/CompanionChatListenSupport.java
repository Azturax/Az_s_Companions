package com.azscompanions.ai;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Filters and reply policy for public-chat auto-react (not {@code /ask}).
 * Name mentions are high priority; nearby chatter is chance + cooldown gated.
 */
public final class CompanionChatListenSupport {
    /** Chance (0–100) a non-mention nearby line may trigger GLOBAL listen. */
    public static final int NEARBY_REPLY_PERCENT = 35;

    private CompanionChatListenSupport() {
    }

    public static boolean shouldIgnoreChat(String raw) {
        if (CompanionAiChatSupport.shouldIgnoreChatMessage(raw)) {
            return true;
        }
        if (CompanionAiChatSupport.looksLikeCompanionReply(raw)) {
            return true;
        }
        return looksLikeSystemOrCci(raw);
    }

    public static boolean looksLikeSystemOrCci(String raw) {
        if (raw == null) {
            return true;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return true;
        }
        String lower = t.toLowerCase(Locale.ROOT);
        if (lower.startsWith("[cci]") || lower.startsWith("cci:") || lower.startsWith("[cci ")) {
            return true;
        }
        if (lower.startsWith("[az]") || lower.startsWith("[azscompanions]")) {
            return true;
        }
        if (lower.startsWith("[system]") || lower.startsWith("[server]")) {
            return true;
        }
        return false;
    }

    /**
     * Nearby (non-mention) replies only in {@link ChatListenMode#GLOBAL}, and not every line.
     */
    public static boolean allowNearbyReply(ChatListenMode mode) {
        return allowNearbyReply(mode, ThreadLocalRandom.current().nextInt(100));
    }

    public static boolean allowNearbyReply(ChatListenMode mode, int roll0to99) {
        if (mode != ChatListenMode.GLOBAL) {
            return false;
        }
        return roll0to99 >= 0 && roll0to99 < NEARBY_REPLY_PERCENT;
    }

    /**
     * Effective listen for one companion. {@code OFF} never auto-replies.
     * Mentions still require a listening mode ({@link ChatListenMode#listens()}).
     */
    public static boolean companionListens(ChatListenMode companionMode, ChatListenMode serverFallback) {
        ChatListenMode mode = companionMode == null ? serverFallback : companionMode;
        return mode != null && mode.listens();
    }

    public static ChatListenMode resolveMode(ChatListenMode companionMode, ChatListenMode serverFallback) {
        if (companionMode != null) {
            return companionMode;
        }
        return serverFallback == null ? ChatListenMode.GLOBAL : serverFallback;
    }
}
