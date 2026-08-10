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
        boolean emf = FabricLoader.getInstance().isModLoaded("entity_model_features");
        boolean etf = FabricLoader.getInstance().isModLoaded("entity_texture_features");
        FancyAnimCompat.setPackSupportPresent(emf || etf);
        if (emf || etf) {
            AzsCompanionsFabric.LOGGER.info(
                    "Fancy Anim soft-compat ready (entity_model_features={}, entity_texture_features={})",
                    emf, etf);
        }
        try {
            FancyAnimCompat.applySettings(FancyAnimConfigIO.loadOrCreate(configPath()));
        } catch (Exception e) {
            AzsCompanionsFabric.LOGGER.warn("Failed to load {}; using defaults", FancyAnimSettings.FILE_NAME, e);
            FancyAnimCompat.applySettings(new FancyAnimSettings());
        }
    }
}
