package com.azscompanions.ai;

/**
 * When companions auto-reply to player chat via the LLM pipeline.
 * <ul>
 *   <li>{@link #OFF} — no auto chat reactions</li>
 *   <li>{@link #PLAYER} — companion replies only when its <em>owner</em> chats</li>
 *   <li>{@link #GLOBAL} — nearby public chat may trigger one spawned companion
 *       (name mentions always prefer the addressed companion)</li>
 * </ul>
 */
public enum ChatListenMode {
    OFF,
    PLAYER,
    GLOBAL;

    public static ChatListenMode fromConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return OFF;
        }
        String key = raw.trim().toLowerCase().replace('-', '_');
        return switch (key) {
            case "player", "owner" -> PLAYER;
            case "global", "all", "everyone" -> GLOBAL;
            default -> OFF;
        };
    }

    public boolean listens() {
        return this != OFF;
    }

    public String configName() {
        return name().toLowerCase();
    }
}
