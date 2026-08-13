package com.azscompanions.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionLuckSupportTest {
    @AfterEach
    void tearDown() {
        CompanionLuckSupport.setLuckAffectsCompanion(CompanionLuckSupport.DEFAULT_LUCK_AFFECTS_COMPANION);
    }

    @Test
    void defaultsDisabledToAvoidArtifactSpam() {
        assertFalse(CompanionLuckSupport.DEFAULT_LUCK_AFFECTS_COMPANION);
        CompanionLuckSupport.setLuckAffectsCompanion(CompanionLuckSupport.DEFAULT_LUCK_AFFECTS_COMPANION);
        assertFalse(CompanionLuckSupport.luckAffectsCompanion());
    }

    @Test
    void detectsLuckEffectIds() {
        assertTrue(CompanionLuckSupport.isLuckEffectId("minecraft:luck"));
        assertTrue(CompanionLuckSupport.isLuckEffectId("minecraft:unluck"));
        assertTrue(CompanionLuckSupport.isLuckEffectId("unluck"));
        assertFalse(CompanionLuckSupport.isLuckEffectId("minecraft:speed"));
    }
}
