package com.azscompanions.compat.fancyanim;

/**
 * Soft facade for Fancy Animations / EMF / ETF resource-pack rendering.
 * Defaults keep vanilla-visible cutout skins; translucent is opt-in when EMF/ETF is present.
 */
public final class FancyAnimCompat {
    private static volatile FancyAnimSettings settings = new FancyAnimSettings();
    /** Set at client bootstrap when entity_model_features and/or entity_texture_features are loaded. */
    private static volatile boolean packSupportPresent;

    private FancyAnimCompat() {
    }

    public static FancyAnimSettings settings() {
        return settings;
    }

    public static void applySettings(FancyAnimSettings next) {
        settings = next == null ? new FancyAnimSettings() : next.copy();
    }

    public static void setPackSupportPresent(boolean present) {
        packSupportPresent = present;
    }

    public static boolean isPackSupportPresent() {
        return packSupportPresent;
    }

    /**
     * Player-form body / cape use translucent buffers only when the config allows it
     * <em>and</em> EMF/ETF is installed. Without packs, always prefer cutout so skins stay visible
     * (Iris/Sodium and some GPUs draw non-player {@code entityTranslucent} meshes as fully invisible).
     */
    public static boolean useTranslucentPlayerSkins() {
        return settings.translucentPlayerSkins() && packSupportPresent;
    }

    /** Mob-form client proxies should share the companion UUID when enabled. */
    public static boolean syncMobFormUuid() {
        return settings.syncMobFormUuid();
    }
}
