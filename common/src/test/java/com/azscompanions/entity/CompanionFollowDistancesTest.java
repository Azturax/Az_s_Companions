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
        assertFalse(CompanionFollowDistances.needsFollow(32.0d));
        assertTrue(CompanionFollowDistances.needsFollow(32.1d));
        assertTrue(CompanionFollowDistances.FOLLOW_STOP >= CompanionFollowDistances.MIN_PERSONAL_SPACE);
        assertTrue(CompanionFollowDistances.FOLLOW_STOP <= CompanionFollowDistances.COMFORT_MAX);
        assertTrue(CompanionFollowDistances.IDLE_WANDER_MIN >= CompanionFollowDistances.COMFORT_MAX);
        assertTrue(CompanionFollowDistances.TELEPORT_DISTANCE > CompanionFollowDistances.FOLLOW_START);
    }
}
