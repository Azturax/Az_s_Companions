package com.azscompanions.ai;

/**
 * Shared filters / prompts for chat reactions, idle ambient speech, and call-when-away.
 */
public final class CompanionAiChatSupport {
    public static final double DEFAULT_CHAT_REACT_RANGE = 48.0d;
    public static final int DEFAULT_CHAT_REACT_COOLDOWN_SECONDS = 20;
    public static final int DEFAULT_IDLE_CHAT_SECONDS_MIN = 90;
    public static final int DEFAULT_IDLE_CHAT_SECONDS_MAX = 240;
    public static final int DEFAULT_CALL_PLAYER_AFTER_SECONDS = 90;
    public static final double DEFAULT_CALL_PLAYER_DISTANCE = 48.0d;
    public static final int DEFAULT_CALL_PLAYER_COOLDOWN_SECONDS = 60;

    private CompanionAiChatSupport() {
    }

    /** Slash commands and blank lines never trigger auto AI. */
    public static boolean shouldIgnoreChatMessage(String raw) {
        if (raw == null) {
            return true;
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return true;
        }
        if (text.startsWith("/")) {
            return true;
        }
        // Explicit named ask is handled separately (owner-scoped); skip listen react.
        return CompanionAskResolve.parseNamedAskChat(text).isPresent();
    }

    /**
     * Heuristic loop guard: companion owner-chat lines look like {@code <Name> …}.
     * Also skips our own action-bar thinking marker if it somehow lands in chat.
     */
    public static boolean looksLikeCompanionReply(String raw) {
        if (raw == null) {
            return false;
        }
        String text = raw.trim();
        if (text.startsWith("… ") && text.contains(" is thinking")) {
            return true;
        }
        return text.startsWith("<") && text.contains("> ");
    }

    public static String chatReactionPrompt(String speakerName, String message) {
        return "[chat from " + speakerName + "] " + message;
    }

    /**
     * Owner addressed the companion by name ({@code Bit, come here}). Obey / help tone.
     */
    public static String ownerAddressPrompt(String ownerName, String message) {
        return "[owner address] Your owner " + ownerName + " is speaking to you by name: \""
                + message + "\". Obey reasonable requests (come here, follow, help) in character.";
    }

    /**
     * Another player addressed the companion by name — be social and helpful, not cold.
     * Grief / inventory tools are blocked server-side.
     */
    public static String strangerAddressPrompt(String speakerName, String message) {
        return "[stranger address] Another player named " + speakerName
                + " (NOT your owner) said to you: \"" + message
                + "\". Be friendly and helpful: chat, answer questions, play briefly (dance, peekaboo, come say hi). "
                + "Do not mine, build, craft, take items, drop gear, or become their follower. Stay loyal to your owner.";
    }

    public static String idleAmbientPrompt(String ownerName) {
        return "[ambient] Your owner " + ownerName
                + " is nearby. Say one short wholesome in-character line — no greeting spam.";
    }

    public static String callPlayerPrompt(String ownerName) {
        return "[call] Your owner " + ownerName
                + " has been away too long. Call their name briefly and ask them to come back.";
    }

    public static String fallbackIdleLine(String ownerName) {
        String name = ownerName == null || ownerName.isBlank() ? "friend" : ownerName.trim();
        String[] lines = {
                "Hey " + name + "… just checking you're still there.",
                "Hmm… nice day for an adventure, " + name + ".",
                "I'm right here if you need me, " + name + ".",
                "Wonder what we should do next…",
                name + ", want to explore a bit?"
        };
        int idx = Math.floorMod(name.hashCode() ^ (int) (System.currentTimeMillis() / 60_000L), lines.length);
        return lines[idx];
    }

    public static String fallbackCallLine(String ownerName) {
        String name = ownerName == null || ownerName.isBlank() ? "friend" : ownerName.trim();
        return name + "? Where did you go?";
    }

    /** True when ambient speech should wait because the companion spoke recently. */
    public static boolean spokeTooRecently(int ticksSinceSpeak, int cooldownSeconds) {
        return ticksSinceSpeak >= 0 && ticksSinceSpeak < Math.max(5, cooldownSeconds) * 20;
    }

    public static int clampIdleSeconds(int value) {
        return Math.max(30, Math.min(3600, value));
    }

    public static int clampCooldownSeconds(int value) {
        return Math.max(5, Math.min(600, value));
    }

    public static double clampRange(double value) {
        return Math.max(8.0d, Math.min(128.0d, value));
    }

    /** Pick a random idle interval in [min, max] seconds (inclusive bounds). */
    public static int nextIdleIntervalSeconds(int minSeconds, int maxSeconds, java.util.Random random) {
        return nextIdleIntervalSeconds(minSeconds, maxSeconds, random::nextInt);
    }

    public static int nextIdleIntervalSeconds(int minSeconds, int maxSeconds, IntRandom random) {
        int min = clampIdleSeconds(Math.min(minSeconds, maxSeconds));
        int max = clampIdleSeconds(Math.max(minSeconds, maxSeconds));
        if (max <= min) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }

    /** Minimal RNG bridge so common code stays free of Minecraft types. */
    @FunctionalInterface
    public interface IntRandom {
        int nextInt(int bound);
    }
}
