package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JindujunParticleSupportTest {
    @Test
    void shapeIsNonEmptyCrossLadder() {
        assertTrue(JindujunParticleSupport.shapePointCount() > 20);
        assertTrue(JindujunParticleSupport.shapeRows() >= 8);
        assertTrue(JindujunParticleSupport.shapeCols() >= 8);
    }

    @Test
    void onlySpawnsWhileRidden() {
        assertFalse(JindujunParticleSupport.shouldSpawn(false));
        assertTrue(JindujunParticleSupport.shouldSpawn(true));
    }

    @Test
    void denserWhenMoving() {
        int idle = JindujunParticleSupport.particlesThisTick(false, 0);
        int moving = JindujunParticleSupport.particlesThisTick(true, 0);
        assertTrue(moving >= idle);
        assertTrue(JindujunParticleSupport.movingFastEnough(0.2, 0, 0));
        assertFalse(JindujunParticleSupport.movingFastEnough(0.01, 0, 0));
    }

    @Test
    void worldOffsetRotatesBehindCloud() {
        float[] out = new float[3];
        // yaw 0: facing +Z, behind is -Z
        JindujunParticleSupport.worldOffset(0f, 0f, 1f, 0f, out);
        assertEquals(0f, out[0], 1.0e-4f);
        assertTrue(out[2] < 0f);
        // yaw 90: facing -X, behind is +X
        JindujunParticleSupport.worldOffset(0f, 0f, 1f, 90f, out);
        assertTrue(out[0] > 0f);
    }

    @Test
    void pointIndexWraps() {
        int n = JindujunParticleSupport.shapePointCount();
        assertEquals(0, JindujunParticleSupport.pointIndex(0, 0) % n);
        assertTrue(JindujunParticleSupport.pointIndex(999, 3) >= 0);
        assertTrue(JindujunParticleSupport.pointIndex(999, 3) < n);
    }

    @Test
    void scaleIsTwoPointFive() {
        assertEquals(2.5f, JindujunSupport.SCALE, 1.0e-4f);
        assertEquals(1.35f * 2.5f, JindujunSupport.WIDTH, 1.0e-4f);
        assertEquals(0.55f * 2.5f, JindujunSupport.HEIGHT, 1.0e-4f);
        // Seat slightly into mesh deck (~0.22×SCALE); 0.32×SCALE still left a visible air gap.
        assertEquals(0.22d * JindujunSupport.SCALE, JindujunSupport.RIDER_Y_OFFSET, 1.0e-4d);
        assertTrue(JindujunSupport.RIDER_Y_OFFSET < JindujunSupport.HEIGHT * 0.7d);
        assertEquals(10.0f, JindujunSupport.riderSteerYaw(370.0f), 1.0e-3f);
        assertEquals(-10.0f, JindujunSupport.riderSteerYaw(-370.0f), 1.0e-3f);
    }

    @Test
    void particleStreamStaysCompactAndSparse() {
        assertTrue(JindujunParticleSupport.PIXEL < 0.12f);
        assertTrue(JindujunParticleSupport.BEHIND > JindujunParticleSupport.SHAPE_CENTER_Y);
        int idle = JindujunParticleSupport.particlesThisTick(false, 0);
        int moving = JindujunParticleSupport.particlesThisTick(true, 0);
        assertTrue(idle <= JindujunParticleSupport.shapePointCount() / 10);
        assertTrue(moving < JindujunParticleSupport.shapePointCount() / 3);
        assertTrue(moving >= idle);
    }

    @Test
    void idleDespawnAfterFiftySixSeconds() {
        assertEquals(56 * 20, JindujunSupport.IDLE_DESPAWN_TICKS);
        assertEquals(0, JindujunSupport.nextIdleTicks(true, 100));
        assertEquals(101, JindujunSupport.nextIdleTicks(false, 100));
        assertFalse(JindujunSupport.shouldDespawnFromIdle(JindujunSupport.IDLE_DESPAWN_TICKS - 1));
        assertTrue(JindujunSupport.shouldDespawnFromIdle(JindujunSupport.IDLE_DESPAWN_TICKS));
    }
}
