package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompanionRideAlongSupportTest {
    @Test
    void kindsMatchSameBucketOnly() {
        assertTrue(CompanionRideAlongSupport.kindsMatch(
                CompanionRideAlongSupport.RideKind.HORSE,
                CompanionRideAlongSupport.RideKind.HORSE));
        assertFalse(CompanionRideAlongSupport.kindsMatch(
                CompanionRideAlongSupport.RideKind.HORSE,
                CompanionRideAlongSupport.RideKind.CAMEL));
        assertFalse(CompanionRideAlongSupport.kindsMatch(
                CompanionRideAlongSupport.RideKind.NONE,
                CompanionRideAlongSupport.RideKind.BOAT));
    }

    @Test
    void seekAndDismountGates() {
        assertTrue(CompanionRideAlongSupport.shouldSeek(true, true, false, false));
        assertFalse(CompanionRideAlongSupport.shouldSeek(false, true, false, false));
        assertFalse(CompanionRideAlongSupport.shouldSeek(true, false, false, false));
        assertFalse(CompanionRideAlongSupport.shouldSeek(true, true, true, false));
        assertFalse(CompanionRideAlongSupport.shouldSeek(true, true, false, true));
        assertTrue(CompanionRideAlongSupport.shouldSyncDismount(true, false));
        assertFalse(CompanionRideAlongSupport.shouldSyncDismount(true, true));
        assertFalse(CompanionRideAlongSupport.shouldSyncDismount(false, false));
    }

    @Test
    void preferredCandidateRejectsOwnerVehicleAndOthers() {
        assertTrue(CompanionRideAlongSupport.isPreferredCandidate(true, false, false));
        assertFalse(CompanionRideAlongSupport.isPreferredCandidate(false, false, false));
        assertFalse(CompanionRideAlongSupport.isPreferredCandidate(true, true, false));
        assertFalse(CompanionRideAlongSupport.isPreferredCandidate(true, false, true));
    }

    @Test
    void rangesAndCooldown() {
        assertTrue(CompanionRideAlongSupport.withinMountReach(2.0d * 2.0d));
        assertFalse(CompanionRideAlongSupport.withinMountReach(4.0d * 4.0d));
        assertTrue(CompanionRideAlongSupport.withinSearchRange(16.0d * 16.0d));
        assertFalse(CompanionRideAlongSupport.withinSearchRange(17.0d * 17.0d));
        assertTrue(CompanionRideAlongSupport.canAttempt(100, 100));
        assertFalse(CompanionRideAlongSupport.canAttempt(99, 100));
        assertEquals(180L, CompanionRideAlongSupport.nextFailCooldown(100));
    }

    @Test
    void steerHoldsOnRing() {
        double[] hold = CompanionRideAlongSupport.steerVelocity(
                4, 64, 0, 0, 64, 0, 4.0d, 0.5d);
        assertEquals(0.0d, hold[0], 0.05d);
        assertEquals(0.0d, hold[2], 0.05d);
        double[] move = CompanionRideAlongSupport.steerVelocity(
                20, 64, 0, 0, 64, 0, 4.0d, 0.5d);
        assertTrue(move[0] < 0.0d);
    }
}
