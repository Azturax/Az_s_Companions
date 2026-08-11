package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionOrbEvilLightningSupportTest {
    @Test
    void graceBlocksEarlyPlayerStrikes() {
        assertFalse(CompanionOrbEvilLightningSupport.shouldTargetPlayer(0, 0.0d));
        assertFalse(CompanionOrbEvilLightningSupport.shouldTargetPlayer(59, 0.0d));
        assertTrue(CompanionOrbEvilLightningSupport.shouldTargetPlayer(60, 0.0d));
        assertFalse(CompanionOrbEvilLightningSupport.shouldTargetPlayer(120, 0.99d));
    }

    @Test
    void periodicPulseOnInterval() {
        assertTrue(CompanionOrbEvilLightningSupport.shouldPeriodicPulse(
                CompanionOrbEvilLightningSupport.PERIODIC_INTERVAL_TICKS));
        assertFalse(CompanionOrbEvilLightningSupport.shouldPeriodicPulse(1));
        assertFalse(CompanionOrbEvilLightningSupport.shouldPeriodicPulse(0));
    }

    @Test
    void nearbyOffsetWithinRadius() {
        double[] o = CompanionOrbEvilLightningSupport.nearbyOffset(42L, 5.5d);
        double len = Math.sqrt(o[0] * o[0] + o[1] * o[1]);
        assertTrue(len <= 5.5d + 1.0e-6d);
        double[] near = CompanionOrbEvilLightningSupport.playerNearOffset(99L);
        double nlen = Math.sqrt(near[0] * near[0] + near[1] * near[1]);
        assertTrue(nlen <= CompanionOrbEvilLightningSupport.PLAYER_NEAR_RADIUS + 1.0e-6d);
    }

    @Test
    void elapsedEvilTicks() {
        assertEquals(40, CompanionOrbEvilLightningSupport.elapsedEvilTicks(200, 160));
        assertEquals(0, CompanionOrbEvilLightningSupport.elapsedEvilTicks(100, 150));
    }
}
