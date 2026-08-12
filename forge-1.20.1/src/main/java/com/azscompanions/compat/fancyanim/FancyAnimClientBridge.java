package com.azscompanions.compat.fancyanim;

import com.azscompanions.config.ClientConfig;

/**
 * Client-only bridge so common-setup never touches {@link ClientConfig} on dedicated servers.
 */
public final class FancyAnimClientBridge {
    private FancyAnimClientBridge() {
    }

    public static void syncFromClientConfig() {
        FancyAnimSettings s = new FancyAnimSettings();
        s.setTranslucentPlayerSkins(ClientConfig.TRANSLUCENT_PLAYER_SKINS.get());
        s.setSyncMobFormUuid(ClientConfig.SYNC_MOB_FORM_UUID.get());
        FancyAnimCompat.applySettings(s);
    }
}
