package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompanionHazardImmunityTest {
    @Test
    void ignoresListedHazards() {
        assertTrue(CompanionHazardImmunity.ignores("fall"));
        assertTrue(CompanionHazardImmunity.ignores("cactus"));
        assertTrue(CompanionHazardImmunity.ignores("sweet_berry_bush"));
        assertTrue(CompanionHazardImmunity.ignores("drown"));
        assertTrue(CompanionHazardImmunity.ignores("in_wall"));
        assertTrue(CompanionHazardImmunity.ignores("campfire"));
        assertTrue(CompanionHazardImmunity.ignores("lava"));
        assertTrue(CompanionHazardImmunity.ignores("in_fire"));
        assertTrue(CompanionHazardImmunity.ignores("on_fire"));
        assertTrue(CompanionHazardImmunity.ignores("hot_floor"));
        assertTrue(CompanionHazardImmunity.ignores("freeze"));
        assertTrue(CompanionHazardImmunity.ignores("cramming"));
        assertTrue(CompanionHazardImmunity.ignores("out_of_world"));
    }

    @Test
    void keepsCombatDamage() {
        assertFalse(CompanionHazardImmunity.ignores("generic"));
        assertFalse(CompanionHazardImmunity.ignores("mob_attack"));
        assertFalse(CompanionHazardImmunity.ignores("player_attack"));
        assertFalse(CompanionHazardImmunity.ignores("arrow"));
        assertFalse(CompanionHazardImmunity.ignores("magic"));
        assertFalse(CompanionHazardImmunity.ignores("player_explosion"));
    }
}
