package com.azscompanions.ai;

/**
 * Client-side cache for the top-right companion AI thinking HUD
 * (spinning gear + Thinking… + progress bar). Updated by S2C packets.
 */
public final class ClientCompanionAiHud {
    private static volatile boolean active;
    private static volatile String companionName = "";
    private static volatile long startedAtMs;
    private static volatile int timeoutSeconds = 30;
    /** Server-provided fraction in {@code [0,1]}, or {@code <0} to estimate locally. */
    private static volatile float serverProgress = -1f;

    private ClientCompanionAiHud() {
    }

    public static void apply(boolean thinking, String name, int timeoutSec, float progress) {
        if (!thinking) {
            clear();
            return;
        }
        active = true;
        companionName = name == null || name.isBlank() ? "Companion" : name.trim();
        startedAtMs = System.currentTimeMillis();
        timeoutSeconds = Math.max(5, Math.min(120, timeoutSec <= 0 ? 30 : timeoutSec));
        serverProgress = progress;
    }

    public static void clear() {
        active = false;
        companionName = "";
        serverProgress = -1f;
    }

    public static boolean isActive() {
        return active;
    }

    public static String companionName() {
        return companionName;
    }

    public static int timeoutSeconds() {
        return timeoutSeconds;
    }

    public static long startedAtMs() {
        return startedAtMs;
    }

    /** Fill amount for the loading bar (0–1). */
    public static float progress() {
        if (!active) {
            return 0f;
        }
        if (serverProgress >= 0f) {
            return Math.max(0f, Math.min(1f, serverProgress));
        }
        return CompanionAiInput.softProgress(startedAtMs, timeoutSeconds, System.currentTimeMillis());
    }

    /** Radians for gear spin animation. */
    public static float gearRadians(float partialTick) {
        if (!active) {
            return 0f;
        }
        long t = System.currentTimeMillis() - startedAtMs;
        return (t / 1000f + partialTick * 0.05f) * ((float) Math.PI * 2f);
    }
}
