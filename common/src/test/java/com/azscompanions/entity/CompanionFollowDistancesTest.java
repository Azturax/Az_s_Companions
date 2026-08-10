package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

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
        assertTrue(CompanionFollowDistances.withinHomeBedRadius(35.0d));
        assertFalse(CompanionFollowDistances.withinHomeBedRadius(35.1d));
        assertTrue(CompanionFollowDistances.beyondHomeBedRadius(35.1d));
        assertTrue(CompanionFollowDistances.HOME_BED_RADIUS == CompanionFollowDistances.LEAVE_BED_OWNER_DISTANCE);
    }
}
