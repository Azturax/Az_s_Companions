package com.azscompanions.entity;

import com.azscompanions.ai.CompanionRecentActionKind;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionGiftOfferSupportTest {
    @Test
    void emptySnapshotStaysInFlowerPool() {
        String id = CompanionGiftOfferSupport.pickOfferId(CompanionGiftOfferSupport.Snapshot.empty(), 0);
        assertTrue(CompanionFlowerGiftSupport.OFFER_FLOWER_IDS.contains(id));
    }

    @Test
    void darknessPoolIncludesTorchFamily() {
        Map<String, Integer> pool = CompanionGiftOfferSupport.buildWeightedPool(
                CompanionGiftOfferSupport.Snapshot.of(CompanionGiftOfferSupport.Hint.DARKNESS));
        assertTrue(pool.containsKey("minecraft:torch"));
        assertTrue(pool.containsKey("minecraft:lantern"));
        assertTrue(pool.get("minecraft:torch") > pool.getOrDefault("minecraft:poppy", 0));
    }

    @Test
    void lowHungerFavorsFood() {
        Map<String, Integer> pool = CompanionGiftOfferSupport.buildWeightedPool(
                CompanionGiftOfferSupport.Snapshot.of(CompanionGiftOfferSupport.Hint.LOW_HUNGER));
        assertTrue(pool.containsKey("minecraft:bread"));
        assertTrue(pool.containsKey("minecraft:apple"));
        assertTrue(pool.get("minecraft:bread") >= 30);
    }

    @Test
    void sleepingAndHostileBiasExpectedItems() {
        Map<String, Integer> sleep = CompanionGiftOfferSupport.buildWeightedPool(
                CompanionGiftOfferSupport.Snapshot.of(CompanionGiftOfferSupport.Hint.SLEEPING));
        assertTrue(sleep.containsKey("minecraft:white_wool"));
        Map<String, Integer> hostile = CompanionGiftOfferSupport.buildWeightedPool(
                CompanionGiftOfferSupport.Snapshot.of(CompanionGiftOfferSupport.Hint.HOSTILE));
        assertTrue(hostile.containsKey("minecraft:wither_rose"));
    }

    @Test
    void recentActionKindsMapToHints() {
        EnumSet<CompanionGiftOfferSupport.Hint> hints = EnumSet.noneOf(CompanionGiftOfferSupport.Hint.class);
        CompanionGiftOfferSupport.addRecentActionHints(hints, CompanionRecentActionKind.EXPLOSION);
        CompanionGiftOfferSupport.addRecentActionHints(hints, CompanionRecentActionKind.CRAFT_READY);
        CompanionGiftOfferSupport.addRecentActionHints(hints, CompanionRecentActionKind.BLOCK_BREAK);
        assertEquals(
                Set.of(CompanionGiftOfferSupport.Hint.EXPLOSION, CompanionGiftOfferSupport.Hint.CRAFTING),
                hints);
    }

    @Test
    void weightedPickIsStableForFixedRoll() {
        CompanionGiftOfferSupport.Snapshot snap =
                CompanionGiftOfferSupport.Snapshot.of(CompanionGiftOfferSupport.Hint.DARKNESS);
        String a = CompanionGiftOfferSupport.pickOfferId(snap, 0);
        String b = CompanionGiftOfferSupport.pickOfferId(snap, 0);
        assertEquals(a, b);
        assertFalse(a.isBlank());
    }
}
