package com.azscompanions.compat.dynamiclights;

/**
 * Client toggles for LambDynamicLights / RyoamicLights / similar soft-compat.
 * Soft-compat only — no hard dependency on those mods.
 */
public final class DynamicLightsSettings {
    public static final String FILE_NAME = "azscompanions-dynamiclights.json";

    /**
     * When true, log detected lighting mods and run optional API/datapack registration hooks.
     * Companions already expose hand/armor via {@code LivingEntity#getItemBySlot}, which most
     * dynamic-light mods scan automatically; this flag does not disable those mods' own mixins.
     */
    private boolean dynamicLightsCompat = true;

    public boolean dynamicLightsCompat() {
        return dynamicLightsCompat;
    }

    public void setDynamicLightsCompat(boolean dynamicLightsCompat) {
        this.dynamicLightsCompat = dynamicLightsCompat;
    }

    public DynamicLightsSettings copy() {
        DynamicLightsSettings c = new DynamicLightsSettings();
        c.dynamicLightsCompat = dynamicLightsCompat;
        return c;
    }
}
