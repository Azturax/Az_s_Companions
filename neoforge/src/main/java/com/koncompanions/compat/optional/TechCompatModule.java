package com.koncompanions.compat.optional;

import com.koncompanions.KonCompanions;

/**
 * Optional tech-mod integration (Create, Mekanism, etc.) using energy/fluid/item capabilities.
 */
public final class TechCompatModule {
    private TechCompatModule() {
    }

    public static void bootstrap() {
        KonCompanions.LOGGER.info("TechCompatModule active — register energy/fluid machine handlers here");
    }
}
