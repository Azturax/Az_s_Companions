package com.azscompanions.compat.map;

import com.azscompanions.config.ClientConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-only bridge so common-setup never touches {@link ClientConfig} on dedicated servers.
 */
@OnlyIn(Dist.CLIENT)
public final class MapCompatClientBridge {
    private MapCompatClientBridge() {
    }

    public static void syncFromClientConfig() {
        MapCompatSettings s = new MapCompatSettings();
        s.setShowOnMinimap(ClientConfig.SHOW_ON_MINIMAP.get());
        s.setShowChildrenOnMap(ClientConfig.SHOW_CHILDREN_ON_MAP.get());
        s.setShowNameOnMap(ClientConfig.SHOW_NAME_ON_MAP.get());
        s.setShowOwnerOnMap(ClientConfig.SHOW_OWNER_ON_MAP.get());
        s.setIconColorArgb(ClientConfig.MAP_ICON_COLOR.get());
        MapCompat.applySettings(s);
    }
}
