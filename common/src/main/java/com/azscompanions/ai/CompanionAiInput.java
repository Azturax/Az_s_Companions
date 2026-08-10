package com.azscompanions.ai;

/**
 * Normalizes player → companion AI prompts. Preserves full multi-sentence chat;
 * never truncates on {@code .} / first line.
 */
public final class CompanionAiInput {
    public static final int DEFAULT_MAX_INPUT_CHARS = 2000;
    public static final int DEFAULT_QUEUE_MAX_DEPTH = 4;

    private CompanionAiInput() {
    }

    /**
     * Trim, strip nulls, clamp length. Keeps every sentence / line in the message.
     */
    public static String normalize(String raw, int maxChars) {
        if (raw == null) {
            return "";
        }
        String t = raw.replace('\0', ' ').trim();
        if (t.isEmpty()) {
            return "";
        }
        // Collapse only extreme whitespace runs; keep spaces between sentences.
        t = t.replaceAll("[\\t\\x0B\\f\\r]+", " ");
        t = t.replaceAll(" *\\n+ *", "\n").trim();
        int max = clampMaxChars(maxChars);
        if (t.length() > max) {
            t = t.substring(0, max);
        }
        return t;
    }

    public static String normalize(String raw, CompanionAiSettings settings) {
        int max = settings == null ? DEFAULT_MAX_INPUT_CHARS : settings.maxInputChars();
        return normalize(raw, max);
    }

    public static int clampMaxChars(int value) {
        return Math.max(64, Math.min(8000, value <= 0 ? DEFAULT_MAX_INPUT_CHARS : value));
    }

    public static int clampQueueDepth(int value) {
        return Math.max(0, Math.min(16, value));
    }

    /**
     * Soft HUD progress from elapsed time vs configured timeout.
     * Caps below 1.0 so the bar never looks “done” before the reply arrives.
     */
    public static float softProgress(long startedAtMs, int timeoutSeconds, long nowMs) {
        int timeout = Math.max(5, Math.min(120, timeoutSeconds <= 0 ? 30 : timeoutSeconds));
        long elapsed = Math.max(0L, nowMs - startedAtMs);
        float p = (float) elapsed / (timeout * 1000f);
        if (p < 0.02f) {
            return 0.02f;
        }
        return Math.min(0.95f, p);
    }
}
