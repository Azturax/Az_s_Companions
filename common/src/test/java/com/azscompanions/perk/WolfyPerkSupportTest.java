package com.azscompanions.perk;

import com.azscompanions.AzsCompanionsConstants;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WolfyPerkSupportTest {
    @Test
    void ownerUuidMatchesConstant() {
        assertTrue(WolfyPerkSupport.isWolfyOwner(AzsCompanionsConstants.WOLFY_PLAYER_UUID));
        assertTrue(AzsCompanionsConstants.isWolfyOwner(AzsCompanionsConstants.WOLFY_PLAYER_UUID));
        assertFalse(WolfyPerkSupport.isWolfyOwner(UUID.randomUUID()));
        assertFalse(WolfyPerkSupport.isWolfyOwner(null));
    }

    @Test
    void wolfyNameIsCaseInsensitive() {
        assertTrue(WolfyPerkSupport.isWolfyName("Wolfy"));
        assertTrue(WolfyPerkSupport.isWolfyName(" wolfy "));
        assertFalse(WolfyPerkSupport.isWolfyName("Wiggly"));
        assertFalse(WolfyPerkSupport.isWolfyName(""));
        assertFalse(WolfyPerkSupport.isWolfyName(null));
    }

    @Test
    void storedDetectionUsesFlagOrName() {
        assertTrue(WolfyPerkSupport.looksLikeStoredWolfy("Wolfy", false));
        assertTrue(WolfyPerkSupport.looksLikeStoredWolfy("Other", true));
        assertFalse(WolfyPerkSupport.looksLikeStoredWolfy("Kon", false));
    }

    @Test
    void brownVariantIsChestnut() {
        assertEquals("minecraft:chestnut", WolfyPerkSupport.BROWN_WOLF_VARIANT_ID);
    }
}
