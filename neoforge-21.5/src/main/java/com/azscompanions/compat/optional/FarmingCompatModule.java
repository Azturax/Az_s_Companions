package com.azscompanions.compat.optional;

import com.azscompanions.AzsCompanions;

/**
 * Optional farming/cooking mod integration via tags and crop APIs.
 */
public final class FarmingCompatModule {
    private FarmingCompatModule() {
    }

    public static void bootstrap() {
        AzsCompanions.LOGGER.info("FarmingCompatModule active — extend harvest/replant tags for modded crops");
    }
}
