package com.azscompanions.ai;

/**
 * Compatibility facade for chat censoring. Prefer {@link CompanionChatCensor} when
 * {@code censorExtraWords} from settings should apply.
 */
public final class CompanionProfanityFilter {
    private CompanionProfanityFilter() {
    }

    /** Replace blocked words with asterisks of the same length. Null-safe. */
    public static String censor(String text) {
        return CompanionChatCensor.censor(text, null);
    }

    public static boolean containsBlocked(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String censored = censor(text);
        return !censored.equals(text);
    }

    /** Apply censor when {@code enabled}; otherwise return text unchanged. */
    public static String maybeCensor(boolean enabled, String text) {
        if (!enabled || text == null) {
            return text == null ? "" : text;
        }
        return censor(text);
    }
}
