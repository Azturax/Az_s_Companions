package com.azscompanions.entity;

import com.azscompanions.AzsCompanionsConstants;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompanionPlayerPersistenceTest {
    private static final UUID PLAYER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BIT = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void konAndBitsUseDistinctKeys() {
        String kon = CompanionPlayerPersistence.companionKey(
                false, PLAYER, CompanionPlayerPersistence.KEY_KON, "Kon", false);
        String bit = CompanionPlayerPersistence.companionKey(
                true, BIT, CompanionPlayerPersistence.KEY_KON, CompanionChildLimits.DEFAULT_NAME, false);
        assertEquals(CompanionPlayerPersistence.KEY_KON, kon);
        assertEquals(CompanionPlayerPersistence.KEY_BIT_PREFIX + BIT, bit);
        assertTrue(CompanionPlayerPersistence.isBitKey(bit));
        assertFalse(CompanionPlayerPersistence.isBitKey(kon));
        assertFalse(kon.equals(bit));
    }

    @Test
    void wolfyAndPeckerDoNotShareKonInventoryKey() {
        String kon = CompanionPlayerPersistence.companionKey(
                false, PLAYER, CompanionPlayerPersistence.KEY_KON, "Kon", false);
        String wolfy = CompanionPlayerPersistence.companionKey(
                false, PLAYER, CompanionPlayerPersistence.KEY_KON,
                AzsCompanionsConstants.WOLFY_COMPANION_NAME, true);
        String pecker = CompanionPlayerPersistence.companionKey(
                false, PLAYER, CompanionPlayerPersistence.KEY_KON,
                AzsCompanionsConstants.PECKER_COMPANION_NAME, false);
        assertEquals(CompanionPlayerPersistence.KEY_KON, kon);
        assertEquals(CompanionPlayerPersistence.KEY_WOLFY, wolfy);
        assertEquals(CompanionPlayerPersistence.KEY_PECKER, pecker);
    }

    @Test
    void payloadIncludesModeAndInventory() {
        boolean mode = false;
        boolean inventory = false;
        for (String key : CompanionPlayerPersistence.PAYLOAD_NBT_KEYS) {
            if ("Mode".equals(key)) {
                mode = true;
            }
            if ("Inventory".equals(key)) {
                inventory = true;
            }
        }
        assertTrue(mode);
        assertTrue(inventory);
    }

    @Test
    void teamfightLeadersAreNotPersistedButBitsAre() {
        assertFalse(CompanionPlayerPersistence.shouldPersist(false, true, false));
        assertTrue(CompanionPlayerPersistence.shouldPersist(false, true, true));
        assertTrue(CompanionPlayerPersistence.shouldPersist(false, false, false));
        assertFalse(CompanionPlayerPersistence.shouldPersist(true, false, false));
        assertFalse(CompanionPlayerPersistence.shouldPersist(false, false, false, true));
        assertFalse(CompanionPlayerPersistence.shouldPersist(false, true, true, true));
    }

    @Test
    void emptyStoreMustNotWipeLoadedInventory() {
        assertFalse(CompanionPlayerPersistence.shouldApplyInventory(true, false));
        assertTrue(CompanionPlayerPersistence.shouldApplyInventory(false, false));
        assertTrue(CompanionPlayerPersistence.shouldApplyInventory(false, true));
        assertTrue(CompanionPlayerPersistence.shouldApplyInventory(true, true));
    }

    @Test
    void restoreKeepsSnapshotModeWhenPresent() {
        assertTrue(CompanionPlayerPersistence.snapshotHasMode(true));
        assertFalse(CompanionPlayerPersistence.snapshotHasMode(false));
    }
}
