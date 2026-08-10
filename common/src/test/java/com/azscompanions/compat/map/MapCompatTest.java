package com.azscompanions.compat.map;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapCompatTest {
    @AfterEach
    void reset() {
        MapCompat.applySettings(new MapCompatSettings());
    }

    @Test
    void shouldShowRespectsMasterAndChildrenToggles() {
        assertTrue(MapCompat.shouldShowOnMap(false));
        assertTrue(MapCompat.shouldShowOnMap(true));

        MapCompatSettings hideAll = new MapCompatSettings();
        hideAll.setShowOnMinimap(false);
        MapCompat.applySettings(hideAll);
        assertFalse(MapCompat.shouldShowOnMap(false));
        assertFalse(MapCompat.shouldShowOnMap(true));

        MapCompatSettings hideKids = new MapCompatSettings();
        hideKids.setShowChildrenOnMap(false);
        MapCompat.applySettings(hideKids);
        assertTrue(MapCompat.shouldShowOnMap(false));
        assertFalse(MapCompat.shouldShowOnMap(true));
    }

    @Test
    void jsonRoundTrip() {
        MapCompatSettings s = new MapCompatSettings();
        s.setShowOnMinimap(false);
        s.setShowChildrenOnMap(false);
        s.setShowNameOnMap(false);
        s.setShowOwnerOnMap(false);
        s.setIconColorArgb(0xFF112233);
        JsonObject json = MapCompatConfigIO.toJson(s);
        MapCompatSettings loaded = MapCompatConfigIO.fromJson(json);
        assertFalse(loaded.showOnMinimap());
        assertFalse(loaded.showChildrenOnMap());
        assertFalse(loaded.showNameOnMap());
        assertFalse(loaded.showOwnerOnMap());
        assertEquals(0xFF112233, loaded.iconColorArgb());
    }

    @Test
    void parseArgbVariants() {
        assertEquals(0xFFE91E8C, MapCompatConfigIO.parseArgb("0xFFE91E8C", 0));
        assertEquals(0xFFFF0000, MapCompatConfigIO.parseArgb("#FF0000", 0));
        assertEquals(0xAABBCCDD, MapCompatConfigIO.parseArgb("#AABBCCDD", 0));
        assertEquals(42, MapCompatConfigIO.parseArgb("nope", 42));
    }

    @Test
    void companionDetectionByClassName() {
        assertTrue(CompanionMapEntity.isCompanion(new FakeCompanionEntity()));
        assertFalse(CompanionMapEntity.isCompanion(new Object()));
        assertTrue(CompanionMapEntity.isChildCompanion(new FakeCompanionEntity()));
        assertEquals("Kon", CompanionMapEntity.displayName(new FakeCompanionEntity()));
    }

    /** Mimics loader companion class naming for reflection helpers. */
    public static final class FakeCompanionEntity {
        public boolean isChildCompanion() {
            return true;
        }

        public String getChatDisplayName() {
            return "Kon";
        }
    }
}
