package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionMobBehaviorSupportTest {
    @Test
    void onlyCatFormScaresCreepers() {
        assertTrue(CompanionMobBehaviorSupport.formScaresCreepers(CompanionForm.CAT));
        assertTrue(CompanionMobBehaviorSupport.formScaresCreepers("cat"));
        assertFalse(CompanionMobBehaviorSupport.formScaresCreepers(CompanionForm.PLAYER));
        assertFalse(CompanionMobBehaviorSupport.formScaresCreepers(CompanionForm.WOLF));
        assertFalse(CompanionMobBehaviorSupport.formScaresCreepers(CompanionForm.FOX));
        assertFalse(CompanionMobBehaviorSupport.formScaresCreepers(""));
    }

    @Test
    void onlyWolfFormScaresSkeletons() {
        assertTrue(CompanionMobBehaviorSupport.formScaresSkeletons(CompanionForm.WOLF));
        assertTrue(CompanionMobBehaviorSupport.formScaresSkeletons("wolf"));
        assertFalse(CompanionMobBehaviorSupport.formScaresSkeletons(CompanionForm.CAT));
        assertFalse(CompanionMobBehaviorSupport.formScaresSkeletons(CompanionForm.PLAYER));
        assertFalse(CompanionMobBehaviorSupport.formScaresSkeletons(CompanionForm.FOX));
        assertFalse(CompanionMobBehaviorSupport.formScaresSkeletons(""));
        assertEquals(
                CompanionMobBehaviorSupport.CREEPER_SCARE_DISTANCE,
                CompanionMobBehaviorSupport.SKELETON_SCARE_DISTANCE);
    }

    @Test
    void wanderInteractGatesOnModeAndState() {
        assertTrue(CompanionMobBehaviorSupport.canStartWanderInteract(true, false, false, false, 0));
        assertFalse(CompanionMobBehaviorSupport.canStartWanderInteract(false, false, false, false, 0));
        assertFalse(CompanionMobBehaviorSupport.canStartWanderInteract(true, true, false, false, 0));
        assertFalse(CompanionMobBehaviorSupport.canStartWanderInteract(true, false, true, false, 0));
        assertFalse(CompanionMobBehaviorSupport.canStartWanderInteract(true, false, false, true, 0));
        assertFalse(CompanionMobBehaviorSupport.canStartWanderInteract(true, false, false, false, 5));
    }

    @Test
    void targetFilterRejectsPlayersCompanionsBosses() {
        assertTrue(CompanionMobBehaviorSupport.isValidInteractTarget(true, false, false, false, false));
        assertFalse(CompanionMobBehaviorSupport.isValidInteractTarget(false, false, false, false, false));
        assertFalse(CompanionMobBehaviorSupport.isValidInteractTarget(true, true, false, false, false));
        assertFalse(CompanionMobBehaviorSupport.isValidInteractTarget(true, false, true, false, false));
        assertFalse(CompanionMobBehaviorSupport.isValidInteractTarget(true, false, false, true, false));
        assertFalse(CompanionMobBehaviorSupport.isValidInteractTarget(true, false, false, false, true));
    }

    @Test
    void bossIdsRecognized() {
        assertTrue(CompanionMobBehaviorSupport.isBossLikeEntityId("minecraft:wither"));
        assertTrue(CompanionMobBehaviorSupport.isBossLikeEntityId("minecraft:warden"));
        assertFalse(CompanionMobBehaviorSupport.isBossLikeEntityId("minecraft:creeper"));
        assertFalse(CompanionMobBehaviorSupport.isBossLikeEntityId("minecraft:cow"));
    }

    @Test
    void pickKindCoversAllActions() {
        assertEquals(CompanionMobBehaviorSupport.WanderInteractKind.CIRCLE,
                CompanionMobBehaviorSupport.pickKind(0));
        assertEquals(CompanionMobBehaviorSupport.WanderInteractKind.SNEAK,
                CompanionMobBehaviorSupport.pickKind(40));
        assertEquals(CompanionMobBehaviorSupport.WanderInteractKind.PUSH,
                CompanionMobBehaviorSupport.pickKind(65));
        assertEquals(CompanionMobBehaviorSupport.WanderInteractKind.PUNCH,
                CompanionMobBehaviorSupport.pickKind(85));
    }

    @Test
    void punchDamageRequiresCombatFlag() {
        assertTrue(CompanionMobBehaviorSupport.punchDealsDamage(true));
        assertFalse(CompanionMobBehaviorSupport.punchDealsDamage(false));
    }
}
