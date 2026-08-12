package com.azscompanions.compat.map;

import com.azscompanions.AzsCompanions;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Optional map-mod soft-compat bootstrap (NeoForge 1.21.1).
 * JourneyMap plugin is discovered via {@code @JourneyMapPlugin} — never classload that type here.
 * Xaero icons ship as resources under {@code assets/xaerominimap/...}.
 */
public final class MapCompatModule {
    private MapCompatModule() {
    }

    public static void bootstrap() {
        boolean xaeroMini = ModList.get().isLoaded("xaerominimap");
        boolean xaeroWorld = ModList.get().isLoaded("xaeroworldmap");
        boolean journeyMap = ModList.get().isLoaded("journeymap");
        if (xaeroMini || xaeroWorld || journeyMap) {
            AzsCompanions.LOGGER.info(
                    "Map soft-compat ready (xaerominimap={}, xaeroworldmap={}, journeymap={})",
                    xaeroMini, xaeroWorld, journeyMap);
        }
        if (FMLEnvironment.dist.isClient()) {
            // Deferred: ClientConfig may not be ready during common setup; client setup syncs again.
            trySyncClientSettings();
        }
    }

    public static void trySyncClientSettings() {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        try {
            Class<?> bridge = Class.forName("com.azscompanions.compat.map.MapCompatClientBridge");
            bridge.getMethod("syncFromClientConfig").invoke(null);
        } catch (ReflectiveOperationException e) {
            AzsCompanions.LOGGER.debug("Map client settings sync skipped: {}", e.toString());
        }
    }
}
