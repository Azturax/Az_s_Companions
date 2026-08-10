package com.azscompanions.ai;

/**
 * How child / Bit companions behave relative to their parent leader when AI is on.
 * <ul>
 *   <li>{@link #CLING} — stay very close; mostly talk to parent; rare solo experiments</li>
 *   <li>{@link #BALANCED} — soft leash + occasional curious wander (default)</li>
 *   <li>{@link #CURIOUS} — explore within leash, poke pickups, more independent idle</li>
 * </ul>
 */
public enum ChildAutonomyMode {
    CLING,
    BALANCED,
    CURIOUS;

    public static ChildAutonomyMode fromConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return BALANCED;
        }
        String key = raw.trim().toLowerCase().replace('-', '_');
        return switch (key) {
            case "cling", "close", "attached" -> CLING;
            case "curious", "explore", "independent" -> CURIOUS;
            default -> BALANCED;
        };
    }

    public String configName() {
        return name().toLowerCase();
    }

    /** Multiplier on idle / ambient intervals for children (higher = less chatty). */
    public double idleIntervalMultiplier() {
        return switch (this) {
            case CLING -> 2.2d;
            case BALANCED -> 1.6d;
            case CURIOUS -> 1.25d;
        };
    }

    public double leashRadius() {
        return switch (this) {
            case CLING -> 6.0d;
            case BALANCED -> 10.0d;
            case CURIOUS -> 16.0d;
        };
    }

    public boolean allowsCuriousWander() {
        return this == CURIOUS || this == BALANCED;
    }

    public boolean prefersTalkToParent() {
        return this == CLING || this == BALANCED;
    }
}
