package com.azscompanions.entity;

/**
 * Shared companion follow / personal-space / home-bed bands (blocks).
 * Used by NeoForge and Fabric goals so both loaders stay in lockstep.
 * <p>
 * Defaults match the classic static bands; per-companion overrides live on the entity
 * ({@code FollowRadius}, {@code PersonalSpace}, {@code WanderRadius} NBT / synched data).
 */
public final class CompanionFollowDistances {
    /** Never stand closer than this to the owner (default personal space). */
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

    /** Follow / teleport leash slider: 1–128 blocks (default matches classic teleport). */
    public static final float FOLLOW_RADIUS_MIN = 1.0f;
    public static final float FOLLOW_RADIUS_MAX = 128.0f;
    public static final float DEFAULT_FOLLOW_RADIUS = (float) TELEPORT_DISTANCE;

    /** Personal space / stop-distance slider. */
    public static final float PERSONAL_SPACE_MIN = 1.0f;
    public static final float PERSONAL_SPACE_MAX = 12.0f;
    public static final float DEFAULT_PERSONAL_SPACE = (float) MIN_PERSONAL_SPACE;

    /** Wander free-roam radius slider (non–home-bed). */
    public static final float WANDER_RADIUS_MIN = 3.0f;
    public static final float WANDER_RADIUS_MAX = 48.0f;
    public static final float DEFAULT_WANDER_RADIUS = (float) IDLE_WANDER_MAX;

    /** Child inherit factor for spacing (slightly tighter than parent). */
    public static final float CHILD_SPACING_SCALE = 0.75f;

    private CompanionFollowDistances() {
    }

    public static float clampFollowRadius(float value) {
        return Math.max(FOLLOW_RADIUS_MIN, Math.min(FOLLOW_RADIUS_MAX, value));
    }

    public static float clampPersonalSpace(float value) {
        return Math.max(PERSONAL_SPACE_MIN, Math.min(PERSONAL_SPACE_MAX, value));
    }

    public static float clampWanderRadius(float value) {
        return Math.max(WANDER_RADIUS_MIN, Math.min(WANDER_RADIUS_MAX, value));
    }

    /** Inherit parent spacing with a slightly smaller radius (Bits cling closer). */
    public static float inheritFollowRadius(float parent) {
        return clampFollowRadius(parent * CHILD_SPACING_SCALE);
    }

    public static float inheritPersonalSpace(float parent) {
        return clampPersonalSpace(Math.max(PERSONAL_SPACE_MIN, parent * CHILD_SPACING_SCALE));
    }

    public static float inheritWanderRadius(float parent) {
        return clampWanderRadius(parent * CHILD_SPACING_SCALE);
    }

    /** Path-start distance derived from follow leash + personal space. */
    public static double followStart(double personalSpace, double followRadius) {
        double stop = followStop(personalSpace);
        double start = Math.max(stop + 0.5d, Math.min(FOLLOW_START, followRadius * 0.25d));
        // Always start pathing before the teleport leash (even on tiny radii).
        return Math.min(followRadius * 0.85d, start);
    }

    /** Stop closing once within this distance of the owner. */
    public static double followStop(double personalSpace) {
        return Math.max(personalSpace, Math.min(COMFORT_MAX, personalSpace + 3.0d));
    }

    /** Preferred stand-off ring between personal space and stop. */
    public static double preferredDistance(double personalSpace) {
        double stop = followStop(personalSpace);
        return (personalSpace + stop) * 0.5d;
    }

    /** Min distance before any teleport snap is allowed for this leash. */
    public static double minTeleportDistance(double followRadius) {
        return Math.min(MIN_TELEPORT_DISTANCE, followRadius);
    }

    public static boolean tooClose(double distance) {
        return tooClose(distance, MIN_PERSONAL_SPACE);
    }

    public static boolean tooClose(double distance, double personalSpace) {
        return distance < personalSpace;
    }

    public static boolean inComfortBand(double distance) {
        return inComfortBand(distance, MIN_PERSONAL_SPACE);
    }

    public static boolean inComfortBand(double distance, double personalSpace) {
        double comfortMax = Math.max(COMFORT_MAX, followStop(personalSpace) + 2.0d);
        return distance >= personalSpace && distance <= comfortMax;
    }

    public static boolean needsFollow(double distance) {
        return needsFollow(distance, MIN_PERSONAL_SPACE, TELEPORT_DISTANCE);
    }

    public static boolean needsFollow(double distance, double personalSpace, double followRadius) {
        return distance > followStart(personalSpace, followRadius);
    }

    public static boolean withinHomeBedRadius(double distance) {
        return distance <= HOME_BED_RADIUS;
    }

    public static boolean beyondHomeBedRadius(double distance) {
        return distance > HOME_BED_RADIUS;
    }

    /** True when a teleport-to-owner would be a short-range snap and must be skipped. */
    public static boolean tooCloseToTeleport(double distanceToOwner) {
        return tooCloseToTeleport(distanceToOwner, TELEPORT_DISTANCE);
    }

    public static boolean tooCloseToTeleport(double distanceToOwner, double followRadius) {
        return distanceToOwner < minTeleportDistance(followRadius);
    }

    /** True when Follow-mode ground leash teleport is allowed. */
    public static boolean shouldGroundTeleport(double distanceToOwner) {
        return shouldGroundTeleport(distanceToOwner, TELEPORT_DISTANCE);
    }

    public static boolean shouldGroundTeleport(double distanceToOwner, double followRadius) {
        return distanceToOwner >= followRadius;
    }
}
