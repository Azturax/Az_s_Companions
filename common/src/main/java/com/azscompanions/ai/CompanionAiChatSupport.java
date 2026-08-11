package com.azscompanions.ai;

/**
 * Shared filters / prompts for chat reactions, idle ambient speech, and call-when-away.
 */
public final class CompanionAiChatSupport {
    public static final double DEFAULT_CHAT_REACT_RANGE = 48.0d;
    public static final int DEFAULT_CHAT_REACT_COOLDOWN_SECONDS = 12;
    public static final int DEFAULT_IDLE_CHAT_SECONDS_MIN = 75;
    public static final int DEFAULT_IDLE_CHAT_SECONDS_MAX = 180;
    public static final int DEFAULT_CALL_PLAYER_AFTER_SECONDS = 90;
    public static final double DEFAULT_CALL_PLAYER_DISTANCE = 48.0d;
    public static final int DEFAULT_CALL_PLAYER_COOLDOWN_SECONDS = 60;

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
                + " is nearby. Say one short wholesome in-character line — no greeting spam.";
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
        String name = safeName(ownerName);
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

    /** Scripted fallback when reacting to a recent event (LLM off or failed). */
    public static String fallbackReactiveLine(String ownerName, CompanionRecentAction focus) {
        String name = safeName(ownerName);
        if (focus == null) {
            return fallbackIdleLine(name);
        }
        return switch (focus.kind()) {
            case EXPLOSION -> {
                String[] lines = {
                        "Whoa! That was loud!",
                        "Ah! TNT?! Warn me next time, " + name + "!",
                        "My ears… what exploded?"
                };
                yield pick(lines, name, focus);
            }
            case DARKNESS -> {
                String[] lines = {
                        "It's so dark… got a torch, " + name + "?",
                        name + ", can we place some light? I can barely see.",
                        "Too dark for me — need a torch!"
                };
                yield pick(lines, name, focus);
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
                        "Ooh, " + item + "! Nice find, " + name + "!",
                        "You found " + item + "? Neat!",
                        "Look at that " + item + "!"
                };
                yield pick(lines, name, focus);
            }
            case DAMAGE -> name + ", are you okay?!";
            case COMBAT -> "I've got your back, " + name + "!";
            case EATING -> "Save me a bite?";
            case SLEEPING -> "Sleep well, " + name + "…";
            case BLOCK_PLACE, BLOCK_BREAK -> fallbackIdleLine(name);
        };
    }

    private static String safeName(String ownerName) {
        return ownerName == null || ownerName.isBlank() ? "friend" : ownerName.trim();
    }

    private static String pick(String[] lines, String name, CompanionRecentAction focus) {
        int salt = name.hashCode()
                ^ (focus.kind().ordinal() * 31)
                ^ (focus.itemId() == null ? 0 : focus.itemId().hashCode());
        return lines[Math.floorMod(salt, lines.length)];
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
