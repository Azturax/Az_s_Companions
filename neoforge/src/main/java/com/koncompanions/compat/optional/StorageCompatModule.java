package com.koncompanions.compat.optional;

import com.koncompanions.KonCompanions;

/**
 * Optional storage-mod integration module (Drawers, Sophisticated Storage/Backpacks, etc.).
 * Enabled only when those mods are loaded — no hard dependency.
 */
public final class StorageCompatModule {
    private StorageCompatModule() {
    }

    public static void bootstrap() {
        KonCompanions.LOGGER.info("StorageCompatModule active — register drawer/backpack handlers here");
        // Example: CompanionApi.registerMachineHandler(...);
    }
}
