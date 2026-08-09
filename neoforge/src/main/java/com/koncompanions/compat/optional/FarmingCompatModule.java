package com.koncompanions.compat.optional;

import com.koncompanions.KonCompanions;

/**
 * Optional farming/cooking mod integration via tags and crop APIs.
 */
public final class FarmingCompatModule {
    private FarmingCompatModule() {
    }

    public static void bootstrap() {
        KonCompanions.LOGGER.info("FarmingCompatModule active — extend harvest/replant tags for modded crops");
    }
}
