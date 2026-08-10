package com.azscompanions.compat.dynamiclights;

import java.util.List;
import java.util.function.Predicate;

/**
 * Soft facade for dynamic lighting mods (LambDynamicLights, RyoamicLights, …).
 * Companions are {@code LivingEntity} and expose held torches/lanterns via
 * {@code getItemBySlot(MAINHAND/OFFHAND)} — LDL and forks already scan that path.
 */
public final class DynamicLightsCompat {
    private static volatile DynamicLightsSettings settings = new DynamicLightsSettings();
    private static volatile boolean lightingModPresent;
    private static volatile List<String> presentModIds = List.of();

    private DynamicLightsCompat() {
    }

    public static DynamicLightsSettings settings() {
        return settings;
    }

    public static void applySettings(DynamicLightsSettings next) {
        settings = next == null ? new DynamicLightsSettings() : next.copy();
    }

    public static void setPresentMods(List<String> modIds) {
        presentModIds = modIds == null ? List.of() : List.copyOf(modIds);
        lightingModPresent = !presentModIds.isEmpty();
    }

    public static void detectAndStore(Predicate<String> isLoaded) {
        setPresentMods(DynamicLightsMods.detectPresent(isLoaded));
    }

    public static boolean isLightingModPresent() {
        return lightingModPresent;
    }

    public static List<String> presentModIds() {
        return presentModIds;
    }

    /**
     * Whether our soft-compat hooks should run (config on <em>and</em> a known lighting mod loaded).
     * Held-item glow from LDL's LivingEntity mixin still applies when that mod is installed
     * even if this returns false — we cannot disable third-party mixins.
     */
    public static boolean shouldApplyHooks() {
        return settings.dynamicLightsCompat() && lightingModPresent;
    }

    /**
     * Master config gate used by loader bootstraps before detecting mods.
     */
    public static boolean isCompatEnabled() {
        return settings.dynamicLightsCompat();
    }
}
