package com.azscompanions.entity;

/**
 * Shared companion follow / personal-space bands (blocks).
 * Used by NeoForge and Fabric goals so both loaders stay in lockstep.
 */
public final class CompanionFollowDistances {
    /** Never stand closer than this to the owner. */
    public static final double MIN_PERSONAL_SPACE = 2.0d;
    /** Preferred stand-off while loosely following (ring center). */
    public static final double PREFERRED_DISTANCE = 6.0d;
    /** Comfortable band max — within {@link #MIN_PERSONAL_SPACE}–this, stroll instead of bee-lining. */
    public static final double COMFORT_MAX = 12.0d;
    /** Begin pathing back toward the owner only beyond this distance. */
    public static final double FOLLOW_START = 32.0d;
    /** Stop closing the gap once within this distance (≥ personal space). */
    public static final double FOLLOW_STOP = 8.0d;
    /** Ground teleport leash while exploring (never while idle / fighting). */
    public static final double TELEPORT_DISTANCE = 48.0d;
    /** Idle free-wander ring around the owner (loaded chunks only). */
    public static final double IDLE_WANDER_MIN = 24.0d;
    public static final double IDLE_WANDER_MAX = 40.0d;

    private CompanionFollowDistances() {
    }

    public static boolean tooClose(double distance) {
        return distance < MIN_PERSONAL_SPACE;
    }

    public static boolean inComfortBand(double distance) {
        return distance >= MIN_PERSONAL_SPACE && distance <= COMFORT_MAX;
    }

    public static boolean needsFollow(double distance) {
        return distance > FOLLOW_START;
    }
}
