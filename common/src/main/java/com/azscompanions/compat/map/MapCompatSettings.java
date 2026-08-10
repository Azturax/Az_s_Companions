package com.azscompanions.compat.map;

/**
 * Client-facing toggles for minimap / world-map companion visibility.
 * Shared by NeoForge ({@code ClientConfig}) and Fabric ({@code azscompanions-map.json}).
 */
public final class MapCompatSettings {
    public static final String FILE_NAME = "azscompanions-map.json";

    /** Master switch: companions may appear on map entity radars. */
    private boolean showOnMinimap = true;
    /** When false, child Bits / fight children are hidden from map radars that honor this API. */
    private boolean showChildrenOnMap = true;
    /** Prefer showing the companion custom name on the radar label when the map mod allows. */
    private boolean showNameOnMap = true;
    /** Append a short owner hint in JourneyMap tooltips when available. */
    private boolean showOwnerOnMap = true;
    /** Dot/label tint (ARGB). Used when JourneyMap WrappedEntity color can be set. */
    private int iconColorArgb = 0xFFE91E8C;

    public boolean showOnMinimap() {
        return showOnMinimap;
    }

    public void setShowOnMinimap(boolean showOnMinimap) {
        this.showOnMinimap = showOnMinimap;
    }

    public boolean showChildrenOnMap() {
        return showChildrenOnMap;
    }

    public void setShowChildrenOnMap(boolean showChildrenOnMap) {
        this.showChildrenOnMap = showChildrenOnMap;
    }

    public boolean showNameOnMap() {
        return showNameOnMap;
    }

    public void setShowNameOnMap(boolean showNameOnMap) {
        this.showNameOnMap = showNameOnMap;
    }

    public boolean showOwnerOnMap() {
        return showOwnerOnMap;
    }

    public void setShowOwnerOnMap(boolean showOwnerOnMap) {
        this.showOwnerOnMap = showOwnerOnMap;
    }

    public int iconColorArgb() {
        return iconColorArgb;
    }

    public void setIconColorArgb(int iconColorArgb) {
        this.iconColorArgb = iconColorArgb;
    }

    public MapCompatSettings copy() {
        MapCompatSettings c = new MapCompatSettings();
        c.showOnMinimap = showOnMinimap;
        c.showChildrenOnMap = showChildrenOnMap;
        c.showNameOnMap = showNameOnMap;
        c.showOwnerOnMap = showOwnerOnMap;
        c.iconColorArgb = iconColorArgb;
        return c;
    }
}
