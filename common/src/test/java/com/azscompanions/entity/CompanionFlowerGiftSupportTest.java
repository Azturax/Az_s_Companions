package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionFlowerGiftSupportTest {
    @Test
    void cooldownGatesGifts() {
        assertTrue(CompanionFlowerGiftSupport.canGift(100, 100));
        assertTrue(CompanionFlowerGiftSupport.canGift(101, 100));
        assertFalse(CompanionFlowerGiftSupport.canGift(99, 100));
        assertEquals(160L, CompanionFlowerGiftSupport.nextCooldownUntil(100));
    }

    @Test
    void placementPrefersMainThenOffWithoutStealing() {
        assertEquals(
                CompanionFlowerGiftSupport.HandPlacement.MAIN_HAND,
                CompanionFlowerGiftSupport.placement(true, false, false, false));
        assertEquals(
                CompanionFlowerGiftSupport.HandPlacement.MAIN_HAND,
                CompanionFlowerGiftSupport.placement(false, true, false, false));
        assertEquals(
                CompanionFlowerGiftSupport.HandPlacement.OFF_HAND,
                CompanionFlowerGiftSupport.placement(false, false, true, false));
        assertEquals(
                CompanionFlowerGiftSupport.HandPlacement.PENDING_ONLY,
                CompanionFlowerGiftSupport.placement(false, false, false, false));
    }

    @Test
    void offerPoolIsNonEmptyAndPickIsStable() {
        assertFalse(CompanionFlowerGiftSupport.OFFER_FLOWER_IDS.isEmpty());
        assertEquals(
                CompanionFlowerGiftSupport.OFFER_FLOWER_IDS.get(0),
                CompanionFlowerGiftSupport.pickRandomOfferId(0));
        assertEquals(
                CompanionFlowerGiftSupport.OFFER_FLOWER_IDS.get(3),
                CompanionFlowerGiftSupport.pickRandomOfferId(3));
    }

    @Test
    void throwVelocityAimsHorizontallyTowardTarget() {
        double[] v = CompanionFlowerGiftSupport.throwVelocity(0, 1, 0, 10, 1, 0);
        assertEquals(CompanionFlowerGiftSupport.THROW_HORIZONTAL, v[0], 1.0e-9);
        assertEquals(CompanionFlowerGiftSupport.THROW_VERTICAL, v[1], 1.0e-9);
        assertEquals(0.0d, v[2], 1.0e-9);
    }

    @Test
    void throwVelocityOnTopOfTargetIsUpwardOnly() {
        double[] v = CompanionFlowerGiftSupport.throwVelocity(1, 1, 1, 1, 2, 1);
        assertEquals(0.0d, v[0], 1.0e-9);
        assertTrue(v[1] >= CompanionFlowerGiftSupport.THROW_VERTICAL);
        assertEquals(0.0d, v[2], 1.0e-9);
    }

    @Test
    void biomeHintsFromFlagsAndTemperature() {
        java.util.EnumSet<CompanionGiftOfferSupport.Hint> hints =
                java.util.EnumSet.noneOf(CompanionGiftOfferSupport.Hint.class);
        CompanionFlowerGiftSupport.addBiomeHints(hints, true, false, false, 0.8f);
        assertTrue(hints.contains(CompanionGiftOfferSupport.Hint.BIOME_OCEAN));
        hints.clear();
        CompanionFlowerGiftSupport.addBiomeHints(hints, false, false, false, 0.05f);
        assertTrue(hints.contains(CompanionGiftOfferSupport.Hint.BIOME_COLD));
        hints.clear();
        CompanionFlowerGiftSupport.addBiomeHints(hints, false, false, true, 2.0f);
        assertTrue(hints.contains(CompanionGiftOfferSupport.Hint.BIOME_NETHER));
        assertFalse(hints.contains(CompanionGiftOfferSupport.Hint.BIOME_DESERT));
    }
}
