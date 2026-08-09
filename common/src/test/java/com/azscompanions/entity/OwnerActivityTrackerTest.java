package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OwnerActivityTrackerTest {
    @Test
    void becomesIdleAfterStillPeriod() {
        OwnerActivityTracker tracker = new OwnerActivityTracker();
        assertFalse(tracker.isStandingAround());
        // First tick only seeds position; need IDLE_TICKS still samples after that.
        for (int i = 0; i < OwnerActivityTracker.IDLE_TICKS + 1; i++) {
            tracker.tick(10.0d, 10.0d);
        }
        assertTrue(tracker.isStandingAround());
        assertFalse(tracker.isExploring());
    }

    @Test
    void movementClearsIdle() {
        OwnerActivityTracker tracker = new OwnerActivityTracker();
        for (int i = 0; i < OwnerActivityTracker.IDLE_TICKS + 1; i++) {
            tracker.tick(0.0d, 0.0d);
        }
        assertTrue(tracker.isStandingAround());
        tracker.tick(1.0d, 0.0d);
        assertTrue(tracker.isExploring());
        assertFalse(tracker.isStandingAround());
    }
}
