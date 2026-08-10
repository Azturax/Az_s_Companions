package com.azscompanions.compat.fancyanim;

/**
 * Soft facade for Fancy Animations / EMF / ETF resource-pack rendering.
 * Defaults are safe with no packs installed (vanilla-looking cutout can be restored via config).
 */
public final class FancyAnimCompat {
    private static volatile FancyAnimSettings settings = new FancyAnimSettings();

    private FancyAnimCompat() {
    }

    public static FancyAnimSettings settings() {
        return settings;
    }

    public static void applySettings(FancyAnimSettings next) {
        settings = next == null ? new FancyAnimSettings() : next.copy();
    }

    /** Player-form body / cape should use translucent buffers when enabled. */
    public static boolean useTranslucentPlayerSkins() {
        return settings.translucentPlayerSkins();
    }

    /** Mob-form client proxies should share the companion UUID when enabled. */
    public static boolean syncMobFormUuid() {
        return settings.syncMobFormUuid();
    }
}
