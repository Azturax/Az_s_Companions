package com.azscompanions.compat.fancyanim;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FancyAnimCompatTest {
    @AfterEach
    void reset() {
        FancyAnimCompat.applySettings(new FancyAnimSettings());
    }

    @Test
    void defaultsPreferPackFriendlyRendering() {
        assertTrue(FancyAnimCompat.useTranslucentPlayerSkins());
        assertTrue(FancyAnimCompat.syncMobFormUuid());
    }

    @Test
    void togglesApply() {
        FancyAnimSettings off = new FancyAnimSettings();
        off.setTranslucentPlayerSkins(false);
        off.setSyncMobFormUuid(false);
        FancyAnimCompat.applySettings(off);
        assertFalse(FancyAnimCompat.useTranslucentPlayerSkins());
        assertFalse(FancyAnimCompat.syncMobFormUuid());
    }

    @Test
    void jsonRoundTrip() {
        FancyAnimSettings s = new FancyAnimSettings();
        s.setTranslucentPlayerSkins(false);
        s.setSyncMobFormUuid(false);
        JsonObject json = FancyAnimConfigIO.toJson(s);
        FancyAnimSettings loaded = FancyAnimConfigIO.fromJson(json);
        assertFalse(loaded.translucentPlayerSkins());
        assertFalse(loaded.syncMobFormUuid());
    }
}
