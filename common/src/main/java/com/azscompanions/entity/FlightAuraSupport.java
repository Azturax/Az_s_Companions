package com.azscompanions.entity;

/**
 * Dragon Ball–style flight ki aura + motion-trail helpers (pure; no Minecraft types).
 * <p>
 * UX: trails and afterimages stay at <strong>foot / cloud height</strong> — never rising
 * particle columns into first-person view. Soft shell is low (ankles–thighs), not head-level.
 */
public final class FlightAuraSupport {
    /** Warm gold/orange ki (default). */
    public static final int DEFAULT_KI_RGB = 0xFFB030;
    /** Soft yellow cloud/ki for Jindujun trails. */
    public static final int DEFAULT_NIMBUS_RGB = 0xFFE566;

    /** Max afterimage samples kept per flyer. */
    public static final int TRAIL_LENGTH = 8;
    /** Min horizontal speed (blocks/tick) before trails densify. */
    public static final double TRAIL_SPEED_THRESHOLD = 0.08d;
    /** Foot-level Y offset above entity feet / cloud top for trail samples. */
    public static final float TRAIL_Y_OFFSET = 0.06f;
    /** Soft shell center above feet (below waist — FP-safe). */
    public static final float SHELL_Y_OFFSET = 0.42f;
    /** Soft shell half-size scale vs body. */
    public static final float SHELL_HALF_SIZE = 0.55f;
    /** Cap vertical placement of shell relative to entity height (never near head). */
    public static final float MAX_SHELL_HEIGHT_FRACTION = 0.35f;

    private FlightAuraSupport() {
    }

    /**
     * Whether to draw flight aura / trails.
     *
     * @param activelyFlying creative/survival flight, elytra, companion air-follow, nimbus move
     * @param onGround       grounded
     * @param inFluid        water/lava — suppress
     * @param ridingNimbus   player on Jindujun: body shell off (cloud trails only)
     */
    public static boolean shouldShowAura(
            boolean activelyFlying,
            boolean onGround,
            boolean inFluid,
            boolean ridingNimbus) {
        if (inFluid || onGround || !activelyFlying) {
            return false;
        }
        return !ridingNimbus;
    }

    /** Trails on the mount itself while moving. */
    public static boolean shouldShowNimbusTrail(boolean hasPassenger, boolean movingFastEnough) {
        return hasPassenger && movingFastEnough;
    }

    public static boolean movingFastEnough(double dx, double dy, double dz) {
        return speedSq(dx, dy, dz) >= TRAIL_SPEED_THRESHOLD * TRAIL_SPEED_THRESHOLD;
    }

    public static double speedSq(double dx, double dy, double dz) {
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Prefer orb tint, else team nametag tint, else default ki.
     *
     * @param orbRgb   packed RGB or {@code -1} if none
     * @param teamId   optional team id
     */
    public static int resolveColorRgb(int orbRgb, String teamId) {
        if (orbRgb >= 0) {
            return CompanionOrbSupport.clampRgb(orbRgb);
        }
        if (teamId != null && !teamId.isBlank()) {
            int team = CompanionTeamColors.nametagRgb(teamId);
            if (team != 0xFFFFFF) {
                return CompanionOrbSupport.clampRgb(team);
            }
        }
        return DEFAULT_KI_RGB;
    }

    public static int resolveNimbusTrailRgb(String teamId) {
        if (teamId != null && !teamId.isBlank()) {
            int team = CompanionTeamColors.nametagRgb(teamId);
            if (team != 0xFFFFFF) {
                return warmTowardGold(CompanionOrbSupport.clampRgb(team));
            }
        }
        return DEFAULT_NIMBUS_RGB;
    }

    /** Blend a team tint toward gold so trails stay ki-like. */
    public static int warmTowardGold(int rgb) {
        int r = CompanionOrbSupport.red(rgb);
        int g = CompanionOrbSupport.green(rgb);
        int b = CompanionOrbSupport.blue(rgb);
        int gr = CompanionOrbSupport.red(DEFAULT_NIMBUS_RGB);
        int gg = CompanionOrbSupport.green(DEFAULT_NIMBUS_RGB);
        int gb = CompanionOrbSupport.blue(DEFAULT_NIMBUS_RGB);
        return CompanionOrbSupport.rgb((r + gr) / 2, (g + gg) / 2, (b + gb) / 2);
    }

    /** Subtle pulse while ascending (boost) — scale multiplier ~1.0–1.08. */
    public static float pulseScale(int tickAge, float partialTicks, boolean ascending) {
        float t = (tickAge + partialTicks) * 0.35f;
        float wave = 1.0f + 0.04f * (float) Math.sin(t);
        if (ascending) {
            wave += 0.04f;
        }
        return wave;
    }

    /**
     * Shell Y above feet, clamped so it never sits near the head.
     *
     * @param entityHeight living height in blocks
     */
    public static float shellOffsetY(float entityHeight) {
        float h = Math.max(0.5f, entityHeight);
        float preferred = SHELL_Y_OFFSET;
        float cap = h * MAX_SHELL_HEIGHT_FRACTION;
        return Math.min(preferred, cap);
    }

    /** Afterimage half-size for trail index {@code 0 = newest}. */
    public static float trailHalfSize(int indexFromNewest, float bodyScale) {
        float base = 0.28f * Math.max(0.35f, bodyScale);
        float fade = 1.0f - (indexFromNewest / (float) Math.max(1, TRAIL_LENGTH));
        return base * (0.55f + 0.45f * fade);
    }

    /** Alpha 0–255 for trail afterimage ({@code 0 = newest}). */
    public static int trailAlpha(int indexFromNewest) {
        float fade = 1.0f - (indexFromNewest / (float) Math.max(1, TRAIL_LENGTH));
        return Math.max(40, Math.min(220, (int) (200 * fade)));
    }

    /**
     * First-person local player: no enveloping shell (blocks view); trails OK at feet.
     *
     * @return {@code true} if soft body shell should draw
     */
    public static boolean shouldDrawBodyShell(boolean firstPersonLocal) {
        return !firstPersonLocal;
    }
}
