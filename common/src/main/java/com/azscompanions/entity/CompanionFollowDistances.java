package com.azscompanions.entity;

/**
 * Shared companion follow / personal-space / home-bed bands (blocks).
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
    public static final double FOLLOW_START = 10.0d;
    /** Stop closing the gap once within this distance (≥ personal space). */
    public static final double FOLLOW_STOP = 5.0d;
    /** Ground teleport leash while exploring in Follow mode (never while idle / fighting / Wander stroll). */
    public static final double TELEPORT_DISTANCE = 48.0d;
    /**
     * Hard floor for any teleport-to-owner. Closer than this → walk/path only (never snap).
     * Prevents the short-range “bounce” while wandering near the player.
     */
    public static final double MIN_TELEPORT_DISTANCE = 24.0d;
    /**
     * Home-bed proximity (blocks). While the companion is within this of her home bed and the
     * owner is also within this of the bed, Follow mode stays home-idle instead of glued follow.
     * If the owner moves farther than this from the bed, the companion may teleport (only if also
     * beyond {@link #MIN_TELEPORT_DISTANCE} from the owner).
     */
    public static final double HOME_BED_RADIUS = 35.0d;
    /** Soft stroll ring around the home bed while home-idle (Happy Ghast–leisurely). */
    public static final double HOME_IDLE_WANDER_MIN = 2.0d;
    public static final double HOME_IDLE_WANDER_MAX = 10.0d;
    /** Explicit WANDER mode free-roam ring (loaded chunks only; stroll, never teleport). */
    public static final double IDLE_WANDER_MIN = 3.0d;
    public static final double IDLE_WANDER_MAX = 16.0d;
    /** Wake / leave bed when owner is farther than this while companion sleeps. */
    public static final double LEAVE_BED_OWNER_DISTANCE = 35.0d;

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

    public static boolean withinHomeBedRadius(double distance) {
        return distance <= HOME_BED_RADIUS;
    }

    public static boolean beyondHomeBedRadius(double distance) {
        return distance > HOME_BED_RADIUS;
    }

    /** True when a teleport-to-owner would be a short-range snap and must be skipped. */
    public static boolean tooCloseToTeleport(double distanceToOwner) {
        return distanceToOwner < MIN_TELEPORT_DISTANCE;
    }

    /** True when Follow-mode ground leash teleport is allowed. */
    public static boolean shouldGroundTeleport(double distanceToOwner) {
        return distanceToOwner >= TELEPORT_DISTANCE;
    }
}
