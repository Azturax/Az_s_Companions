package com.azscompanions.entity;

/**
 * Safe stand-off teleports: never land inside the owner's hitbox or a guessed block.
 * Loader code still checks world blocks; this supplies candidate offsets.
 */
public final class CompanionSafeTeleportSupport {
    /** Brief vanilla {@code invulnerableTime} after a snap so post-teleport ticks cannot kill. */
    public static final int POST_TELEPORT_INVULN_TICKS = 60;

    /** Minimum horizontal stand-off from the owner's feet (not inside their AABB). */
    public static final double MIN_OWNER_SEPARATION = 2.0d;

    public static final int[] Y_OFFSETS = {0, 1, -1, 2, -2, 3, 4};

    private CompanionSafeTeleportSupport() {
    }

    /**
     * Horizontal (dx, dz) candidates around the target, starting at {@code preferred} then
     * tighter rings so a cave/house still finds a standable block. Never includes (0, 0).
     */
    public static int[][] horizontalOffsets(double preferred) {
        int ring = Math.max(2, (int) Math.round(preferred));
        int inner = Math.max(2, ring - 2);
        return new int[][]{
                {ring, 0}, {-ring, 0}, {0, ring}, {0, -ring},
                {ring, ring}, {ring, -ring}, {-ring, ring}, {-ring, -ring},
                {inner, 0}, {-inner, 0}, {0, inner}, {0, -inner},
                {2, 0}, {-2, 0}, {0, 2}, {0, -2},
                {2, 2}, {2, -2}, {-2, 2}, {-2, -2}
        };
    }

    /**
     * Offset beside/behind the owner using look yaw (Minecraft degrees).
     *
     * @return {@code [dx, dz]} at {@code distance} behind the look vector
     */
    public static double[] behindOwner(float yawDegrees, double distance) {
        double dist = Math.max(MIN_OWNER_SEPARATION, distance);
        double yaw = Math.toRadians(yawDegrees);
        // Minecraft look XZ: (-sin(yaw), cos(yaw)); behind is the opposite.
        return new double[]{Math.sin(yaw) * dist, -Math.cos(yaw) * dist};
    }

    public static boolean tooCloseToOwner(double dx, double dz, double minSeparation) {
        return dx * dx + dz * dz < minSeparation * minSeparation;
    }
}
