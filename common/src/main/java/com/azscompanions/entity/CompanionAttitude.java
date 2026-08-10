package com.azscompanions.entity;

import java.util.Locale;

/**
 * Combat stance for a companion. Independent of visual {@link CompanionForm}.
 * Persisted as {@code PASSIVE} / {@code HOSTILE} on NBT.
 */
public enum CompanionAttitude {
    /**
     * Default friendly companion: never attacks unprovoked; may defend the owner.
     */
    PASSIVE,
    /**
     * Aggressive toward nearby players/mobs except the owner (and trusted players).
     * Still owned by the summoner — useful for stream “spawn threat” events.
     */
    HOSTILE;

    public String serializedName() {
        return name();
    }

    public static CompanionAttitude byName(String value) {
        if (value == null || value.isBlank()) {
            return PASSIVE;
        }
        String trimmed = value.trim().toUpperCase(Locale.ROOT);
        if ("HOSTILE".equals(trimmed) || "AGGRESSIVE".equals(trimmed) || "ATTACK".equals(trimmed)) {
            return HOSTILE;
        }
        return PASSIVE;
    }

    public boolean isHostile() {
        return this == HOSTILE;
    }
}
