package com.azscompanions.entity;

/**
 * Pure flight-follow / air-wander helpers shared by Fabric / NeoForge.
 * <p>
 * Mirrors {@link CompanionSwimFollowSupport}: station-keep on a personal-space ring
 * around the owner instead of bee-lining into their hitbox.
 */
public final class CompanionFlightFollowSupport {
    /** Soft hover offset above the owner's feet while flying. */
    public static final double HOVER_Y = 0.35d;
    public static final double FLIGHT_SPEED_FAR = 0.72d;
    public static final double FLIGHT_SPEED_MID = 0.52d;
    public static final double FLIGHT_SPEED_NEAR = 0.38d;
    /** Soft station-keeping when already on the preferred ring. */
    public static final double HOLD_SPEED = 0.16d;
    /** Treat as arrived at flight target (blocks). */
    public static final double ARRIVE_EPSILON = 0.45d;
    /** Leisurely air-wander cruise speed. */
    public static final double WANDER_SPEED = 0.42d;
    /** Vertical jitter range for air wander picks (blocks). */
    public static final double WANDER_Y_RANGE = 2.5d;

    private CompanionFlightFollowSupport() {
    }

    /**
     * Preferred stand-off ring around the owner at hover height (Y matched to owner + hover).
     *
     * @return {@code [tx, ty, tz]}
     */
    public static double[] preferredFlightTarget(
            double ownerX,
            double ownerY,
            double ownerZ,
            double companionX,
            double companionZ,
            double preferredDistance) {
        return preferredFlightTarget(
                ownerX, ownerY, ownerZ, companionX, companionZ, preferredDistance, HOVER_Y, 0.0d, 0.0d, 0.0d);
    }

    /**
     * Preferred stand-off with custom hover height and optional world-space offset
     * (e.g. orb X/Y/Z after converting from owner-local).
     *
     * @return {@code [tx, ty, tz]}
     */
    public static double[] preferredFlightTarget(
            double ownerX,
            double ownerY,
            double ownerZ,
            double companionX,
            double companionZ,
            double preferredDistance,
            double hoverY,
            double worldOffsetX,
            double worldOffsetY,
            double worldOffsetZ) {
        double baseX = ownerX + worldOffsetX;
        double baseY = ownerY + hoverY + worldOffsetY;
        double baseZ = ownerZ + worldOffsetZ;
        double dx = companionX - baseX;
        double dz = companionZ - baseZ;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        if (horiz < 1.0e-4d) {
            dx = 1.0d;
            dz = 0.0d;
            horiz = 1.0d;
        }
        double scale = preferredDistance / horiz;
        return new double[]{baseX + dx * scale, baseY, baseZ + dz * scale};
    }

    public static double speedForDistance(double distToTarget) {
        if (distToTarget > 8.0d) {
            return FLIGHT_SPEED_FAR;
        }
        if (distToTarget > 4.0d) {
            return FLIGHT_SPEED_MID;
        }
        if (distToTarget > ARRIVE_EPSILON) {
            return FLIGHT_SPEED_NEAR;
        }
        return HOLD_SPEED;
    }

    /**
     * Velocity toward {@code (tx,ty,tz)} at {@code speed}. Soft hold if already arrived.
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
            if (Math.abs(dy) > 0.12d) {
                return new double[]{0.0d, clamp(dy * 0.22d, -HOLD_SPEED, HOLD_SPEED), 0.0d};
            }
            return new double[]{0.0d, 0.0d, 0.0d};
        }
        double s = speed / len;
        return new double[]{dx * s, dy * s, dz * s};
    }

    /** Damped hover hold (pause / arrived). */
    public static double[] holdVelocity(double currentVx, double currentVy, double currentVz, double targetY, double cy) {
        double yDelta = targetY - cy;
        double vy = Math.abs(yDelta) > 0.2d
                ? clamp(yDelta * 0.22d, -0.4d, 0.4d)
                : currentVy * 0.55d;
        return new double[]{currentVx * 0.55d, vy, currentVz * 0.55d};
    }

    /**
     * Far-leash snap while flying — never under personal space.
     * Uses the follow teleport leash (or a modest flight floor).
     */
    public static boolean shouldFlightSnap(double distanceToOwner, double teleportDistance) {
        double leash = Math.max(12.0d, Math.min(teleportDistance, CompanionFollowDistances.TELEPORT_DISTANCE));
        return distanceToOwner >= leash;
    }

    /**
     * Air-wander pick: horizontal ring outside personal space, with light Y jitter.
     *
     * @return {@code [tx, ty, tz]} or {@code null} if radii are invalid
     */
    public static double[] pickAirWanderTarget(
            double centerX,
            double centerY,
            double centerZ,
            double personalSpace,
            double minRadius,
            double maxRadius,
            double angleRadians,
            double radiusUnit,
            double yUnit) {
        double minR = Math.max(minRadius, personalSpace);
        double maxR = Math.max(minR + 0.5d, maxRadius);
        double radius = minR + clamp(radiusUnit, 0.0d, 1.0d) * (maxR - minR);
        double y = centerY + HOVER_Y + (clamp(yUnit, 0.0d, 1.0d) * 2.0d - 1.0d) * WANDER_Y_RANGE;
        return new double[]{
                centerX + Math.cos(angleRadians) * radius,
                y,
                centerZ + Math.sin(angleRadians) * radius
        };
    }

    /** 3D distance check for air wander leash (includes vertical). */
    public static boolean beyondAirWanderRadius(
            double cx,
            double cy,
            double cz,
            double centerX,
            double centerY,
            double centerZ,
            double maxRadius) {
        double dx = cx - centerX;
        double dy = cy - (centerY + HOVER_Y);
        double dz = cz - centerZ;
        double limit = maxRadius + 1.5d;
        return (dx * dx + dy * dy + dz * dz) > limit * limit;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
