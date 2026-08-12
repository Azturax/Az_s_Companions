package com.azscompanions.deposit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DepositChestSelectionTest {
    @AfterEach
    void tearDown() {
        DepositChestSelection.clearAll();
    }

    @Test
    void toggleMultiSelectAndNearest() {
        UUID id = UUID.randomUUID();
        DepositChestSelection sel = DepositChestSelection.of(id);
        sel.enableSelecting();
        assertTrue(sel.toggle("minecraft:overworld", 1, 64, 1));
        assertTrue(sel.toggle("minecraft:overworld", 10, 64, 10));
        assertEquals(2, sel.size());
        assertFalse(sel.toggle("minecraft:overworld", 1, 64, 1)); // remove
        assertEquals(1, sel.size());

        sel.finishKeepingSelection();
        assertFalse(sel.isSelecting());
        assertEquals(1, sel.size());

        assertEquals(10, sel.nearestInDimension("minecraft:overworld", 9, 64, 9).orElseThrow().x());
    }

    @Test
    void snapshotRoundTrip() {
        DepositChestSelection sel = DepositChestSelection.of(UUID.randomUUID());
        sel.enableSelecting();
        sel.toggle("minecraft:overworld", 2, 70, 3);
        String encoded = sel.snapshot().encode();
        DepositSelectionSnapshot decoded = DepositSelectionSnapshot.decode(encoded);
        assertTrue(decoded.selecting());
        assertEquals(1, decoded.chests().size());
        assertEquals(2, decoded.chests().get(0).x());
    }
}
