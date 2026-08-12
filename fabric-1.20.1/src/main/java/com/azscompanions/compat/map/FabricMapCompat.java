package com.azscompanions.compat.map;

import com.azscompanions.AzsCompanionsFabric;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/**
 * Optional map-mod soft-compat bootstrap (Fabric 1.21.1).
 * JourneyMap plugin is annotation-discovered — do not classload JM plugin types unless needed.
 */
public final class FabricMapCompat {
    private FabricMapCompat() {
    }

    public static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(MapCompatSettings.FILE_NAME);
    }

    public static void bootstrapClient() {
        try {
            MapCompat.applySettings(MapCompatConfigIO.loadOrCreate(configPath()));
        } catch (Exception e) {
            AzsCompanionsFabric.LOGGER.warn("Failed to load {}; using defaults", MapCompatSettings.FILE_NAME, e);
            MapCompat.applySettings(new MapCompatSettings());
        }
        boolean xaeroMini = FabricLoader.getInstance().isModLoaded("xaerominimap");
        boolean xaeroWorld = FabricLoader.getInstance().isModLoaded("xaeroworldmap");
        boolean journeyMap = FabricLoader.getInstance().isModLoaded("journeymap");
        if (xaeroMini || xaeroWorld || journeyMap) {
            AzsCompanionsFabric.LOGGER.info(
                    "Map soft-compat ready (xaerominimap={}, xaeroworldmap={}, journeymap={})",
                    xaeroMini, xaeroWorld, journeyMap);
        }
    }
}
