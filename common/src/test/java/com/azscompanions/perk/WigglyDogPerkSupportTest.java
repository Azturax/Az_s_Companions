package com.azscompanions.perk;

import com.azscompanions.AzsCompanionsConstants;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WigglyDogPerkSupportTest {
    @Test
    void eligibleMatchesWolfyUuid() {
        assertTrue(WigglyDogPerkSupport.isEligible(AzsCompanionsConstants.WOLFY_PLAYER_UUID));
        assertFalse(WigglyDogPerkSupport.isEligible(AzsCompanionsConstants.SPECIAL_PERK_PLAYER_UUID));
        assertFalse(WigglyDogPerkSupport.isEligible(AzsCompanionsConstants.MISTER_WIGGLY_PLAYER_UUID));
        assertFalse(WigglyDogPerkSupport.isEligible(null));
        assertFalse(WigglyDogPerkSupport.isEligible(UUID.randomUUID()));
    }

    @Test
    void toggleName() {
        assertTrue(WigglyDogPerkSupport.isToggleDogName("Wiggly"));
        assertTrue(WigglyDogPerkSupport.isToggleDogName(" wiggly "));
        assertFalse(WigglyDogPerkSupport.isToggleDogName("Wolfy"));
        assertFalse(WigglyDogPerkSupport.isToggleDogName(null));
        assertEquals("Wiggly", AzsCompanionsConstants.TOGGLE_WIGGLY_DOG_NAME);
    }

    @Test
    void bobAndSitHelpers() {
        assertEquals(0.0d, WigglyDogFlightSupport.bobDeltaY(0), 1.0e-9);
        assertTrue(WigglyDogFlightSupport.shouldFlipSit(0));
        assertFalse(WigglyDogFlightSupport.shouldFlipSit(1));
        assertTrue(WigglyDogFlightSupport.shouldFlipSit(90));
    }
}
