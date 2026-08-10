package com.azscompanions.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompanionChunkLoadingTest {
    private static final UUID OWNER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PARENT = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID CHILD = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @AfterEach
    void tearDown() {
        CompanionChunkLoading.clearAll();
    }

    @Test
    void parentAndChildCountTowardSameCap() {
        assertTrue(CompanionChunkLoading.tryAcquire(OWNER, PARENT, 2));
        assertTrue(CompanionChunkLoading.tryAcquire(OWNER, CHILD, 2));
        assertEquals(2, CompanionChunkLoading.heldCount(OWNER));
        assertFalse(CompanionChunkLoading.tryAcquire(OWNER, UUID.randomUUID(), 2));
        CompanionChunkLoading.release(OWNER, CHILD);
        assertEquals(1, CompanionChunkLoading.heldCount(OWNER));
        assertTrue(CompanionChunkLoading.tryAcquire(OWNER, UUID.randomUUID(), 2));
    }

    @Test
    void reacquireIsIdempotent() {
        assertTrue(CompanionChunkLoading.tryAcquire(OWNER, PARENT, 1));
        assertTrue(CompanionChunkLoading.tryAcquire(OWNER, PARENT, 1));
        assertEquals(1, CompanionChunkLoading.heldCount(OWNER));
    }

    @Test
    void clampMaxForcedChunks() {
        assertEquals(1, CompanionChunkLoading.clampMaxForcedChunks(0));
        assertEquals(64, CompanionChunkLoading.clampMaxForcedChunks(999));
        assertEquals(16, CompanionChunkLoading.clampMaxForcedChunks(16));
    }
}
