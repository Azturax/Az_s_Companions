package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompanionSwimFollowSupportTest {
    @Test
    void engageAndShoreKeep() {
        assertTrue(CompanionSwimFollowSupport.useDirectSwimControl(true, true));
        assertFalse(CompanionSwimFollowSupport.useDirectSwimControl(true, false));
        assertFalse(CompanionSwimFollowSupport.useDirectSwimControl(false, true));
        assertTrue(CompanionSwimFollowSupport.keepGoalWhileOwnerWet(true, false));
        assertFalse(CompanionSwimFollowSupport.keepGoalWhileOwnerWet(true, true));
        assertFalse(CompanionSwimFollowSupport.keepGoalWhileOwnerWet(false, false));
    }

    @Test
    void preferredRingMatchesOwnerDepth() {
        double[] t = CompanionSwimFollowSupport.preferredSwimTarget(0, 62, 0, 10, 0, 4.0d);
        assertEquals(4.0d, Math.sqrt(t[0] * t[0] + t[2] * t[2]), 0.001d);
        assertEquals(62.0d, t[1], 0.001d);
    }

    @Test
    void velocityScalesAndArrives() {
        assertEquals(CompanionSwimFollowSupport.SWIM_SPEED_FAR, CompanionSwimFollowSupport.speedForDistance(12.0d), 0.001d);
        assertEquals(CompanionSwimFollowSupport.HOLD_SPEED, CompanionSwimFollowSupport.speedForDistance(0.1d), 0.001d);
        double[] v = CompanionSwimFollowSupport.velocityToward(0, 0, 0, 10, 0, 0, 0.5d);
        assertEquals(0.5d, v[0], 0.001d);
        assertEquals(0.0d, v[1], 0.001d);
        double[] arrived = CompanionSwimFollowSupport.velocityToward(0, 0, 0, 0.05d, 0, 0, 0.5d);
        assertEquals(0.0d, arrived[0], 0.001d);
    }
}
