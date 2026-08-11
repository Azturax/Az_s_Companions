package com.azscompanions.compat.dynamiclights;

import com.azscompanions.AzsCompanionsFabric;
import com.azscompanions.registry.FabricModEntities;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/** Fabric client bootstrap for dynamic lighting soft-compat. */
public final class FabricDynamicLightsCompat {
    private FabricDynamicLightsCompat() {
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(DynamicLightsSettings.FILE_NAME);
    }

    public static void bootstrapClient() {
        try {
            DynamicLightsCompat.applySettings(DynamicLightsConfigIO.loadOrCreate(configPath()));
        } catch (Exception e) {
            AzsCompanionsFabric.LOGGER.warn("Failed to load {}; using defaults", DynamicLightsSettings.FILE_NAME, e);
            DynamicLightsCompat.applySettings(new DynamicLightsSettings());
        }

        DynamicLightsCompat.detectAndStore(FabricLoader.getInstance()::isModLoaded);
        if (!DynamicLightsCompat.isCompatEnabled()) {
            AzsCompanionsFabric.LOGGER.info("Dynamic lights soft-compat disabled via config");
            return;
        }
        if (DynamicLightsCompat.isLightingModPresent()) {
            AzsCompanionsFabric.LOGGER.info(
                    "Dynamic lights soft-compat ready ({})",
                    String.join(", ", DynamicLightsCompat.presentModIds()));
            if (DynamicLightsLegacyHooks.tryRegisterLivingEntityHandler(
                    () -> FabricModEntities.COMPANION,
                    com.azscompanions.entity.CompanionOrbSupport::lightLuminanceReflective)) {
                AzsCompanionsFabric.LOGGER.info("Registered companion with legacy DynamicLightHandlers API");
            }
        }
    }
}
