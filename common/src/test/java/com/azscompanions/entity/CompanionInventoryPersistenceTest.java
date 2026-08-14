package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompanionInventoryPersistenceTest {
    @Test
    void defaultKeepsInventoryOnDeath() {
        assertTrue(CompanionInventoryPersistence.DEFAULT_KEEP_INVENTORY_ON_DEATH);
        assertTrue(CompanionInventoryPersistence.shouldKeepInventoryOnDeath(true));
        assertFalse(CompanionInventoryPersistence.shouldKeepInventoryOnDeath(false));
    }

    @Test
    void bitStoresOnlyOnLivingParent() {
        assertTrue(CompanionInventoryPersistence.shouldStoreBitOnParent(true, true, true));
        assertFalse(CompanionInventoryPersistence.shouldStoreBitOnParent(true, false, true));
        assertFalse(CompanionInventoryPersistence.shouldStoreBitOnParent(false, true, true));
        assertFalse(CompanionInventoryPersistence.shouldStoreBitOnParent(true, true, false));
    }
}
