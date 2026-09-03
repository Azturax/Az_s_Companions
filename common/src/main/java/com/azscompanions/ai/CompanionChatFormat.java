package com.azscompanions.ai;

import com.azscompanions.AzsCompanionsConstants;

import java.util.UUID;

/**
 * Formats companion / CCI say lines for owner chat. Default is {@code <Name> message}.
 * UUID-gated rank prefixes (currently {@link AzsCompanionsConstants#BRAT_CHAT_PREFIX})
 * are applied when the attributed player owns the companion.
 */
public final class CompanionChatFormat {
    private CompanionChatFormat() {
    }

    /**
     * @param attributedPlayerUuid companion owner (or CCI summon owner); {@code null} → no rank
     */
    public static String formatLine(UUID attributedPlayerUuid, String displayName, String message) {
        String name = displayName == null || displayName.isBlank() ? "Companion" : displayName.trim();
        String body = message == null ? "" : message;
        String tagged = "<" + name + "> " + body;
        String prefix = rankPrefix(attributedPlayerUuid);
        if (prefix == null) {
            return tagged;
        }
        return "[" + prefix + "] " + tagged;
    }

    /**
     * Prefer the companion owner UUID; fall back when the entity has not stored one yet.
     */
    public static String formatLine(UUID ownerUuid, UUID fallbackUuid, String displayName, String message) {
        return formatLine(ownerUuid != null ? ownerUuid : fallbackUuid, displayName, message);
    }

    public static String rankPrefix(UUID attributedPlayerUuid) {
        if (AzsCompanionsConstants.isBratOwner(attributedPlayerUuid)) {
            return AzsCompanionsConstants.BRAT_CHAT_PREFIX;
        }
        return null;
    }

    /** Strip a known rank tag so listen-loop detection still sees {@code <Name> …}. */
    public static String stripRankPrefix(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String tag = "[" + AzsCompanionsConstants.BRAT_CHAT_PREFIX + "] ";
        if (text.startsWith(tag)) {
            return text.substring(tag.length());
        }
        return text;
    }
}
