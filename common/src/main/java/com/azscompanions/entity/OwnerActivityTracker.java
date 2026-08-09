package com.azscompanions.entity;

/**
 * Tracks whether an owner is exploring (moving) or standing around (idle).
 * Pure math — no Minecraft dependency — so NeoForge and Fabric can share it.
 *
 * <p>Idle = little horizontal movement for {@link #IDLE_TICKS} (~2.5s).
 * Exploring = recent meaningful horizontal displacement.
 */
public final class OwnerActivityTracker {
    /** Ticks of low movement before the owner counts as standing around (~2.5s). */
    public static final int IDLE_TICKS = 50;

    /**
     * Squared horizontal displacement per tick below which the owner is treated as still.
     * ~0.04 blocks/tick (~0.8 b/s) — below sneak walk, above tiny float noise.
     */
    public static final double STILL_MOVE_THRESHOLD_SQ = 0.0016d;

    private double lastX;
    private double lastZ;
    private int stillTicks;
    private boolean initialized;

    public void reset() {
        initialized = false;
        stillTicks = 0;
        lastX = 0.0d;
        lastZ = 0.0d;
    }

    /**
     * Sample the owner's horizontal position once per server tick.
     */
    public void tick(double x, double z) {
        if (!initialized) {
            lastX = x;
            lastZ = z;
            stillTicks = 0;
            initialized = true;
            return;
        }
        double dx = x - lastX;
        double dz = z - lastZ;
        lastX = x;
        lastZ = z;
        if ((dx * dx) + (dz * dz) < STILL_MOVE_THRESHOLD_SQ) {
            if (stillTicks < IDLE_TICKS + 20) {
                stillTicks++;
            }
        } else {
            stillTicks = 0;
        }
    }

    /** Owner has been mostly still for {@link #IDLE_TICKS}. */
    public boolean isStandingAround() {
        return initialized && stillTicks >= IDLE_TICKS;
    }

    /** Owner is moving meaningfully, or has not been still long enough yet. */
    public boolean isExploring() {
        return !isStandingAround();
    }

    public int stillTicks() {
        return stillTicks;
    }
}
