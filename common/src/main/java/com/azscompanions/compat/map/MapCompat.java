package com.azscompanions.compat.map;

/**
 * Soft map-mod facade. Settings only — loader plugins apply JourneyMap / Xaero hooks.
 * FTB Chunks claim overlays stay in {@code compat.ftb}; this package does not replace them.
 */
public final class MapCompat {
    private static volatile MapCompatSettings settings = new MapCompatSettings();

    private MapCompat() {
    }

    public static MapCompatSettings settings() {
        return settings;
    }

    public static void applySettings(MapCompatSettings next) {
        settings = next == null ? new MapCompatSettings() : next.copy();
    }

    /**
     * Whether a companion entity should be drawn on map radars that honor our toggles.
     *
     * @param childCompanion true when the entity is a child Bit / fight child
     */
    public static boolean shouldShowOnMap(boolean childCompanion) {
        MapCompatSettings s = settings;
        if (!s.showOnMinimap()) {
            return false;
        }
        if (childCompanion && !s.showChildrenOnMap()) {
            return false;
        }
        return true;
    }
}
