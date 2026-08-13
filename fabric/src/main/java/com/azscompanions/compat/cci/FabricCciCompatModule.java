package com.azscompanions.compat.cci;

import com.azscompanions.AzsCompanionsFabric;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Soft-loads Fabric CCI bridge only when Content Creator Integration is present.
 * Never classloads CCI API types otherwise.
 */
public final class FabricCciCompatModule {
    public static final String CCI_MOD_ID = "contentcreatorintegration";

    private FabricCciCompatModule() {
    }

    public static void bootstrapCommon() {
        if (!FabricLoader.getInstance().isModLoaded(CCI_MOD_ID)) {
            AzsCompanionsFabric.LOGGER.info("CCI soft-compat idle (contentcreatorintegration not installed)");
            return;
        }
        try {
            Class<?> boot = Class.forName("com.azscompanions.compat.cci.FabricCciBootstrap");
            boot.getMethod("bootstrap").invoke(null);
            AzsCompanionsFabric.LOGGER.info("CCI soft-compat registered (optional Content Creator Integration)");
        } catch (Throwable t) {
            AzsCompanionsFabric.LOGGER.warn("CCI soft-compat failed to register", t);
        }
    }

    public static void bootstrapClient() {
        if (!FabricLoader.getInstance().isModLoaded(CCI_MOD_ID)) {
            return;
        }
        try {
            Class<?> boot = Class.forName("com.azscompanions.compat.cci.FabricCciClientBootstrap");
            boot.getMethod("bootstrap").invoke(null);
        } catch (Throwable t) {
            AzsCompanionsFabric.LOGGER.warn("CCI soft-compat client failed to register", t);
        }
    }
}