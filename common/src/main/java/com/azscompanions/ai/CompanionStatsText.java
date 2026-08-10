package com.azscompanions.ai;

/**
 * Shared helpers for the companion/owner stats screen (short persona + AI snippets).
 */
public final class CompanionStatsText {
    public static final int PERSONA_SNIPPET = 48;
    public static final int AI_SNIPPET = 72;

    private CompanionStatsText() {
    }

    public static String snippet(String raw, int max) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim().replace('\n', ' ').replace('\r', ' ');
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, Math.max(0, max - 1)) + "…";
    }

    public static String personaSnippet(String raw) {
        return snippet(raw, PERSONA_SNIPPET);
    }

    public static String aiSnippet(String statusLine) {
        return snippet(statusLine, AI_SNIPPET);
    }

    /** Empty when AI provider is disabled. */
    public static String aiStatusIfEnabled() {
        CompanionAiRuntime runtime = CompanionAiRuntime.get();
        if (!runtime.isEnabled()) {
            return "";
        }
        return aiSnippet(runtime.statusLine());
    }
}
