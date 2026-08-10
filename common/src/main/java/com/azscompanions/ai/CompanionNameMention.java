package com.azscompanions.ai;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Detects when chat addresses a companion by display name ({@code Bit, come here}).
 * Multiplayer-safe: matching is by name string only; ownership decides owner vs stranger mode.
 */
public final class CompanionNameMention {
    private CompanionNameMention() {
    }

    /**
     * True when {@code displayName} appears as an addressable word/phrase:
     * start of message, or after whitespace, followed by whitespace / punctuation / end.
     * Case-insensitive. Does not require {@code ask}.
     */
    public static boolean messageMentionsName(String rawMessage, String displayName) {
        if (rawMessage == null || displayName == null) {
            return false;
        }
        String msg = rawMessage.trim();
        String name = displayName.trim();
        if (msg.isEmpty() || name.isEmpty()) {
            return false;
        }
        String msgLower = msg.toLowerCase(Locale.ROOT);
        String nameLower = name.toLowerCase(Locale.ROOT);
        int idx = 0;
        while ((idx = msgLower.indexOf(nameLower, idx)) >= 0) {
            int end = idx + nameLower.length();
            boolean startOk = idx == 0 || isAddressBoundary(msg.charAt(idx - 1));
            boolean endOk = end >= msg.length() || isAddressBoundary(msg.charAt(end));
            if (startOk && endOk) {
                // Prefer vocative: at start, or name then comma/colon
                if (idx == 0) {
                    return true;
                }
                if (end < msg.length()) {
                    char after = msg.charAt(end);
                    if (after == ',' || after == ':' || after == '!' || after == '?') {
                        return true;
                    }
                }
                // Mid-sentence "hey Bit please" — allow after whitespace when name is a whole token
                if (Character.isWhitespace(msg.charAt(idx - 1))) {
                    return true;
                }
            }
            idx++;
        }
        // Sanitized fallback for names with punctuation in display ("Bit!")
        String want = CompanionAskResolve.sanitizeToken(name);
        if (want.length() < 2) {
            return false;
        }
        return Pattern.compile("(?i)(?<![\\p{L}\\p{N}_])" + Pattern.quote(want) + "(?![\\p{L}\\p{N}_])")
                .matcher(msgLower)
                .find();
    }

    private static boolean isAddressBoundary(char c) {
        return Character.isWhitespace(c)
                || c == ',' || c == ':' || c == '!' || c == '?' || c == '.'
                || c == ';' || c == '"' || c == '\'' || c == '(' || c == ')';
    }

    public static String mentionPrompt(String speakerName, String message, boolean speakerIsOwner) {
        if (speakerIsOwner) {
            return CompanionAiChatSupport.ownerAddressPrompt(speakerName, message);
        }
        return CompanionAiChatSupport.strangerAddressPrompt(speakerName, message);
    }
}
