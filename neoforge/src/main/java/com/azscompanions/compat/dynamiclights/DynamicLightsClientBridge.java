package com.azscompanions.compat.dynamiclights;

import com.azscompanions.config.ClientConfig;

/**
 * Client-only bridge so common-setup never touches {@link ClientConfig} on dedicated servers.
 */
public final class DynamicLightsClientBridge {
    private DynamicLightsClientBridge() {
    }

    public static void syncFromClientConfig() {
        DynamicLightsSettings s = new DynamicLightsSettings();
        s.setDynamicLightsCompat(ClientConfig.DYNAMIC_LIGHTS_COMPAT.get());
        DynamicLightsCompat.applySettings(s);
        DynamicLightsCompatModule.registerLegacyHandlerIfNeeded();
    }
}
