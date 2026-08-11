package com.azscompanions.loot;

import com.azscompanions.entity.JindujunSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionLootSupportTest {
    @Test
    void treasureRatesAreRareAndRollsStaySmall() {
        assertEquals(0.05f, CompanionLootSupport.DESERT_PYRAMID_CHARM_CHANCE, 0.0001f);
        assertEquals(0.005f, JindujunSupport.TRAIL_RUINS_LOOT_CHANCE, 0.0001f);
        assertEquals(1, CompanionLootSupport.TREASURE_ROLLS_MIN);
        assertEquals(3, CompanionLootSupport.TREASURE_ROLLS_MAX);
        assertTrue(CompanionLootSupport.TREASURE_ROLLS_MIN <= CompanionLootSupport.TREASURE_ROLLS_MAX);
        assertTrue(CompanionLootSupport.DESERT_PYRAMID_CHARM_CHANCE < 0.5f);
        assertTrue(JindujunSupport.TRAIL_RUINS_LOOT_CHANCE < 0.02f);
    }

    @Test
    void enableLootDefaultsOnAndGatesInjection() {
        assertTrue(CompanionLootSupport.DEFAULT_ENABLE_LOOT);
        boolean previous = CompanionLootSupport.isLootInjectionEnabled();
        try {
            CompanionLootSupport.setLootInjectionEnabled(true);
            assertTrue(CompanionLootSupport.isLootInjectionEnabled());
            CompanionLootSupport.setLootInjectionEnabled(false);
            assertFalse(CompanionLootSupport.isLootInjectionEnabled());
        } finally {
            CompanionLootSupport.setLootInjectionEnabled(previous);
        }
    }
}
