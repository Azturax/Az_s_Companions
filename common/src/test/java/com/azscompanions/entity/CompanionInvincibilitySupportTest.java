package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompanionInvincibilitySupportTest {
    @Test
    void konAndBitsAreInvincible() {
        assertTrue(CompanionInvincibilitySupport.isFullyInvincible(true, false, "Kon", "azscompanions:kon"));
        assertTrue(CompanionInvincibilitySupport.isFullyInvincible(false, true, "Bit", "azscompanions:kon"));
        assertTrue(CompanionInvincibilitySupport.isFullyInvincible(false, false, "Bits", "other:custom"));
        assertTrue(CompanionInvincibilitySupport.isFullyInvincible(false, false, "Dox", "azscompanions:kon"));
        assertTrue(CompanionInvincibilitySupport.isKonDefinition("azscompanions:kon"));
        assertTrue(CompanionInvincibilitySupport.isKonDefinition("kon"));
        assertTrue(CompanionInvincibilitySupport.isBitName("Bit"));
        assertTrue(CompanionInvincibilitySupport.isBitName("bits"));
    }

    @Test
    void customNonKonParentsAreNotFullyInvincible() {
        assertFalse(CompanionInvincibilitySupport.isFullyInvincible(false, false, "Wolfy", "azscompanions:custom"));
        assertFalse(CompanionInvincibilitySupport.isFullyInvincible(false, false, null, null));
        assertFalse(CompanionInvincibilitySupport.isKonName("Dox"));
    }

    @Test
    void cciSummonsAreNeverInvincible() {
        assertFalse(CompanionInvincibilitySupport.isFullyInvincible(
                true, false, "Kon", "azscompanions:kon", true));
        assertFalse(CompanionInvincibilitySupport.isFullyInvincible(
                false, true, "Bit", "azscompanions:kon", true));
        assertFalse(CompanionInvincibilitySupport.isFullyInvincible(
                false, false, "Alice", "azscompanions:kon", true));
    }

    @Test
    void unknownAndCustomDamageTypesAreStillCancelledWhenInvincible() {
        assertTrue(CompanionInvincibilitySupport.shouldCancelDamage(true, "draconicevolution:chaos"));
        assertTrue(CompanionInvincibilitySupport.shouldCancelDamage(true, "draconicevolution:wyvern"));
        assertTrue(CompanionInvincibilitySupport.shouldCancelDamage(true, "modded:armor_pierce"));
        assertTrue(CompanionInvincibilitySupport.shouldCancelDamage(true, "modded:absolute"));
        assertTrue(CompanionInvincibilitySupport.shouldCancelDamage(true, "unknown:custom"));
        assertTrue(CompanionInvincibilitySupport.shouldCancelDamage(true, null));
        assertFalse(CompanionInvincibilitySupport.shouldCancelDamage(false, "draconicevolution:chaos"));
        assertFalse(CompanionInvincibilitySupport.shouldCancelDamage(false, "minecraft:generic"));
    }

    @Test
    void cciSummonsDoNotCancelModdedDamage() {
        boolean cciInvincible = CompanionInvincibilitySupport.isFullyInvincible(
                true, false, "Kon", "azscompanions:kon", true);
        assertFalse(cciInvincible);
        assertFalse(CompanionInvincibilitySupport.shouldCancelDamage(cciInvincible, "draconicevolution:chaos"));
        assertFalse(CompanionInvincibilitySupport.shouldRejectHealthDrop(cciInvincible, 20.0f, 0.0f));
    }

    @Test
    void invincibleCompanionsRejectHealthDropsAndRestore() {
        assertTrue(CompanionInvincibilitySupport.shouldRejectHealthDrop(true, 20.0f, 0.0f));
        assertTrue(CompanionInvincibilitySupport.shouldRejectHealthDrop(true, 20.0f, 19.9f));
        assertTrue(CompanionInvincibilitySupport.shouldRejectHealthDrop(true, 20.0f, Float.NaN));
        assertFalse(CompanionInvincibilitySupport.shouldRejectHealthDrop(true, 20.0f, 20.0f));
        assertFalse(CompanionInvincibilitySupport.shouldRejectHealthDrop(false, 20.0f, 0.0f));
        assertEquals(20.0f, CompanionInvincibilitySupport.restoreHealth(true, 0.0f, 20.0f));
        assertEquals(20.0f, CompanionInvincibilitySupport.restoreHealth(true, 5.0f, 20.0f));
        assertEquals(5.0f, CompanionInvincibilitySupport.restoreHealth(false, 5.0f, 20.0f));
    }
}
