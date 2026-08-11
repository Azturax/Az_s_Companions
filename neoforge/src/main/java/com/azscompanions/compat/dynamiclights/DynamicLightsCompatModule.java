package com.azscompanions.compat.dynamiclights;

import com.azscompanions.AzsCompanions;
import com.azscompanions.registry.ModEntities;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Optional dynamic-lighting soft-compat bootstrap (NeoForge 1.21.1).
 * No hard dependency — detects LambDynamicLights / RyoamicLights / similar and optionally
 * registers legacy DynamicLightHandlers for the companion entity type.
 */
public final class DynamicLightsCompatModule {
    private DynamicLightsCompatModule() {
    }

    public static void bootstrap() {
        DynamicLightsCompat.detectAndStore(ModList.get()::isLoaded);
        if (DynamicLightsCompat.isLightingModPresent()) {
            AzsCompanions.LOGGER.info(
                    "Dynamic lights soft-compat detected ({})",
                    String.join(", ", DynamicLightsCompat.presentModIds()));
        }
        if (FMLEnvironment.dist.isClient()) {
            trySyncClientSettings();
        }
    }

    public static void trySyncClientSettings() {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        try {
            Class<?> bridge = Class.forName("com.azscompanions.compat.dynamiclights.DynamicLightsClientBridge");
            bridge.getMethod("syncFromClientConfig").invoke(null);
        } catch (ReflectiveOperationException e) {
            AzsCompanions.LOGGER.debug("Dynamic lights client settings sync skipped: {}", e.toString());
        }
    }

    /** Called from the client bridge after config is applied. */
    public static void registerLegacyHandlerIfNeeded() {
        if (!DynamicLightsCompat.shouldApplyHooks()) {
            return;
        }
        if (DynamicLightsLegacyHooks.tryRegisterLivingEntityHandler(
                () -> ModEntities.COMPANION.get(),
                com.azscompanions.entity.CompanionOrbSupport::lightLuminanceReflective)) {
            AzsCompanions.LOGGER.info("Registered companion with legacy DynamicLightHandlers API");
        }
    }
}
