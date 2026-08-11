package com.azscompanions.entity;

/**
 * Pure ride-along helpers shared by Fabric / NeoForge.
 * <p>
 * When the owner mounts a rideable, following companions try to befriend/mount a
 * matching nearby empty vehicle — never the owner's current vehicle.
 */
public final class CompanionRideAlongSupport {
    /** Horizontal search for a matching mount (blocks). */
    public static final double SEARCH_RANGE = 16.0d;
    /** Mount when within this distance of the candidate. */
    public static final double MOUNT_REACH = 2.75d;
    /** After a failed / empty search, wait before trying again. */
    public static final int FAIL_COOLDOWN_TICKS = 80;
    /** Soft repath cadence while approaching a candidate. */
    public static final int APPROACH_REPATH_TICKS = 10;
    /** Cruise speed factor while steering a mounted vehicle toward the owner. */
    public static final double STEER_SPEED = 0.35d;
    public static final double STEER_SPEED_FAR = 0.55d;

    /**
     * Sensible matching buckets for owner vehicle → companion candidate.
     * Horse family (horse / donkey / mule / undead horses) share {@link #HORSE}.
     */
    public enum RideKind {
        NONE,
        HORSE,
        CAMEL,
        LLAMA,
        BOAT,
        MINECART,
        PIG,
        STRIDER
    }

    private CompanionRideAlongSupport() {
    }

    public static boolean kindsMatch(RideKind ownerKind, RideKind candidateKind) {
        return ownerKind != null
                && candidateKind != null
                && ownerKind != RideKind.NONE
                && ownerKind == candidateKind;
    }

    /**
     * Seek mounts only while following (or wander rescue follow) and the owner is riding.
     */
    public static boolean shouldSeek(
            boolean activelyFollowing,
            boolean ownerIsPassenger,
            boolean companionSleeping,
            boolean companionInCombat) {
        return activelyFollowing && ownerIsPassenger && !companionSleeping && !companionInCombat;
    }

    /** Dismount when this feature mounted us and the owner is no longer riding. */
    public static boolean shouldSyncDismount(boolean rideAlongActive, boolean ownerIsPassenger) {
        return rideAlongActive && !ownerIsPassenger;
    }

    /**
     * Empty, not the owner's vehicle, and not owned by someone else.
     */
    public static boolean isPreferredCandidate(
            boolean empty,
            boolean isOwnerVehicle,
            boolean ownedByOtherPlayer) {
        return empty && !isOwnerVehicle && !ownedByOtherPlayer;
    }

    public static boolean withinMountReach(double distanceSq) {
        return distanceSq <= MOUNT_REACH * MOUNT_REACH;
    }

    public static boolean withinSearchRange(double distanceSq) {
        return distanceSq <= SEARCH_RANGE * SEARCH_RANGE;
    }

    public static boolean canAttempt(long gameTime, long cooldownUntilGameTime) {
        return gameTime >= cooldownUntilGameTime;
    }

    public static long nextFailCooldown(long gameTime) {
        return gameTime + FAIL_COOLDOWN_TICKS;
    }

    public static double steerSpeedForDistance(double distToOwner) {
        return distToOwner > 8.0d ? STEER_SPEED_FAR : STEER_SPEED;
    }

    /**
     * Velocity toward the owner's stand-off ring (Y matched for land vehicles).
     *
     * @return {@code [vx, vy, vz]}
     */
    public static double[] steerVelocity(
            double vehicleX,
            double vehicleY,
            double vehicleZ,
            double ownerX,
            double ownerY,
            double ownerZ,
            double preferredDistance,
            double speed) {
        double dx = vehicleX - ownerX;
        double dz = vehicleZ - ownerZ;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        double tx;
        double tz;
        if (horiz < 1.0e-4d) {
            tx = ownerX + preferredDistance;
            tz = ownerZ;
        } else {
            double scale = preferredDistance / horiz;
            tx = ownerX + dx * scale;
            tz = ownerZ + dz * scale;
        }
        double ty = ownerY;
        double mx = tx - vehicleX;
        double my = ty - vehicleY;
        double mz = tz - vehicleZ;
        double len = Math.sqrt(mx * mx + my * my + mz * mz);
        if (len < 0.35d) {
            return new double[]{0.0d, 0.0d, 0.0d};
        }
        double s = speed / len;
        return new double[]{mx * s, clamp(my * s, -0.25d, 0.25d), mz * s};
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
