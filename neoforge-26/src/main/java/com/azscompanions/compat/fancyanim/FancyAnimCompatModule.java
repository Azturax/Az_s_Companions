package com.azscompanions.compat.fancyanim;

import com.azscompanions.AzsCompanions;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Optional Fancy Animations / EMF / ETF soft-compat bootstrap (NeoForge 1.21.1).
 * No hard dependency — only logs when EMF/ETF are present and syncs client config.
 */
public final class FancyAnimCompatModule {
    private FancyAnimCompatModule() {
    }

    public static void bootstrap() {
        boolean emf = ModList.get().isLoaded("entity_model_features");
        boolean etf = ModList.get().isLoaded("entity_texture_features");
        FancyAnimCompat.setPackSupportPresent(emf || etf);
        if (emf || etf) {
            AzsCompanions.LOGGER.info(
                    "Fancy Anim soft-compat ready (entity_model_features={}, entity_texture_features={})",
                    emf, etf);
        }
        if (FMLEnvironment.getDist().isClient()) {
            trySyncClientSettings();
        }
    }

    public static void trySyncClientSettings() {
        if (!FMLEnvironment.getDist().isClient()) {
            return;
        }
        try {
            Class<?> bridge = Class.forName("com.azscompanions.compat.fancyanim.FancyAnimClientBridge");
            bridge.getMethod("syncFromClientConfig").invoke(null);
        } catch (ReflectiveOperationException e) {
            AzsCompanions.LOGGER.debug("Fancy Anim client settings sync skipped: {}", e.toString());
        }
    }
}
