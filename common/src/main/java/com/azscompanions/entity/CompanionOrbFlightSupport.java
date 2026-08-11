package com.azscompanions.entity;

/**
 * Pure always-air follow helpers for {@link CompanionForm#GLOWING_ORB}.
 * Uses {@link CompanionFlightFollowSupport} + {@link CompanionOrbSupport} offsets.
 */
public final class CompanionOrbFlightSupport {
    private CompanionOrbFlightSupport() {
    }

    /**
     * Preferred orb hover target beside the owner (personal-space ring + local X/Y/Z).
     *
     * @return {@code [tx, ty, tz]}
     */
    public static double[] preferredTarget(
            double ownerX,
            double ownerY,
            double ownerZ,
            float ownerYawDegrees,
            double companionX,
            double companionZ,
            double personalSpace,
            float floatHeight,
            float offsetX,
            float offsetY,
            float offsetZ
    ) {
        double preferred = CompanionFollowDistances.preferredDistance(personalSpace);
        double[] local = CompanionOrbSupport.worldOffsetFromLocal(ownerYawDegrees, offsetX, offsetY, offsetZ);
        return CompanionFlightFollowSupport.preferredFlightTarget(
                ownerX,
                ownerY,
                ownerZ,
                companionX,
                companionZ,
                preferred,
                CompanionOrbSupport.clampFloatHeight(floatHeight),
                local[0],
                local[1],
                local[2]);
    }

    /**
     * Motion toward preferred target with optional bob on Y.
     *
     * @return {@code [vx, vy, vz]}
     */
    public static double[] velocityWithBob(
            double cx,
            double cy,
            double cz,
            double tx,
            double ty,
            double tz,
            int tickAge,
            float floatAmplitude,
            float floatSpeed
    ) {
        double dist = Math.sqrt(
                (tx - cx) * (tx - cx) + (ty - cy) * (ty - cy) + (tz - cz) * (tz - cz));
        double speed = CompanionFlightFollowSupport.speedForDistance(dist);
        double[] v = CompanionFlightFollowSupport.velocityToward(cx, cy, cz, tx, ty, tz, speed);
        double bob = CompanionOrbSupport.bobDeltaY(tickAge, 0.0f, floatAmplitude, floatSpeed);
        return new double[]{v[0], v[1] + bob * 0.35d, v[2]};
    }
}
