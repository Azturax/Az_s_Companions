package com.azscompanions.entity;

/**
 * Pure swim-follow helpers shared by Fabric / NeoForge follow goals.
 * <p>
 * Loader code detects owner/companion water state and applies the returned velocity.
 */
public final class CompanionSwimFollowSupport {
    public static final double SWIM_SPEED_FAR = 0.58d;
    public static final double SWIM_SPEED_MID = 0.44d;
    public static final double SWIM_SPEED_NEAR = 0.30d;
    /** Soft station-keeping when already near the owner underwater. */
    public static final double HOLD_SPEED = 0.16d;
    /** Treat as arrived at swim target (blocks). */
    public static final double ARRIVE_EPSILON = 0.35d;

    private CompanionSwimFollowSupport() {
    }

    /**
     * Direct swim control only while both are wet; on shore, pathfinding + water malus should enter.
     */
    public static boolean useDirectSwimControl(boolean ownerInWater, boolean companionInWater) {
        return ownerInWater && companionInWater;
    }

    /**
     * Keep the follow goal running while the owner is wet and the companion is still on shore
     * (otherwise {@code followStop} can cancel before they enter the water).
     */
    public static boolean keepGoalWhileOwnerWet(boolean ownerInWater, boolean companionInWater) {
        return ownerInWater && !companionInWater;
    }

    public static double speedForDistance(double dist) {
        if (dist > 8.0d) {
            return SWIM_SPEED_FAR;
        }
        if (dist > 4.0d) {
            return SWIM_SPEED_MID;
        }
        if (dist > ARRIVE_EPSILON) {
            return SWIM_SPEED_NEAR;
        }
        return HOLD_SPEED;
    }

    /**
     * Preferred stand-off ring around the owner at the owner's swim depth (Y matched).
     *
     * @return {@code [tx, ty, tz]}
     */
    public static double[] preferredSwimTarget(
            double ownerX,
            double ownerY,
            double ownerZ,
            double companionX,
            double companionZ,
            double preferredDistance) {
        double dx = companionX - ownerX;
        double dz = companionZ - ownerZ;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        if (horiz < 1.0e-4d) {
            dx = 1.0d;
            dz = 0.0d;
            horiz = 1.0d;
        }
        double scale = preferredDistance / horiz;
        return new double[]{ownerX + dx * scale, ownerY, ownerZ + dz * scale};
    }

    /**
     * Velocity toward {@code (tx,ty,tz)} at {@code speed}. Zero if already arrived.
     *
     * @return {@code [vx, vy, vz]}
     */
    public static double[] velocityToward(
            double cx,
            double cy,
            double cz,
            double tx,
            double ty,
            double tz,
            double speed) {
        double dx = tx - cx;
        double dy = ty - cy;
        double dz = tz - cz;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < ARRIVE_EPSILON * 0.5d) {
            // Tiny residual depth correction so FloatGoal cannot yank them to the surface alone.
            if (Math.abs(dy) > 0.12d) {
                return new double[]{0.0d, clamp(dy * 0.25d, -HOLD_SPEED, HOLD_SPEED), 0.0d};
            }
            return new double[]{0.0d, 0.0d, 0.0d};
        }
        double s = speed / len;
        return new double[]{dx * s, dy * s, dz * s};
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
