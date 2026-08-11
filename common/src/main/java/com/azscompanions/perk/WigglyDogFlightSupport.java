package com.azscompanions.perk;

/**
 * Pure helpers for toggle-dog flight/wiggle timing (no Minecraft types).
 * Flight follow itself lives in loader {@code SpecialPlayerPerks} / perk tick code.
 */
public final class WigglyDogFlightSupport {
    /** Soft hover offset above the owner's feet while flying (matches companion flight). */
    public static final double FLIGHT_HOVER_Y = 0.35d;
    /** Max distance before a snap teleport while both are flying. */
    public static final double FLIGHT_KEEP_RADIUS = 5.0d;
    /** Playful vertical bob amplitude while floating. */
    public static final double WIGGLE_BOB_AMP = 0.045d;

    private WigglyDogFlightSupport() {
    }

    /** Sin-wave bob for playful float (tick-based). */
    public static double bobDeltaY(int ageTicks) {
        return Math.sin(ageTicks * 0.35d) * WIGGLE_BOB_AMP;
    }

    /** True on frames that should flip sit/stand for grounded wiggle. */
    public static boolean shouldFlipSit(int ageTicks) {
        return ageTicks % 90 == 0;
    }
}
