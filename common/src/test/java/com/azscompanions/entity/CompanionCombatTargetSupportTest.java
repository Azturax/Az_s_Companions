package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompanionCombatTargetSupportTest {
    @Test
    void passiveKonAggrosMonstersNotPlayers() {
        assertTrue(CompanionCombatTargetSupport.wantsCombatTargets());
        assertTrue(CompanionCombatTargetSupport.isValidHostilePrey(
                true, false, false, false, false, false));
        assertFalse(CompanionCombatTargetSupport.isValidHostilePrey(
                true, false, false, false, true, false));
        assertFalse(CompanionCombatTargetSupport.isValidHostilePrey(
                true, false, false, false, false, true));
    }

    @Test
    void hostileAttitudeMayTargetPlayers() {
        assertTrue(CompanionCombatTargetSupport.isValidHostilePrey(
                true, false, false, true, false, true));
    }

    @Test
    void teamRivalAlwaysValid() {
        assertTrue(CompanionCombatTargetSupport.isValidHostilePrey(
                true, false, true, false, true, false));
    }

    @Test
    void acquiredHostileCanBeAttackedWhenPassive() {
        assertTrue(CompanionCombatTargetSupport.canAttackAcquiredTarget(
                true, true, false, false, false, false, true, false));
        assertFalse(CompanionCombatTargetSupport.canAttackAcquiredTarget(
                true, true, false, false, false, true, true, false));
        assertTrue(CompanionCombatTargetSupport.canAttackAcquiredTarget(
                true, true, false, false, false, true, true, true));
    }
}
