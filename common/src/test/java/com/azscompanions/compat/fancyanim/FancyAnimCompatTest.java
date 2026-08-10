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
        FancyAnimCompat.setPackSupportPresent(false);
    }

    @Test
    void withoutPacksTranslucentStaysOffEvenWhenConfigAllows() {
        assertTrue(FancyAnimCompat.settings().translucentPlayerSkins());
        assertFalse(FancyAnimCompat.isPackSupportPresent());
        assertFalse(FancyAnimCompat.useTranslucentPlayerSkins());
        assertTrue(FancyAnimCompat.syncMobFormUuid());
    }

    @Test
    void translucentRequiresConfigAndPack() {
        FancyAnimCompat.setPackSupportPresent(true);
        assertTrue(FancyAnimCompat.useTranslucentPlayerSkins());

        FancyAnimSettings off = new FancyAnimSettings();
        off.setTranslucentPlayerSkins(false);
        FancyAnimCompat.applySettings(off);
        assertFalse(FancyAnimCompat.useTranslucentPlayerSkins());
    }

    @Test
    void togglesApply() {
        FancyAnimSettings off = new FancyAnimSettings();
        off.setTranslucentPlayerSkins(false);
        off.setSyncMobFormUuid(false);
        FancyAnimCompat.applySettings(off);
        FancyAnimCompat.setPackSupportPresent(true);
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
