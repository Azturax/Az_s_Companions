package com.azscompanions.perk;

import com.azscompanions.AzsCompanionsConstants;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WigglyDogPerkSupportTest {
    @Test
    void eligibleMatchesSpecialPerkUuid() {
        assertTrue(WigglyDogPerkSupport.isEligible(AzsCompanionsConstants.SPECIAL_PERK_PLAYER_UUID));
        assertFalse(WigglyDogPerkSupport.isEligible(AzsCompanionsConstants.WOLFY_PLAYER_UUID));
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

    @Test
    void defaultOffAndHardCap() {
        assertFalse(WigglyDogPerkSupport.DEFAULT_VISIBLE);
        assertEquals(1, WigglyDogPerkSupport.MAX_OWNED_DOGS);
    }

    @Test
    void shownFromTagsRequiresExplicitShown() {
        assertFalse(WigglyDogPerkSupport.isShownFromTags(Set.of()));
        assertFalse(WigglyDogPerkSupport.isShownFromTags(null));
        assertTrue(WigglyDogPerkSupport.isShownFromTags(Set.of(WigglyDogPerkSupport.PLAYER_SHOWN_TAG)));
        assertFalse(WigglyDogPerkSupport.isShownFromTags(Set.of(WigglyDogPerkSupport.PLAYER_HIDDEN_TAG)));
        assertFalse(WigglyDogPerkSupport.isShownFromTags(Set.of(
                WigglyDogPerkSupport.PLAYER_SHOWN_TAG,
                WigglyDogPerkSupport.PLAYER_HIDDEN_TAG)));
    }

    @Test
    void shownFromPersistentFlagsDefaultOff() {
        assertFalse(WigglyDogPerkSupport.isShownFromPersistentFlags(false, false, false, false));
        assertTrue(WigglyDogPerkSupport.isShownFromPersistentFlags(true, true, false, false));
        assertFalse(WigglyDogPerkSupport.isShownFromPersistentFlags(true, false, false, false));
        assertFalse(WigglyDogPerkSupport.isShownFromPersistentFlags(true, true, true, true));
        assertFalse(WigglyDogPerkSupport.isShownFromPersistentFlags(false, false, true, true));
    }

    @Test
    void pickOneToKeepChoosesClosest() {
        assertNull(WigglyDogPerkSupport.pickOneToKeep(List.of(), x -> 0.0d));
        String a = "near";
        String b = "far";
        String c = "mid";
        assertSame(a, WigglyDogPerkSupport.pickOneToKeep(List.of(b, a, c), s -> switch (s) {
            case "near" -> 1.0d;
            case "mid" -> 4.0d;
            default -> 9.0d;
        }));
    }
}
