package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompanionFlightFollowSupportTest {
    @Test
    void preferredRingMatchesHoverHeight() {
        double preferred = CompanionFollowDistances.preferredDistance(2.0d);
        assertEquals(3.5d, preferred, 0.001d);
        double[] t = CompanionFlightFollowSupport.preferredFlightTarget(0, 100, 0, 10, 0, preferred);
        assertEquals(preferred, Math.sqrt(t[0] * t[0] + t[2] * t[2]), 0.001d);
        assertEquals(100.0d + CompanionFlightFollowSupport.HOVER_Y, t[1], 0.001d);
    }

    @Test
    void flightSnapUsesLeashNotPersonalSpace() {
        assertFalse(CompanionFlightFollowSupport.shouldFlightSnap(5.0d, 48.0d));
        assertFalse(CompanionFlightFollowSupport.shouldFlightSnap(12.0d, 48.0d));
        assertFalse(CompanionFlightFollowSupport.shouldFlightSnap(47.9d, 48.0d));
        assertTrue(CompanionFlightFollowSupport.shouldFlightSnap(48.0d, 48.0d));
        assertFalse(CompanionFlightFollowSupport.shouldFlightSnap(15.0d, 16.0d));
        assertTrue(CompanionFlightFollowSupport.shouldFlightSnap(16.0d, 16.0d));
    }

    @Test
    void airWanderRespectsPersonalSpace() {
        double[] t = CompanionFlightFollowSupport.pickAirWanderTarget(
                0, 80, 0, 2.0d, 3.0d, 16.0d, 0.0d, 0.0d, 0.5d);
        double horiz = Math.sqrt(t[0] * t[0] + t[2] * t[2]);
        assertTrue(horiz >= 2.0d);
        assertEquals(80.0d + CompanionFlightFollowSupport.HOVER_Y, t[1], 0.001d);
    }

    @Test
    void beyondAirWanderUses3d() {
        assertFalse(CompanionFlightFollowSupport.beyondAirWanderRadius(
                5, 80.35d, 0, 0, 80, 0, 16.0d));
        assertTrue(CompanionFlightFollowSupport.beyondAirWanderRadius(
                30, 80.35d, 0, 0, 80, 0, 16.0d));
    }

    @Test
    void velocityScalesAndArrives() {
        assertEquals(
                CompanionFlightFollowSupport.FLIGHT_SPEED_FAR,
                CompanionFlightFollowSupport.speedForDistance(12.0d),
                0.001d);
        assertEquals(
                CompanionFlightFollowSupport.HOLD_SPEED,
                CompanionFlightFollowSupport.speedForDistance(0.1d),
                0.001d);
        double[] v = CompanionFlightFollowSupport.velocityToward(0, 0, 0, 10, 0, 0, 0.5d);
        assertEquals(0.5d, v[0], 0.001d);
        double[] arrived = CompanionFlightFollowSupport.velocityToward(0, 0, 0, 0.1d, 0, 0, 0.5d);
        assertEquals(0.0d, arrived[0], 0.001d);
    }
}
