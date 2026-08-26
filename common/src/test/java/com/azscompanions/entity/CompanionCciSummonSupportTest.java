package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompanionCciSummonSupportTest {
    @Test
    void durationZeroMeansNoExpiry() {
        assertEquals(0, CompanionCciSummonSupport.clampDurationSeconds(0));
        assertEquals(0, CompanionCciSummonSupport.clampDurationSeconds(-5));
        assertEquals(0L, CompanionCciSummonSupport.expireAtGameTime(1000L, 0));
        assertFalse(CompanionCciSummonSupport.shouldExpire(true, 0L, 99_999L));
    }

    @Test
    void finiteDurationExpiresAtGameTime() {
        assertEquals(90, CompanionCciSummonSupport.clampDurationSeconds(90));
        assertEquals(1000L + 90L * 20L, CompanionCciSummonSupport.expireAtGameTime(1000L, 90));
        assertFalse(CompanionCciSummonSupport.shouldExpire(true, 2000L, 1999L));
        assertTrue(CompanionCciSummonSupport.shouldExpire(true, 2000L, 2000L));
        assertFalse(CompanionCciSummonSupport.shouldExpire(false, 2000L, 5000L));
    }

    @Test
    void charmCompanionsAreParkedCciSummonsAreNot() {
        assertTrue(CompanionCciSummonSupport.shouldParkOnLogout(false));
        assertFalse(CompanionCciSummonSupport.shouldParkOnLogout(true));
    }

    @Test
    void typesMapToKonBitsAndWolf() {
        assertEquals("player", CompanionCciSummonSupport.resolveType("kon").formName());
        assertEquals("player", CompanionCciSummonSupport.resolveType("dox").formName());
        assertTrue(CompanionCciSummonSupport.resolveType("bits").bitSized());
        assertEquals("wolf", CompanionCciSummonSupport.resolveType("wiggly").formName());
        assertEquals("zombie", CompanionCciSummonSupport.resolveType("zombie").formName());
        assertEquals("azscompanions:kon", CompanionCciSummonSupport.resolveType("kon").definitionId("azscompanions"));
    }

    @Test
    void armorMaterialsExpandToItemsAndNumbersAreIgnored() {
        List<String> diamond = CompanionCciSummonSupport.armorItemIds("diamond");
        assertEquals(4, diamond.size());
        assertTrue(diamond.get(0).contains("helmet"));
        assertTrue(CompanionCciSummonSupport.armorItemIds("20").isEmpty());
        assertTrue(CompanionCciSummonSupport.armorItemIds("-").isEmpty());
        assertEquals("minecraft:diamond_chestplate", CompanionCciSummonSupport.armorItemIds("diamond_chestplate").get(0));
    }

    @Test
    void playerSkinOnlyForPlayerForm() {
        assertTrue(CompanionCciSummonSupport.wantsPlayerSkin("player"));
        assertFalse(CompanionCciSummonSupport.wantsPlayerSkin("wolf"));
    }
}
