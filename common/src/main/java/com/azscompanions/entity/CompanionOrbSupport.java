package com.azscompanions.entity;

/**
 * Shared clamps / defaults for {@link CompanionForm#GLOWING_ORB} appearance and follow stand-off.
 * Values live on the companion entity (NBT + synched data).
 */
public final class CompanionOrbSupport {
    /** Packed RGB default — warm white glow. */
    public static final int DEFAULT_COLOR_RGB = 0xFFF5E6;
    public static final int MIN_CHANNEL = 0;
    public static final int MAX_CHANNEL = 255;

    /** Dynamic-lights luminance (block-light scale 0–15). */
    public static final int MIN_BRIGHTNESS = 0;
    public static final int MAX_BRIGHTNESS = 15;
    public static final int DEFAULT_BRIGHTNESS = 10;

    /** Vertical bob amplitude (blocks). */
    public static final float MIN_FLOAT_AMPLITUDE = 0.0f;
    public static final float MAX_FLOAT_AMPLITUDE = 0.75f;
    public static final float DEFAULT_FLOAT_AMPLITUDE = 0.18f;

    /** Bob cycles per second (approx). */
    public static final float MIN_FLOAT_SPEED = 0.05f;
    public static final float MAX_FLOAT_SPEED = 2.5f;
    public static final float DEFAULT_FLOAT_SPEED = 0.55f;

    /** Base hover height above owner feet before bob (blocks). */
    public static final float MIN_FLOAT_HEIGHT = 0.0f;
    public static final float MAX_FLOAT_HEIGHT = 4.0f;
    public static final float DEFAULT_FLOAT_HEIGHT = 1.15f;

    /** Follow stand-off in owner-local space (blocks). X=right, Y=up, Z=forward. */
    public static final float MIN_OFFSET = -8.0f;
    public static final float MAX_OFFSET = 8.0f;
    public static final float DEFAULT_OFFSET_X = 0.0f;
    public static final float DEFAULT_OFFSET_Y = 0.0f;
    public static final float DEFAULT_OFFSET_Z = 0.0f;

    public static final String NBT_COLOR = "OrbColor";
    public static final String NBT_BRIGHTNESS = "OrbBrightness";
    public static final String NBT_FLOAT_AMPLITUDE = "OrbFloatAmplitude";
    public static final String NBT_FLOAT_SPEED = "OrbFloatSpeed";
    public static final String NBT_FLOAT_HEIGHT = "OrbFloatHeight";
    public static final String NBT_OFFSET_X = "OrbOffsetX";
    public static final String NBT_OFFSET_Y = "OrbOffsetY";
    public static final String NBT_OFFSET_Z = "OrbOffsetZ";

    private CompanionOrbSupport() {
    }

    public static int clampChannel(int v) {
        return Math.max(MIN_CHANNEL, Math.min(MAX_CHANNEL, v));
    }

    public static int clampRgb(int rgb) {
        int r = clampChannel((rgb >> 16) & 0xFF);
        int g = clampChannel((rgb >> 8) & 0xFF);
        int b = clampChannel(rgb & 0xFF);
        return (r << 16) | (g << 8) | b;
    }

    public static int rgb(int r, int g, int b) {
        return (clampChannel(r) << 16) | (clampChannel(g) << 8) | clampChannel(b);
    }

    public static int red(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    public static int green(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    public static int blue(int rgb) {
        return rgb & 0xFF;
    }

    public static int clampBrightness(int v) {
        return Math.max(MIN_BRIGHTNESS, Math.min(MAX_BRIGHTNESS, v));
    }

    public static float clampFloatAmplitude(float v) {
        return clamp(v, MIN_FLOAT_AMPLITUDE, MAX_FLOAT_AMPLITUDE);
    }

    public static float clampFloatSpeed(float v) {
        return clamp(v, MIN_FLOAT_SPEED, MAX_FLOAT_SPEED);
    }

    public static float clampFloatHeight(float v) {
        return clamp(v, MIN_FLOAT_HEIGHT, MAX_FLOAT_HEIGHT);
    }

    public static float clampOffset(float v) {
        return clamp(v, MIN_OFFSET, MAX_OFFSET);
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Vertical bob delta for render / motion (blocks).
     *
     * @param tickAge entity age in ticks
     * @param partialTicks render partial
     */
    public static float bobDeltaY(int tickAge, float partialTicks, float amplitude, float speed) {
        float amp = clampFloatAmplitude(amplitude);
        if (amp <= 0.0f) {
            return 0.0f;
        }
        float spd = clampFloatSpeed(speed);
        float t = (tickAge + partialTicks) * spd * ((float) Math.PI * 2.0f / 20.0f);
        return (float) Math.sin(t) * amp;
    }

    /**
     * Owner-local stand-off: X right, Y up, Z forward of owner yaw (degrees).
     *
     * @return {@code [wx, wy, wz]} world offset
     */
    public static double[] worldOffsetFromLocal(
            float ownerYawDegrees,
            float offsetX,
            float offsetY,
            float offsetZ
    ) {
        double yaw = Math.toRadians(ownerYawDegrees);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double ox = clampOffset(offsetX);
        double oy = clampOffset(offsetY);
        double oz = clampOffset(offsetZ);
        // Minecraft: yaw 0 looks south (+Z); right is −X when facing +Z.
        double wx = ox * cos - oz * sin;
        double wz = ox * sin + oz * cos;
        return new double[]{wx, oy, wz};
    }

    /** Dynamic-lights luminance for an orb form (0 when not an orb / brightness off). */
    public static int lightLuminance(boolean orbForm, int brightness) {
        if (!orbForm) {
            return 0;
        }
        return clampBrightness(brightness);
    }

    /**
     * Reflection-safe luminance for dynamic-light handlers (no Minecraft types in signature).
     * Looks for {@code getForm().isOrb()} + {@code getOrbBrightness()} on the entity.
     */
    public static int lightLuminanceReflective(Object entity) {
        if (entity == null) {
            return 0;
        }
        try {
            Object form = entity.getClass().getMethod("getForm").invoke(entity);
            if (form == null) {
                return 0;
            }
            Object isOrb = form.getClass().getMethod("isOrb").invoke(form);
            if (!(isOrb instanceof Boolean orb) || !orb) {
                return 0;
            }
            Object brightness = entity.getClass().getMethod("getOrbBrightness").invoke(entity);
            if (brightness instanceof Number n) {
                return clampBrightness(n.intValue());
            }
        } catch (ReflectiveOperationException ignored) {
            return 0;
        }
        return 0;
    }
}
