package com.azscompanions.entity;

/**
 * Caps for CCI / cake child (Bit) companions under a leader.
 */
public final class CompanionChildLimits {
    /** Default max active children per leader. */
    public static final int MAX_PER_LEADER = 6;
    /** Absolute max for a single {@code count=} request. */
    public static final int MAX_SPAWN_COUNT = 8;
    /** Default body scale when {@code size=} omitted. */
    public static final float DEFAULT_BODY_SCALE = 0.5f;
    /** Default display name when {@code name=} omitted. */
    public static final String DEFAULT_NAME = "Bit";

    private CompanionChildLimits() {
    }

    public static int clampSpawnCount(int count) {
        return Math.max(1, Math.min(MAX_SPAWN_COUNT, count));
    }

    public static int remainingSlots(int currentChildren, int maxPerLeader) {
        int cap = Math.max(1, Math.min(MAX_SPAWN_COUNT, maxPerLeader));
        return Math.max(0, cap - Math.max(0, currentChildren));
    }

    public static int remainingSlots(int currentChildren) {
        return remainingSlots(currentChildren, MAX_PER_LEADER);
    }
}
