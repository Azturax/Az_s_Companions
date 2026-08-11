package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionFormTest {
    @Test
    void removedGlowingOrbAliasesMigrateToPlayer() {
        assertEquals(CompanionForm.PLAYER, CompanionForm.byName("glowing_orb"));
        assertEquals(CompanionForm.PLAYER, CompanionForm.byName("orb"));
        assertEquals(CompanionForm.PLAYER, CompanionForm.byName("GLOWING_ORB"));
        assertFalse(java.util.Arrays.asList(CompanionForm.values()).stream()
                .anyMatch(f -> f.name().contains("ORB")));
    }

    @Test
    void passengerSitPoseIsPlayerAndHumanoidHostiles() {
        assertTrue(CompanionForm.PLAYER.usesPassengerSitPose());
        assertTrue(CompanionForm.ZOMBIE.usesPassengerSitPose());
        assertTrue(CompanionForm.SKELETON.usesPassengerSitPose());
        assertTrue(CompanionForm.HUSK.usesPassengerSitPose());
        assertTrue(CompanionForm.STRAY.usesPassengerSitPose());
        assertTrue(CompanionForm.ENDERMAN.usesPassengerSitPose());
        assertFalse(CompanionForm.SPIDER.usesPassengerSitPose());
        assertFalse(CompanionForm.WOLF.usesPassengerSitPose());
        assertFalse(CompanionForm.CHICKEN.usesPassengerSitPose());
    }

    @Test
    void nativeAnimalSitPoseIsWolfCatFox() {
        assertTrue(CompanionForm.WOLF.usesNativeAnimalSitPose());
        assertTrue(CompanionForm.CAT.usesNativeAnimalSitPose());
        assertTrue(CompanionForm.FOX.usesNativeAnimalSitPose());
        assertFalse(CompanionForm.SHEEP.usesNativeAnimalSitPose());
        assertFalse(CompanionForm.PLAYER.usesNativeAnimalSitPose());
        assertTrue(CompanionSitPose.hasVisualSitPose(CompanionForm.PLAYER));
        assertTrue(CompanionSitPose.hasVisualSitPose(CompanionForm.WOLF));
        assertFalse(CompanionSitPose.hasVisualSitPose(CompanionForm.COW));
    }
}
