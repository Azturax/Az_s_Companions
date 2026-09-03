package com.azscompanions.entity;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertEquals(CompanionCciSummonSupport.VanillaPlayerPick.NONE,
                CompanionCciSummonSupport.resolveType("kon").vanillaPlayerPick());
        assertEquals(CompanionCciSummonSupport.VanillaPlayerPick.NONE,
                CompanionCciSummonSupport.resolveType("bits").vanillaPlayerPick());
        assertEquals(CompanionCciSummonSupport.VanillaPlayerPick.NONE,
                CompanionCciSummonSupport.resolveType("zombie").vanillaPlayerPick());
    }

    @Test
    void defaultTypeIsPlayerWithRandomSteveOrAlex() {
        assertEquals("player", CompanionCciSummonSupport.DEFAULT_TYPE);
        assertEquals(CompanionCciSummonSupport.VanillaPlayerPick.RANDOM,
                CompanionCciSummonSupport.resolveType(null).vanillaPlayerPick());
        assertEquals(CompanionCciSummonSupport.VanillaPlayerPick.RANDOM,
                CompanionCciSummonSupport.resolveType("").vanillaPlayerPick());
        assertEquals(CompanionCciSummonSupport.VanillaPlayerPick.RANDOM,
                CompanionCciSummonSupport.resolveType("player").vanillaPlayerPick());
        assertEquals(CompanionCciSummonSupport.VanillaPlayerPick.STEVE,
                CompanionCciSummonSupport.resolveType("steve").vanillaPlayerPick());
        assertEquals(CompanionCciSummonSupport.VanillaPlayerPick.ALEX,
                CompanionCciSummonSupport.resolveType("alex").vanillaPlayerPick());
        assertEquals("player", CompanionCciSummonSupport.resolveType("player").formName());
        assertEquals("azscompanions:kon",
                CompanionCciSummonSupport.resolveType("player").definitionId("azscompanions"));
    }

    @Test
    void vanillaSteveIsWideAlexIsSlim() {
        CompanionCciSummonSupport.VanillaPlayerSkin steve =
                CompanionCciSummonSupport.pickVanillaPlayerSkin(CompanionCciSummonSupport.VanillaPlayerPick.STEVE);
        CompanionCciSummonSupport.VanillaPlayerSkin alex =
                CompanionCciSummonSupport.pickVanillaPlayerSkin(CompanionCciSummonSupport.VanillaPlayerPick.ALEX);
        assertNotNull(steve);
        assertNotNull(alex);
        assertFalse(steve.slim());
        assertTrue(alex.slim());
        assertTrue(steve.texturePath().contains("wide/steve.png"));
        assertTrue(alex.texturePath().contains("slim/alex.png"));
        assertNull(CompanionCciSummonSupport.pickVanillaPlayerSkin(CompanionCciSummonSupport.VanillaPlayerPick.NONE));
    }

    @Test
    void randomVanillaPickIsPerCallNotCached() {
        CompanionCciSummonSupport.VanillaPlayerSkin alwaysHeads =
                CompanionCciSummonSupport.pickVanillaPlayerSkin(
                        CompanionCciSummonSupport.VanillaPlayerPick.RANDOM, new Random() {
                            @Override
                            public boolean nextBoolean() {
                                return true;
                            }
                        });
        CompanionCciSummonSupport.VanillaPlayerSkin alwaysTails =
                CompanionCciSummonSupport.pickVanillaPlayerSkin(
                        CompanionCciSummonSupport.VanillaPlayerPick.RANDOM, new Random() {
                            @Override
                            public boolean nextBoolean() {
                                return false;
                            }
                        });
        assertEquals(CompanionCciSummonSupport.VanillaPlayerSkin.STEVE, alwaysHeads);
        assertEquals(CompanionCciSummonSupport.VanillaPlayerSkin.ALEX, alwaysTails);
    }

    @Test
    void vanillaDefaultSkippedWhenUsernameSkinAppliedOrKonType() {
        CompanionCciSummonSupport.TypeSpec player = CompanionCciSummonSupport.resolveType("player");
        CompanionCciSummonSupport.TypeSpec kon = CompanionCciSummonSupport.resolveType("kon");
        assertTrue(CompanionCciSummonSupport.shouldApplyVanillaDefault(player, false));
        assertFalse(CompanionCciSummonSupport.shouldApplyVanillaDefault(player, true));
        assertFalse(CompanionCciSummonSupport.shouldApplyVanillaDefault(kon, false));
        assertFalse(CompanionCciSummonSupport.shouldApplyVanillaDefault(
                CompanionCciSummonSupport.resolveType("wiggly"), false));
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

    @Test
    void behaviorModeAliasesAndSkip() {
        assertEquals("follow", CompanionCciSummonSupport.DEFAULT_MODE);
        assertEquals("follow", CompanionCciSummonSupport.resolveBehaviorMode(null));
        assertEquals("follow", CompanionCciSummonSupport.resolveBehaviorMode("-"));
        assertEquals("follow", CompanionCciSummonSupport.resolveBehaviorMode("follow"));
        assertEquals("wander", CompanionCciSummonSupport.resolveBehaviorMode("idle"));
        assertEquals("wander", CompanionCciSummonSupport.resolveBehaviorMode("wander"));
        assertEquals("guard", CompanionCciSummonSupport.resolveBehaviorMode("attack"));
        assertEquals("guard", CompanionCciSummonSupport.resolveBehaviorMode("guard"));
        assertEquals("stay", CompanionCciSummonSupport.resolveBehaviorMode("stay"));
        assertEquals("sit", CompanionCciSummonSupport.resolveBehaviorMode("sit"));
        assertTrue(CompanionCciSummonSupport.looksLikeBehaviorMode("Idle"));
        assertTrue(CompanionCciSummonSupport.looksLikeBehaviorMode("-"));
        assertFalse(CompanionCciSummonSupport.looksLikeBehaviorMode("Alice"));
        String[] named = CompanionCciSummonSupport.splitModeAndName("Alice", null);
        assertEquals("follow", named[0]);
        assertEquals("Alice", named[1]);
        String[] modeThenName = CompanionCciSummonSupport.splitModeAndName("attack", "Alice");
        assertEquals("guard", modeThenName[0]);
        assertEquals("Alice", modeThenName[1]);
        String[] skipped = CompanionCciSummonSupport.splitModeAndName("-", "Alice");
        assertEquals("follow", skipped[0]);
        assertEquals("Alice", skipped[1]);
        assertTrue(CompanionCciSummonSupport.displayNameMatches("Alice", "alice"));
        assertFalse(CompanionCciSummonSupport.displayNameMatches("Kon", "Alice"));
    }
}
