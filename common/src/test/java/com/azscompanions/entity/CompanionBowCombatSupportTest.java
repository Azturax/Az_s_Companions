package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompanionBowCombatSupportTest {
    @Test
    void detectsBowsAndArrows() {
        assertTrue(CompanionBowCombatSupport.isBowItemId("minecraft:bow"));
        assertTrue(CompanionBowCombatSupport.isBowItemId("minecraft:crossbow"));
        assertTrue(CompanionBowCombatSupport.isBowItemId("mod:iron_bow"));
        assertFalse(CompanionBowCombatSupport.isBowItemId("minecraft:bowl"));
        assertTrue(CompanionBowCombatSupport.isArrowItemId("minecraft:arrow"));
        assertTrue(CompanionBowCombatSupport.isArrowItemId("minecraft:spectral_arrow"));
        assertFalse(CompanionBowCombatSupport.isArrowItemId("minecraft:stick"));
    }

    @Test
    void onlyHumanoidFormsCanUseBows() {
        assertTrue(CompanionBowCombatSupport.formCanUseBow(CompanionForm.PLAYER));
        assertTrue(CompanionBowCombatSupport.formCanUseBow(CompanionForm.ZOMBIE));
        assertTrue(CompanionBowCombatSupport.formCanUseBow(CompanionForm.SKELETON));
        assertTrue(CompanionBowCombatSupport.formCanUseBow(CompanionForm.ENDERMAN));
        assertFalse(CompanionBowCombatSupport.formCanUseBow(CompanionForm.WOLF));
        assertFalse(CompanionBowCombatSupport.formCanUseBow(CompanionForm.CAT));
        assertFalse(CompanionBowCombatSupport.formCanUseBow(CompanionForm.FOX));
        assertFalse(CompanionBowCombatSupport.formCanUseBow(CompanionForm.CHICKEN));
        assertFalse(CompanionBowCombatSupport.formCanUseBow(CompanionForm.SPIDER));
        assertFalse(CompanionBowCombatSupport.formCanUseBow((CompanionForm) null));
    }

    @Test
    void preferRangedNeedsHumanoidPlusAmmoOrInfinity() {
        assertTrue(CompanionBowCombatSupport.shouldPreferRanged(true, true, true, false));
        assertTrue(CompanionBowCombatSupport.shouldPreferRanged(true, true, false, true));
        assertFalse(CompanionBowCombatSupport.shouldPreferRanged(false, true, true, true));
        assertFalse(CompanionBowCombatSupport.shouldPreferRanged(true, true, false, false));
        assertFalse(CompanionBowCombatSupport.shouldPreferRanged(true, false, true, true));
    }
}