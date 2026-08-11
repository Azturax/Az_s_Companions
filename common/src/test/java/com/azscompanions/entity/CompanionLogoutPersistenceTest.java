package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class CompanionLogoutPersistenceTest {
    @Test
    void keysAreStable() {
        assertEquals("azscompanions.LogoutCompanions", CompanionLogoutPersistence.PLAYER_LIST_TAG);
        assertEquals("Uuid", CompanionLogoutPersistence.ENTRY_UUID);
        assertEquals("Data", CompanionLogoutPersistence.ENTRY_DATA);
        assertEquals("LogoutParked", CompanionLogoutPersistence.CHARM_LOGOUT_PARKED);
        assertFalse(CompanionLogoutPersistence.PLAYER_LIST_TAG.isBlank());
    }
}
