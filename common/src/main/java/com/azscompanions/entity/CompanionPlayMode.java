package com.azscompanions.entity;

/**
 * Light social / play behaviors driven by AI tools or CCI.
 */
public enum CompanionPlayMode {
    NONE,
    /** Sprint toward owner playfully. */
    RUN_AT_PLAYER,
    /** Path to nearby cover then sit quietly. */
    HIDE,
    /** Actively seek / path to the owner. */
    SEEK,
    /** Spin / dance in place. */
    DANCE,
    /** Sit still for peekaboo, then jump up. */
    PEEKABOO;

    public static CompanionPlayMode fromConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return NONE;
        }
        String key = raw.trim().toUpperCase().replace('-', '_');
        try {
            return valueOf(key);
        } catch (IllegalArgumentException e) {
            return switch (key.toLowerCase()) {
                case "run", "rush", "charge" -> RUN_AT_PLAYER;
                case "hider" -> HIDE;
                case "seeker" -> SEEK;
                case "spin" -> DANCE;
                case "peek" -> PEEKABOO;
                default -> NONE;
            };
        }
    }
}
