package com.azscompanions.ai;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Shared filters / prompts for chat reactions, idle ambient speech, and call-when-away.
 */
public final class CompanionAiChatSupport {
    public static final double DEFAULT_CHAT_REACT_RANGE = 48.0d;
    public static final int DEFAULT_CHAT_REACT_COOLDOWN_SECONDS = 12;
    /** Rare idle: 8 minutes (9600 ticks). Legacy ~1–3 min configs are remapped here. */
    public static final int DEFAULT_IDLE_CHAT_SECONDS_MIN = 480;
    /** Rare idle: 20 minutes (24000 ticks). */
    public static final int DEFAULT_IDLE_CHAT_SECONDS_MAX = 1200;
    /** Config max at or below this is the old chatty default (~3 min) and is remapped. */
    public static final int LEGACY_CHATTY_IDLE_MAX_SECONDS = 180;
    /** Chance (0–100) that a due idle tick is skipped and a new long interval is scheduled. */
    public static final int DEFAULT_IDLE_SKIP_PERCENT = 60;
    /** Per-companion gap after any spoken line before idle chatter (3 min / 3600 ticks). */
    public static final int DEFAULT_IDLE_SPEAK_COOLDOWN_SECONDS = 180;
    /** Per-companion gap after any spoken line before event chatter (1.5 min / 1800 ticks). */
    public static final int DEFAULT_REACTIVE_SPEAK_COOLDOWN_SECONDS = 90;
    /** Shared per-player gap so only one companion idles at a time (4 min / 4800 ticks). */
    public static final int DEFAULT_PLAYER_AMBIENT_GAP_SECONDS = 240;
    /** Shared per-player gap between event lines (2 min / 2400 ticks). */
    public static final int DEFAULT_REACTIVE_PLAYER_GAP_SECONDS = 120;
    /** Scripted low-health / inventory / danger lines share this floor (1.5 min). */
    public static final int DEFAULT_SCRIPTED_SPEAK_COOLDOWN_SECONDS = 90;
    /** Low-health scripted line interval (2 min / 2400 ticks). Was every 5 seconds. */
    public static final int LOW_HEALTH_SPEAK_INTERVAL_TICKS = 20 * 120;
    /** Inventory-full scripted line interval (3 min / 3600 ticks). Was every 10 seconds. */
    public static final int INVENTORY_FULL_SPEAK_INTERVAL_TICKS = 20 * 180;
    public static final int MAX_SPOKEN_LINE_CHARS = 140;
    public static final int DEFAULT_CALL_PLAYER_AFTER_SECONDS = 90;
    public static final double DEFAULT_CALL_PLAYER_DISTANCE = 48.0d;
    public static final int DEFAULT_CALL_PLAYER_COOLDOWN_SECONDS = 60;

    private static final ConcurrentHashMap<UUID, PlayerChatGate> PLAYER_GATES = new ConcurrentHashMap<>();

    private CompanionAiChatSupport() {
    }

    /**
     * Short player-facing LLM error for chat. Never includes raw JSON bodies (those wrap into
     * confusing {@code "role"}/{@code "content"} lines in Minecraft chat). Full detail stays in logs.
     */
    public static String playerFacingAiError(Throwable error) {
        String raw = error == null ? "unknown error"
                : (error.getMessage() == null || error.getMessage().isBlank()
                ? error.toString() : error.getMessage().trim());
        // Drop any embedded response dump from older clients / nested causes.
        int bodyAt = indexOfIgnoreCase(raw, "Body:");
        if (bodyAt >= 0) {
            raw = raw.substring(0, bodyAt).trim();
        }
        if (raw.contains("\"choices\"") || raw.contains("\"content\"") || raw.contains("\"role\"")) {
            return "Companion AI error: empty or invalid model reply. "
                    + "For Gemma 4, disable thinking / raise maxTokens (512+). See server log.";
        }
        if (raw.toLowerCase(java.util.Locale.ROOT).contains("empty assistant content")) {
            return "Companion AI error: empty model reply (HTTP 200). "
                    + "Check model id; for Gemma 4 disable thinking or raise maxTokens. See server log.";
        }
        if (raw.length() > 140) {
            raw = raw.substring(0, 137) + "…";
        }
        if (!raw.regionMatches(true, 0, "Companion AI", 0, "Companion AI".length())) {
            return "Companion AI error: " + raw;
        }
        return raw;
    }

    private static int indexOfIgnoreCase(String haystack, String needle) {
        if (haystack == null || needle == null) {
            return -1;
        }
        return haystack.toLowerCase(java.util.Locale.ROOT)
                .indexOf(needle.toLowerCase(java.util.Locale.ROOT));
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
     * Heuristic loop guard: companion owner-chat lines look like {@code <Name> …},
     * optionally with a UUID rank prefix ({@code [BRAT] <Name> …}).
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
        text = CompanionChatFormat.stripRankPrefix(text);
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

    /**
     * Idle / react / call-away prompts — lower queue priority and a smaller token budget so
     * {@code /ask} is not stuck behind ambient generation.
     */
    public static boolean isBackgroundPrompt(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String t = message.trim();
        return t.startsWith("[ambient]") || t.startsWith("[react]") || t.startsWith("[call]");
    }

    public static String idleAmbientPrompt(String ownerName) {
        return "[ambient] Your owner " + ownerName
                + " is nearby. One short in-character observation (under 20 words). "
                + "Do not greet, do not say you're here, do not say let's go. Vary the topic.";
    }

    /**
     * Idle or reactive ambient prompt. When {@code focus} or {@code recent} is set, ground the
     * line in those world events (explosion, darkness, craft, find, …).
     */
    public static String ambientPromptWithRecent(
            String ownerName,
            CompanionRecentAction focus,
            java.util.List<CompanionRecentAction> recent) {
        StringBuilder sb = new StringBuilder();
        if (focus != null) {
            sb.append("[react] Your owner ").append(safeName(ownerName))
                    .append(" just did something nearby. React in one short wholesome in-character line. Event: ")
                    .append(describeAction(focus));
            if (focus.kind() == CompanionRecentActionKind.DARKNESS) {
                sb.append(" Ask for light / a torch.");
            } else if (focus.kind() == CompanionRecentActionKind.ITEM_CRAFT
                    && focus.itemId() != null
                    && CompanionNotableItemSupport.isSword(focus.itemId())) {
                sb.append(" Cheer the new sword (e.g. NICE SWORD!).");
            } else if (focus.kind() == CompanionRecentActionKind.EXPLOSION) {
                sb.append(" React to the boom / TNT.");
            } else if (focus.kind() == CompanionRecentActionKind.CUSTOM
                    && focus.detail() != null && !focus.detail().isBlank()) {
                sb.append(' ').append(focus.detail());
            }
        } else {
            sb.append(idleAmbientPrompt(ownerName));
        }
        if (recent != null && !recent.isEmpty()) {
            sb.append(" Recent context:");
            int n = 0;
            for (CompanionRecentAction a : recent) {
                if (a == null || (focus != null && a.equals(focus))) {
                    continue;
                }
                sb.append(" | ").append(describeAction(a));
                if (++n >= 4) {
                    break;
                }
            }
        }
        sb.append(" No greeting spam.");
        return sb.toString();
    }

    public static String describeAction(CompanionRecentAction action) {
        if (action == null) {
            return "something happened";
        }
        if (action.detail() != null && !action.detail().isBlank()) {
            return action.kind().name().toLowerCase(java.util.Locale.ROOT) + ": " + action.detail();
        }
        return action.kind().name().toLowerCase(java.util.Locale.ROOT);
    }

    public static String callPlayerPrompt(String ownerName) {
        return "[call] Your owner " + ownerName
                + " has been away too long. Call their name briefly and ask them to come back.";
    }

    public static String fallbackIdleLine(String ownerName) {
        return fallbackIdleLine(ownerName, null);
    }

    public static String fallbackIdleLine(String ownerName, UUID playerId) {
        String name = safeName(ownerName);
        String[] lines = {
                "Clouds look different from down here.",
                "Wonder if there's a village over that hill…",
                "I keep spotting little caves. Maybe later.",
                "The air smells like rain. Or maybe just dirt.",
                "I've been counting birds. Lost track.",
                "This biome has a mood, doesn't it?",
                "Quiet stretch. I'm okay with that.",
                "If we camp, I want the side facing the dark.",
                "Don't mind me — just people-watching the wildlife.",
                "The sky's doing that pretty thing again.",
                "I packed… wait, did I pack anything?",
                "Hmm. That tree looks climbable. Not that I will.",
                name + ", this stretch of trail is nicer than I expected."
        };
        return pickAvoidingLast(lines, playerId);
    }

    /** Scripted fallback when reacting to a recent event (LLM off or failed). */
    public static String fallbackReactiveLine(String ownerName, CompanionRecentAction focus) {
        String name = safeName(ownerName);
        if (focus == null) {
            return fallbackIdleLine(name);
        }
        return switch (focus.kind()) {
            case EXPLOSION -> {
                String[] lines = {
                        "Whoa — that was loud!",
                        "Ah! Warn me next time, " + name + "!",
                        "My ears… what exploded?",
                        "Okay, who brought the boom?",
                        "I felt that one in my teeth."
                };
                yield pickAvoidingLast(lines, null);
            }
            case DARKNESS -> {
                String[] lines = {
                        "It's so dark… got a torch, " + name + "?",
                        name + ", can we place some light?",
                        "Too dark — I keep imagining eyes.",
                        "A lantern would be nice about now.",
                        "I can barely see the path."
                };
                yield pickAvoidingLast(lines, null);
            }
            case ITEM_CRAFT -> {
                if (focus.itemId() != null) {
                    yield CompanionNotableItemSupport.craftCompliment(focus.itemId());
                }
                yield "Nice craft, " + name + "!";
            }
            case CRAFT_READY -> {
                String item = focus.itemId() == null ? "that"
                        : CompanionNotableItemSupport.prettyName(focus.itemId());
                yield "Hey " + name + ", you've got everything for a " + item + "!";
            }
            case ITEM_FIND -> {
                String item = focus.itemId() == null ? "that"
                        : CompanionNotableItemSupport.prettyName(focus.itemId());
                String[] lines = {
                        "Ooh, " + item + ". That's a keeper.",
                        "You found " + item + "? Lucky.",
                        "Look at that " + item + ".",
                        item + " — didn't expect that today."
                };
                yield pickAvoidingLast(lines, null);
            }
            case CUSTOM -> {
                CompanionCustomChatEvent ev = CompanionChatEventSupport.findById(
                        CompanionChatEventSupport.settings(), focus.customEventId());
                if (ev != null && ev.fallback() != null && !ev.fallback().isBlank()) {
                    yield ev.fallback();
                }
                if (focus.detail() != null && !focus.detail().isBlank()) {
                    yield focus.detail();
                }
                yield fallbackIdleLine(name);
            }
            case DAMAGE -> pickAvoidingLast(new String[] {
                    name + ", are you okay?",
                    "That looked like it hurt.",
                    "Easy — I've got you."
            }, null);
            case COMBAT -> pickAvoidingLast(new String[] {
                    "I've got your back!",
                    "Stay behind me if you need to.",
                    "Watch the flanks."
            }, null);
            case EATING -> pickAvoidingLast(new String[] {
                    "Save me a bite?",
                    "That smells better than my rations.",
                    "Don't forget to share the leftovers."
            }, null);
            case SLEEPING -> pickAvoidingLast(new String[] {
                    "Sleep well… I'll keep an ear out.",
                    "Rest. I'll be nearby.",
                    "Night watch is on me."
            }, null);
            case BLOCK_PLACE, BLOCK_BREAK -> fallbackIdleLine(name);
        };
    }

    private static String safeName(String ownerName) {
        return ownerName == null || ownerName.isBlank() ? "friend" : ownerName.trim();
    }

    private static String pickAvoidingLast(String[] lines, UUID playerId) {
        if (lines == null || lines.length == 0) {
            return "";
        }
        if (lines.length == 1) {
            return lines[0];
        }
        String last = lastSpokenLine(playerId);
        IntRandom rng = ThreadLocalRandom.current()::nextInt;
        int idx = rng.nextInt(lines.length);
        String pick = lines[idx];
        if (last != null && last.equals(pick)) {
            pick = lines[(idx + 1) % lines.length];
        }
        return pick;
    }

    public static String fallbackCallLine(String ownerName) {
        String name = ownerName == null || ownerName.isBlank() ? "friend" : ownerName.trim();
        return name + "? Where did you go?";
    }

    /** True when ambient speech should wait because the companion spoke recently. */
    public static boolean spokeTooRecently(int ticksSinceSpeak, int cooldownSeconds) {
        return ticksSinceSpeak >= 0 && ticksSinceSpeak < Math.max(5, cooldownSeconds) * 20;
    }

    public static int idleSpeakCooldownSeconds() {
        return DEFAULT_IDLE_SPEAK_COOLDOWN_SECONDS;
    }

    public static int reactiveSpeakCooldownSeconds() {
        return DEFAULT_REACTIVE_SPEAK_COOLDOWN_SECONDS;
    }

    public static int scriptedSpeakCooldownSeconds() {
        return DEFAULT_SCRIPTED_SPEAK_COOLDOWN_SECONDS;
    }

    public static void clearPlayerChatGates() {
        PLAYER_GATES.clear();
    }

    public static boolean playerAmbientTooRecent(UUID playerId, long gameTime, boolean reactive) {
        if (playerId == null) {
            return false;
        }
        PlayerChatGate gate = PLAYER_GATES.get(playerId);
        if (gate == null) {
            return false;
        }
        int gap = reactive ? DEFAULT_REACTIVE_PLAYER_GAP_SECONDS : DEFAULT_PLAYER_AMBIENT_GAP_SECONDS;
        synchronized (gate) {
            return gameTime - gate.lastSpeakTick < (long) Math.max(5, gap) * 20L;
        }
    }

    public static void recordAmbientSpeak(UUID playerId, long gameTime, String line) {
        if (playerId == null) {
            return;
        }
        PlayerChatGate gate = PLAYER_GATES.computeIfAbsent(playerId, id -> new PlayerChatGate());
        synchronized (gate) {
            gate.lastSpeakTick = gameTime;
            if (line != null && !line.isBlank()) {
                gate.lastLine = line.trim();
            }
        }
    }

    public static boolean isSameAsLastLine(UUID playerId, String line) {
        if (playerId == null || line == null || line.isBlank()) {
            return false;
        }
        String last = lastSpokenLine(playerId);
        return last != null && last.equalsIgnoreCase(line.trim());
    }

    public static String lastSpokenLine(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        PlayerChatGate gate = PLAYER_GATES.get(playerId);
        if (gate == null) {
            return null;
        }
        synchronized (gate) {
            return gate.lastLine;
        }
    }

    /** First line only, capped — no multi-line dumps in owner chat. */
    public static String shortenSpokenLine(String text) {
        if (text == null) {
            return "";
        }
        String t = text.replace('\r', ' ').trim();
        int nl = t.indexOf('\n');
        if (nl >= 0) {
            t = t.substring(0, nl).trim();
        }
        if (t.length() > MAX_SPOKEN_LINE_CHARS) {
            t = t.substring(0, MAX_SPOKEN_LINE_CHARS - 1) + "…";
        }
        return t;
    }

    public static boolean shouldSkipIdleRoll(IntRandom random) {
        IntRandom rng = random == null ? ThreadLocalRandom.current()::nextInt : random;
        return rng.nextInt(100) < DEFAULT_IDLE_SKIP_PERCENT;
    }

    /**
     * Old chatty configs (~1–3 min max) become the rare 8–20 min range so existing
     * worlds get fewer messages without requiring a settings reset.
     */
    public static int[] effectiveIdleBounds(int minSeconds, int maxSeconds) {
        int min = clampIdleSeconds(Math.min(minSeconds, maxSeconds));
        int max = clampIdleSeconds(Math.max(minSeconds, maxSeconds));
        if (max <= LEGACY_CHATTY_IDLE_MAX_SECONDS) {
            return new int[] { DEFAULT_IDLE_CHAT_SECONDS_MIN, DEFAULT_IDLE_CHAT_SECONDS_MAX };
        }
        return new int[] { min, max };
    }

    public static int nextRareIdleIntervalSeconds(int minSeconds, int maxSeconds, IntRandom random) {
        int[] bounds = effectiveIdleBounds(minSeconds, maxSeconds);
        return nextIdleIntervalSeconds(bounds[0], bounds[1], random);
    }

    public static int nextIdleDelayTicks(int minSeconds, int maxSeconds, double idleMul, IntRandom random) {
        double mul = idleMul <= 0.0d ? 1.0d : idleMul;
        int secs = (int) Math.round(nextRareIdleIntervalSeconds(minSeconds, maxSeconds, random) * mul);
        return Math.max(40, secs * 20);
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

    private static final class PlayerChatGate {
        private long lastSpeakTick;
        private String lastLine;
    }

    /** Minimal RNG bridge so common code stays free of Minecraft types. */
    @FunctionalInterface
    public interface IntRandom {
        int nextInt(int bound);
    }
}
