package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompanionDimensionTravelTest {
    @Test
    void detectsAnyKeyChangeIncludingModded() {
        assertTrue(CompanionDimensionTravel.isDimensionChange(
                "minecraft:overworld", "minecraft:the_nether"));
        assertTrue(CompanionDimensionTravel.isDimensionChange(
                "minecraft:overworld", "ad_astra:mars"));
        assertTrue(CompanionDimensionTravel.isDimensionChange(
                "rftoolsdim:dim1", "twilightforest:twilight_forest"));
        assertFalse(CompanionDimensionTravel.isDimensionChange(
                "minecraft:the_nether", "minecraft:the_nether"));
        assertFalse(CompanionDimensionTravel.isDimensionChange(null, "minecraft:overworld"));
        assertFalse(CompanionDimensionTravel.isDimensionChange("minecraft:overworld", null));
    }

    @Test
    void dimensionIdParsesResourceKeyToString() {
        assertEquals("minecraft:overworld", CompanionDimensionTravel.dimensionId(
                "ResourceKey[minecraft:dimension / minecraft:overworld]"));
        assertEquals("ad_astra:mars", CompanionDimensionTravel.dimensionId("ad_astra:mars"));
        assertTrue(CompanionDimensionTravel.isVanillaOverworldId("minecraft:overworld"));
        assertFalse(CompanionDimensionTravel.isVanillaOverworldId("minecraft:the_nether"));
    }

    @Test
    void identityKeysIncludePersonaAndForm() {
        boolean hasForm = false;
        boolean hasPersona = false;
        boolean hasSkin = false;
        for (String key : CompanionDimensionTravel.IDENTITY_NBT_KEYS) {
            if ("CompanionForm".equals(key) || CompanionFormVariants.NBT_KEY.equals(key)) {
                hasForm = true;
            }
            if (CompanionPersonaInit.INITIALIZED.equals(key) || "WhoAmI".equals(key)) {
                hasPersona = true;
            }
            if ("SkinPath".equals(key)) {
                hasSkin = true;
            }
        }
        assertTrue(hasForm);
        assertTrue(hasPersona);
        assertTrue(hasSkin);
    }

    @Test
    void personaInitializedWhenFlagOrTextPresent() {
        assertTrue(CompanionDimensionTravel.identityMarksPersonaInitialized(
                true, false, false, false, false, false, false));
        assertTrue(CompanionDimensionTravel.identityMarksPersonaInitialized(
                false, true, false, false, false, false, false));
        assertFalse(CompanionDimensionTravel.identityMarksPersonaInitialized(
                false, false, false, false, false, false, false));
    }

    /** Local alias so the test file does not hard-depend on CompanionPersona class name typos. */
    private static final class CompanionPersonaInit {
        static final String INITIALIZED = com.azscompanions.ai.CompanionPersona.NBT_INITIALIZED;
    }
}
