package com.azscompanions.entity;

/**
 * Pure helpers for Glowing Orb playful-evil lightning (no Minecraft types).
 * <p>
 * Most strikes scatter near the orb; after a short grace period a low-probability
 * roll may place a bolt on / very near the owner — scary but not spam-lethal.
 */
public final class CompanionOrbEvilLightningSupport {
    /** Bolts when evil mode starts. */
    public static final int ENTER_BOLTS = 3;
    /** Bolts per periodic pulse (usually nearby; maybe one player-aimed). */
    public static final int PERIODIC_BOLTS = 1;
    /** Ticks between periodic lightning pulses while evil. */
    public static final int PERIODIC_INTERVAL_TICKS = 45;
    /** Horizontal scatter radius around the orb (blocks). */
    public static final double NEARBY_RADIUS = 5.5d;
    /** How close a “player strike” lands to the owner (blocks). */
    public static final double PLAYER_NEAR_RADIUS = 1.25d;
    /** Evil ticks before a player-aimed strike is allowed (~3s). */
    public static final int PLAYER_STRIKE_GRACE_TICKS = 60;
    /**
     * Chance per periodic pulse (after grace) that the bolt targets the owner.
     * Keep low — atmospheric threat, not a kill loop.
     */
    public static final double PLAYER_STRIKE_CHANCE = 0.14d;

    private CompanionOrbEvilLightningSupport() {
    }

    /** Whether this countdown tick should fire a periodic lightning pulse. */
    public static boolean shouldPeriodicPulse(int playfulEvilTicksRemaining) {
        return playfulEvilTicksRemaining > 0
                && playfulEvilTicksRemaining % PERIODIC_INTERVAL_TICKS == 0;
    }

    /**
     * Ticks already spent in the current evil burst (approximate from remaining + duration).
     *
     * @param durationTicks total duration set at activate
     * @param remainingTicks countdown left
     */
    public static int elapsedEvilTicks(int durationTicks, int remainingTicks) {
        return Math.max(0, durationTicks - remainingTicks);
    }

    /**
     * After grace, {@code random01} in {@code [0,1)} decides a rare owner-aimed strike.
     */
    public static boolean shouldTargetPlayer(int elapsedEvilTicks, double random01) {
        if (elapsedEvilTicks < PLAYER_STRIKE_GRACE_TICKS) {
            return false;
        }
        return random01 >= 0.0d && random01 < PLAYER_STRIKE_CHANCE;
    }

    /**
     * Horizontal offset for a nearby atmospheric bolt.
     *
     * @return {@code [dx, dz]}
     */
    public static double[] nearbyOffset(long seed, double radius) {
        double r = Math.max(0.5d, radius);
        // Deterministic polar sample from seed bits.
        long h = seed * 6364136223846793005L + 1L;
        double angle = ((h >>> 33) & 0x7fffffffL) / (double) 0x7fffffffL * Math.PI * 2.0d;
        double dist = Math.sqrt(((h >>> 11) & 0xffffL) / 65535.0d) * r;
        return new double[]{Math.cos(angle) * dist, Math.sin(angle) * dist};
    }

    /**
     * Offset for a player-aimed bolt (very close, not always exact feet).
     *
     * @return {@code [dx, dz]}
     */
    public static double[] playerNearOffset(long seed) {
        return nearbyOffset(seed ^ 0x9E3779B97F4A7C15L, PLAYER_NEAR_RADIUS);
    }
}
