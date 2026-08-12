package com.azscompanions.compat.optional;

import com.azscompanions.AzsCompanions;

/**
 * Optional tech-mod integration (Create, Mekanism, etc.) using energy/fluid/item capabilities.
 */
public final class TechCompatModule {
    private TechCompatModule() {
    }

    public static void bootstrap() {
        AzsCompanions.LOGGER.info("TechCompatModule active — register energy/fluid machine handlers here");
    }
}
