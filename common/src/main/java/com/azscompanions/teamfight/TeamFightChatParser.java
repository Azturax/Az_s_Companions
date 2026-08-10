package com.azscompanions.teamfight;

import com.azscompanions.cci.CciCompanionParams;

/**
 * CCI-first message helper. Prefer structured IMC {@code key=value} messages from your bot.
 * Freeform chat is not platform-specific (no cheer/gift parsers); unstructured lines pass through empty
 * so streamers define events entirely via CCI subjects + messages.
 */
public final class TeamFightChatParser {
    private TeamFightChatParser() {
    }

    /**
     * Returns structured CCI messages unchanged. Unstructured freeform lines return empty
     * (define interactions via CCI IMC, e.g. {@code amount=500;user=Alice;team=red}).
     */
    public static String toCciMessage(String chatLine) {
        if (chatLine == null || chatLine.isBlank()) {
            return "";
        }
        String trimmed = chatLine.trim();
        if (trimmed.contains("=")) {
            return trimmed;
        }
        return "";
    }

    public static CciCompanionParams parseChatOrMessage(String raw) {
        String msg = raw == null ? "" : raw.trim();
        if (!msg.contains("=")) {
            msg = toCciMessage(msg);
        }
        return CciCompanionParams.parse(msg);
    }
}
