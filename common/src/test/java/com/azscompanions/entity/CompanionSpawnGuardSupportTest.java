package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompanionSpawnGuardSupportTest {
    @Test
    void loginGraceCoversJoinTicks() {
        assertTrue(CompanionSpawnGuardSupport.inLoginGrace(0));
        assertTrue(CompanionSpawnGuardSupport.inLoginGrace(79));
        assertFalse(CompanionSpawnGuardSupport.inLoginGrace(80));
        assertFalse(CompanionSpawnGuardSupport.inLoginGrace(200));
    }

    @Test
    void reuseExistingLivingUuid() {
        assertTrue(CompanionSpawnGuardSupport.shouldReuseExisting(true));
        assertFalse(CompanionSpawnGuardSupport.shouldReuseExisting(false));
    }

    @Test
    void pickPrefersCharmBoundThenClosest() {
        UUID bound = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID other = UUID.fromString("22222222-2222-2222-2222-222222222222");
        record C(UUID id, double d) {
        }
        C keep = new C(bound, 99.0d);
        C close = new C(other, 1.0d);
        assertSame(keep, CompanionSpawnGuardSupport.pickPrimaryToKeep(
                List.of(close, keep), bound, C::id, C::d));
        assertSame(close, CompanionSpawnGuardSupport.pickPrimaryToKeep(
                List.of(close, keep), null, C::id, C::d));
    }

    @Test
    void safeOffsetsNeverIncludeOrigin() {
        for (int[] off : CompanionSafeTeleportSupport.horizontalOffsets(6.0d)) {
            assertFalse(off[0] == 0 && off[1] == 0, "offset inside owner");
        }
        double[] behind = CompanionSafeTeleportSupport.behindOwner(0.0f, 2.5d);
        assertTrue(CompanionSafeTeleportSupport.tooCloseToOwner(0.0d, 0.0d, 2.0d));
        assertFalse(CompanionSafeTeleportSupport.tooCloseToOwner(behind[0], behind[1], 2.0d));
        assertEquals(2.5d, Math.hypot(behind[0], behind[1]), 1.0e-6);
    }
}
