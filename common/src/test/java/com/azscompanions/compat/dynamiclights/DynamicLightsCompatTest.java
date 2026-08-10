package com.azscompanions.compat.dynamiclights;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicLightsCompatTest {
    @AfterEach
    void reset() {
        DynamicLightsCompat.applySettings(new DynamicLightsSettings());
        DynamicLightsCompat.setPresentMods(List.of());
    }

    @Test
    void defaultsEnableCompat() {
        assertTrue(DynamicLightsCompat.settings().dynamicLightsCompat());
        assertTrue(DynamicLightsCompat.isCompatEnabled());
        assertFalse(DynamicLightsCompat.isLightingModPresent());
        assertFalse(DynamicLightsCompat.shouldApplyHooks());
    }

    @Test
    void hooksRequireConfigAndPresentMod() {
        DynamicLightsCompat.setPresentMods(List.of(DynamicLightsMods.LAMB_DYN_LIGHTS));
        assertTrue(DynamicLightsCompat.shouldApplyHooks());

        DynamicLightsSettings off = new DynamicLightsSettings();
        off.setDynamicLightsCompat(false);
        DynamicLightsCompat.applySettings(off);
        assertFalse(DynamicLightsCompat.shouldApplyHooks());
        assertTrue(DynamicLightsCompat.isLightingModPresent());
    }

    @Test
    void detectPresentFiltersKnownIds() {
        Set<String> loaded = Set.of("lambdynlights", "sodium", "ryoamiclights");
        List<String> found = DynamicLightsMods.detectPresent(loaded::contains);
        assertEquals(List.of("lambdynlights", "ryoamiclights"), found);
        assertTrue(DynamicLightsMods.anyPresent(loaded::contains));
        assertFalse(DynamicLightsMods.anyPresent(id -> false));
    }

    @Test
    void looksLikeDynamicLightsMod() {
        assertTrue(DynamicLightsMods.looksLikeDynamicLightsMod("lambdynlights"));
        assertTrue(DynamicLightsMods.looksLikeDynamicLightsMod("SomeDynamicLightsFork"));
        assertFalse(DynamicLightsMods.looksLikeDynamicLightsMod("journeymap"));
    }

    @Test
    void jsonRoundTrip() {
        DynamicLightsSettings s = new DynamicLightsSettings();
        s.setDynamicLightsCompat(false);
        JsonObject json = DynamicLightsConfigIO.toJson(s);
        DynamicLightsSettings loaded = DynamicLightsConfigIO.fromJson(json);
        assertFalse(loaded.dynamicLightsCompat());
    }
}
