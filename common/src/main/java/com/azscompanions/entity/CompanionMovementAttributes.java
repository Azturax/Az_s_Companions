package com.azscompanions.entity;

/**
 * Shared movement attributes so companions clear a full 1-block step at any body scale.
 * Used by NeoForge and Fabric {@code createAttributes()}.
 */
public final class CompanionMovementAttributes {
    /**
     * World-space max step-up (blocks). Vanilla default is 0.6 (slabs only).
     * 1.0 lets companions walk onto a full block regardless of {@code Attributes.SCALE}.
     */
    public static final double STEP_HEIGHT = 1.0d;

    /**
     * Vanilla player/mob jump strength (~1.25 blocks clearance). Explicit so scale changes
     * cannot leave companions without a registered jump attribute for pathfinding leaps.
     */
    public static final double JUMP_STRENGTH = 0.42d;

    private CompanionMovementAttributes() {
    }
}
