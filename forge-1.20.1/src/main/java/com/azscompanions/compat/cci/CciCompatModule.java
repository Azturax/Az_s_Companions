package com.azscompanions.compat.cci;

import com.azscompanions.AzsCompanions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;

/**
 * Soft-loads CCI bridge only when Content Creator Integration is present.
 * Never classloads {@link CciCompatBootstrap} (or CCI API types) otherwise.
 */
public final class CciCompatModule {
    public static final String CCI_MOD_ID = "contentcreatorintegration";

    private CciCompatModule() {
    }

    public static void register(IEventBus modBus) {
        if (!ModList.get().isLoaded(CCI_MOD_ID)) {
            AzsCompanions.LOGGER.info("CCI soft-compat idle (contentcreatorintegration not installed)");
            return;
        }
        try {
            Class<?> boot = Class.forName("com.azscompanions.compat.cci.CciCompatBootstrap");
            boot.getMethod("register", IEventBus.class).invoke(null, modBus);
            AzsCompanions.LOGGER.info("CCI soft-compat registered (optional Content Creator Integration)");
        } catch (Throwable t) {
            AzsCompanions.LOGGER.warn("CCI soft-compat failed to register", t);
        }
    }
}