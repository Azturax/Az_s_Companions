package com.azscompanions.entity;

/**
 * Adult feminine body proportion multipliers for companion models.
 * Ranges are clamped so the silhouette cannot become child-like.
 * Overall height/size is separate ({@code bodyScale} 0.5–3.0, default 0.7).
 */
public final class CompanionBodyProportions {
    /** Bust / chest size. */
    public static final float MIN_BUST = 0.85f;
    public static final float MAX_BUST = 1.45f;
    public static final float DEFAULT_BUST = 1.15f;

    /** Waist width (lower = narrower). */
    public static final float MIN_WAIST = 0.80f;
    public static final float MAX_WAIST = 1.15f;
    public static final float DEFAULT_WAIST = 0.92f;

    /** Hip width. */
    public static final float MIN_HIPS = 0.95f;
    public static final float MAX_HIPS = 1.50f;
    public static final float DEFAULT_HIPS = 1.18f;

    /** Shoulder width. */
    public static final float MIN_SHOULDERS = 0.85f;
    public static final float MAX_SHOULDERS = 1.20f;
    public static final float DEFAULT_SHOULDERS = 0.95f;

    /** Forward bust offset (shape), model units. */
    public static final float MIN_BUST_OFFSET = 0.0f;
    public static final float MAX_BUST_OFFSET = 0.55f;
    public static final float DEFAULT_BUST_OFFSET = 0.22f;

    private CompanionBodyProportions() {
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clampBust(float v) {
        return clamp(v, MIN_BUST, MAX_BUST);
    }

    public static float clampWaist(float v) {
        return clamp(v, MIN_WAIST, MAX_WAIST);
    }

    public static float clampHips(float v) {
        return clamp(v, MIN_HIPS, MAX_HIPS);
    }

    public static float clampShoulders(float v) {
        return clamp(v, MIN_SHOULDERS, MAX_SHOULDERS);
    }

    public static float clampBustOffset(float v) {
        return clamp(v, MIN_BUST_OFFSET, MAX_BUST_OFFSET);
    }
}
