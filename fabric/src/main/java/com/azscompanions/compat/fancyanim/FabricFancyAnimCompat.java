package com.azscompanions.compat.fancyanim;

import com.azscompanions.AzsCompanionsFabric;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/** Fabric client bootstrap for Fancy Animations soft-compat settings. */
public final class FabricFancyAnimCompat {
    private FabricFancyAnimCompat() {
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FancyAnimSettings.FILE_NAME);
    }

    public static void bootstrapClient() {
        try {
            FancyAnimCompat.applySettings(FancyAnimConfigIO.loadOrCreate(configPath()));
        } catch (Exception e) {
            AzsCompanionsFabric.LOGGER.warn("Failed to load {}; using defaults", FancyAnimSettings.FILE_NAME, e);
            FancyAnimCompat.applySettings(new FancyAnimSettings());
        }
    }
}
