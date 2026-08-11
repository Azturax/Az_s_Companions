package com.azscompanions.entity;

/**
 * Pure always-air follow helpers for {@link CompanionForm#GLOWING_ORB}.
 * Front/Back locks stand-off along owner look; XYZ offsets refine from that.
 */
public final class CompanionOrbFlightSupport {
    private CompanionOrbFlightSupport() {
    }

    /**
     * Preferred orb hover target beside the owner (defaults to back).
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
        return preferredTarget(
                ownerX, ownerY, ownerZ, ownerYawDegrees,
                companionX, companionZ, personalSpace, floatHeight,
                offsetX, offsetY, offsetZ, CompanionOrbSupport.DEFAULT_FRONT);
    }

    /**
     * Preferred orb hover target with Front/Back stand-off on look axis.
     *
     * @param front {@code true} = in front of owner, {@code false} = behind
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
            float offsetZ,
            boolean front
    ) {
        double preferred = CompanionFollowDistances.preferredDistance(personalSpace);
        float standOffZ = front ? (float) preferred : (float) -preferred;
        double[] local = CompanionOrbSupport.worldOffsetFromLocal(
                ownerYawDegrees, offsetX, offsetY, offsetZ + standOffZ);
        return new double[]{
                ownerX + local[0],
                ownerY + CompanionOrbSupport.clampFloatHeight(floatHeight) + local[1],
                ownerZ + local[2]
        };
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
