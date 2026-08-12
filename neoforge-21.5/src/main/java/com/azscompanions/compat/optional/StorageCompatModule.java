package com.azscompanions.compat.optional;

import com.azscompanions.AzsCompanions;

/**
 * Optional storage-mod integration module (Drawers, Sophisticated Storage/Backpacks, etc.).
 * Enabled only when those mods are loaded — no hard dependency.
 */
public final class StorageCompatModule {
    private StorageCompatModule() {
    }

    public static void bootstrap() {
        AzsCompanions.LOGGER.info("StorageCompatModule active — register drawer/backpack handlers here");
        // Example: CompanionApi.registerMachineHandler(...);
    }
}
