package com.azscompanions.entity;

/**
 * Compatibility alias for {@link CompanionOrbSupport}.
 * Prefer {@link CompanionOrbSupport} in new code.
 */
public final class CompanionOrbSettings {
    public static final int DEFAULT_COLOR_RGB = CompanionOrbSupport.DEFAULT_COLOR_RGB;
    public static final int MIN_CHANNEL = CompanionOrbSupport.MIN_CHANNEL;
    public static final int MAX_CHANNEL = CompanionOrbSupport.MAX_CHANNEL;
    public static final int MIN_BRIGHTNESS = CompanionOrbSupport.MIN_BRIGHTNESS;
    public static final int MAX_BRIGHTNESS = CompanionOrbSupport.MAX_BRIGHTNESS;
    public static final int TORCH_BRIGHTNESS = CompanionOrbSupport.TORCH_BRIGHTNESS;
    public static final int DEFAULT_BRIGHTNESS = CompanionOrbSupport.DEFAULT_BRIGHTNESS;
    public static final float MIN_FLOAT_AMPLITUDE = CompanionOrbSupport.MIN_FLOAT_AMPLITUDE;
    public static final float MAX_FLOAT_AMPLITUDE = CompanionOrbSupport.MAX_FLOAT_AMPLITUDE;
    public static final float DEFAULT_FLOAT_AMPLITUDE = CompanionOrbSupport.DEFAULT_FLOAT_AMPLITUDE;
    public static final float MIN_FLOAT_SPEED = CompanionOrbSupport.MIN_FLOAT_SPEED;
    public static final float MAX_FLOAT_SPEED = CompanionOrbSupport.MAX_FLOAT_SPEED;
    public static final float DEFAULT_FLOAT_SPEED = CompanionOrbSupport.DEFAULT_FLOAT_SPEED;
    public static final float MIN_FLOAT_HEIGHT = CompanionOrbSupport.MIN_FLOAT_HEIGHT;
    public static final float MAX_FLOAT_HEIGHT = CompanionOrbSupport.MAX_FLOAT_HEIGHT;
    public static final float DEFAULT_FLOAT_HEIGHT = CompanionOrbSupport.DEFAULT_FLOAT_HEIGHT;
    public static final float MIN_OFFSET = CompanionOrbSupport.MIN_OFFSET;
    public static final float MAX_OFFSET = CompanionOrbSupport.MAX_OFFSET;
    public static final float DEFAULT_OFFSET_X = CompanionOrbSupport.DEFAULT_OFFSET_X;
    public static final float DEFAULT_OFFSET_Y = CompanionOrbSupport.DEFAULT_OFFSET_Y;
    public static final float DEFAULT_OFFSET_Z = CompanionOrbSupport.DEFAULT_OFFSET_Z;
    public static final float FRONT_STAND_OFF_Z = CompanionOrbSupport.FRONT_STAND_OFF_Z;
    public static final float BACK_STAND_OFF_Z = CompanionOrbSupport.BACK_STAND_OFF_Z;
    public static final boolean DEFAULT_FRONT = CompanionOrbSupport.DEFAULT_FRONT;

    public static final String NBT_COLOR = CompanionOrbSupport.NBT_COLOR;
    public static final String NBT_BRIGHTNESS = CompanionOrbSupport.NBT_BRIGHTNESS;
    public static final String NBT_FLOAT_AMPLITUDE = CompanionOrbSupport.NBT_FLOAT_AMPLITUDE;
    public static final String NBT_FLOAT_SPEED = CompanionOrbSupport.NBT_FLOAT_SPEED;
    public static final String NBT_FLOAT_HEIGHT = CompanionOrbSupport.NBT_FLOAT_HEIGHT;
    public static final String NBT_OFFSET_X = CompanionOrbSupport.NBT_OFFSET_X;
    public static final String NBT_OFFSET_Y = CompanionOrbSupport.NBT_OFFSET_Y;
    public static final String NBT_OFFSET_Z = CompanionOrbSupport.NBT_OFFSET_Z;
    public static final String NBT_FRONT = CompanionOrbSupport.NBT_FRONT;

    private CompanionOrbSettings() {
    }

    public static int clampChannel(int v) {
        return CompanionOrbSupport.clampChannel(v);
    }

    public static int clampRgb(int rgb) {
        return CompanionOrbSupport.clampRgb(rgb);
    }

    public static int rgb(int r, int g, int b) {
        return CompanionOrbSupport.rgb(r, g, b);
    }

    public static int red(int rgb) {
        return CompanionOrbSupport.red(rgb);
    }

    public static int green(int rgb) {
        return CompanionOrbSupport.green(rgb);
    }

    public static int blue(int rgb) {
        return CompanionOrbSupport.blue(rgb);
    }

    public static int clampBrightness(int v) {
        return CompanionOrbSupport.clampBrightness(v);
    }

    public static float clampFloatAmplitude(float v) {
        return CompanionOrbSupport.clampFloatAmplitude(v);
    }

    public static float clampFloatSpeed(float v) {
        return CompanionOrbSupport.clampFloatSpeed(v);
    }

    public static float clampFloatHeight(float v) {
        return CompanionOrbSupport.clampFloatHeight(v);
    }

    public static float clampOffset(float v) {
        return CompanionOrbSupport.clampOffset(v);
    }

    public static float effectiveOffsetZ(float offsetZ, boolean front) {
        return CompanionOrbSupport.effectiveOffsetZ(offsetZ, front);
    }

    public static float bobDeltaY(int tickAge, float partialTicks, float amplitude, float speed) {
        return CompanionOrbSupport.bobDeltaY(tickAge, partialTicks, amplitude, speed);
    }

    public static double[] worldOffsetFromLocal(
            float ownerYawDegrees, float offsetX, float offsetY, float offsetZ) {
        return CompanionOrbSupport.worldOffsetFromLocal(ownerYawDegrees, offsetX, offsetY, offsetZ);
    }

    public static int lightLuminance(boolean orbForm, int brightness) {
        return CompanionOrbSupport.lightLuminance(orbForm, brightness);
    }

    public static int lightLuminanceReflective(Object entity) {
        return CompanionOrbSupport.lightLuminanceReflective(entity);
    }
}
