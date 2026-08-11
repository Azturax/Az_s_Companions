package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompanionFollowDistancesTest {
    @Test
    void personalSpaceAndComfortBands() {
        assertTrue(CompanionFollowDistances.tooClose(1.5d));
        assertFalse(CompanionFollowDistances.tooClose(2.0d));
        assertTrue(CompanionFollowDistances.inComfortBand(2.0d));
        assertTrue(CompanionFollowDistances.inComfortBand(12.0d));
        assertFalse(CompanionFollowDistances.inComfortBand(12.1d));
        assertFalse(CompanionFollowDistances.needsFollow(10.0d));
        assertTrue(CompanionFollowDistances.needsFollow(10.1d));
        assertTrue(CompanionFollowDistances.FOLLOW_STOP >= CompanionFollowDistances.MIN_PERSONAL_SPACE);
        assertTrue(CompanionFollowDistances.FOLLOW_STOP <= CompanionFollowDistances.COMFORT_MAX);
        assertTrue(CompanionFollowDistances.TELEPORT_DISTANCE > CompanionFollowDistances.FOLLOW_START);
        assertTrue(CompanionFollowDistances.MIN_TELEPORT_DISTANCE < CompanionFollowDistances.TELEPORT_DISTANCE);
        assertTrue(CompanionFollowDistances.tooCloseToTeleport(8.0d));
        assertTrue(CompanionFollowDistances.tooCloseToTeleport(23.9d));
        assertFalse(CompanionFollowDistances.tooCloseToTeleport(24.0d));
        assertFalse(CompanionFollowDistances.shouldGroundTeleport(47.0d));
        assertTrue(CompanionFollowDistances.shouldGroundTeleport(48.0d));
        assertTrue(CompanionFollowDistances.withinHomeBedRadius(35.0d));
        assertFalse(CompanionFollowDistances.withinHomeBedRadius(35.1d));
        assertTrue(CompanionFollowDistances.beyondHomeBedRadius(35.1d));
        assertTrue(CompanionFollowDistances.HOME_BED_RADIUS == CompanionFollowDistances.LEAVE_BED_OWNER_DISTANCE);
    }

    @Test
    void clampsAndPerCompanionBands() {
        assertEquals(1.0f, CompanionFollowDistances.clampFollowRadius(0.0f), 0.001f);
        assertEquals(128.0f, CompanionFollowDistances.clampFollowRadius(999.0f), 0.001f);
        assertEquals(48.0f, CompanionFollowDistances.clampFollowRadius(48.0f), 0.001f);
        assertEquals(1.0f, CompanionFollowDistances.clampPersonalSpace(0.1f), 0.001f);
        assertEquals(12.0f, CompanionFollowDistances.clampPersonalSpace(99.0f), 0.001f);
        assertEquals(3.0f, CompanionFollowDistances.clampWanderRadius(1.0f), 0.001f);
        assertEquals(128.0f, CompanionFollowDistances.clampWanderRadius(200.0f), 0.001f);
        assertEquals(48.0f, CompanionFollowDistances.clampWanderRadius(48.0f), 0.001f);

        assertTrue(CompanionFollowDistances.tooClose(1.5d, 2.0d));
        assertFalse(CompanionFollowDistances.tooClose(3.0d, 2.0d));
        assertTrue(CompanionFollowDistances.needsFollow(11.0d, 2.0d, 48.0d));
        assertFalse(CompanionFollowDistances.needsFollow(5.0d, 2.0d, 48.0d));
        assertTrue(CompanionFollowDistances.shouldGroundTeleport(48.0d, 48.0d));
        assertFalse(CompanionFollowDistances.shouldGroundTeleport(47.0d, 48.0d));
        assertTrue(CompanionFollowDistances.shouldGroundTeleport(128.0d, 128.0d));
        assertTrue(CompanionFollowDistances.tooCloseToTeleport(10.0d, 48.0d));
        assertFalse(CompanionFollowDistances.tooCloseToTeleport(10.0d, 10.0d));

        float inherited = CompanionFollowDistances.inheritFollowRadius(48.0f);
        assertEquals(36.0f, inherited, 0.001f);
        assertTrue(inherited < 48.0f);
    }

    @Test
    void wanderAlwaysAtLeastFollow() {
        assertTrue(CompanionFollowDistances.DEFAULT_WANDER_RADIUS
                >= CompanionFollowDistances.DEFAULT_FOLLOW_RADIUS);
        assertEquals(CompanionFollowDistances.FOLLOW_RADIUS_MAX,
                CompanionFollowDistances.WANDER_RADIUS_MAX, 0.001f);

        // Wander below follow → raised to follow.
        assertEquals(64.0f, CompanionFollowDistances.clampWanderRadius(16.0f, 64.0f), 0.001f);
        // Wander already above follow → kept.
        assertEquals(80.0f, CompanionFollowDistances.clampWanderRadius(80.0f, 64.0f), 0.001f);
        // Both max out at 128.
        assertEquals(128.0f, CompanionFollowDistances.clampWanderRadius(999.0f, 999.0f), 0.001f);
        // Tiny follow still respects wander min.
        assertEquals(3.0f, CompanionFollowDistances.clampWanderRadius(1.0f, 1.0f), 0.001f);

        float[] pair = CompanionFollowDistances.clampFollowAndWander(96.0f, 12.0f);
        assertEquals(96.0f, pair[0], 0.001f);
        assertEquals(96.0f, pair[1], 0.001f);

        float childFollow = CompanionFollowDistances.inheritFollowRadius(64.0f);
        float childWander = CompanionFollowDistances.inheritWanderRadius(64.0f, childFollow);
        assertTrue(childWander >= childFollow);
    }
}
