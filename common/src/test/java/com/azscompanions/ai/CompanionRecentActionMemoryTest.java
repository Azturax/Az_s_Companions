package com.azscompanions.ai;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionRecentActionMemoryTest {
    private final UUID player = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @AfterEach
    void tearDown() {
        CompanionRecentActionMemory.clearAll();
        CompanionInventoryWatchSupport.clearAll();
    }

    @Test
    void recordsExplosionAndConsumesReactive() {
        assertTrue(CompanionRecentActionMemory.record(
                player, 100L, CompanionRecentActionKind.EXPLOSION,
                "an explosion nearby", null, true));
        assertTrue(CompanionRecentActionMemory.hasReactive(player, 110L));
        Optional<CompanionRecentAction> focus =
                CompanionRecentActionMemory.consumeReactive(player, 110L);
        assertTrue(focus.isPresent());
        assertEquals(CompanionRecentActionKind.EXPLOSION, focus.get().kind());
        assertFalse(CompanionRecentActionMemory.hasReactive(player, 110L));
        assertFalse(CompanionRecentActionMemory.peek(player, 110L).isEmpty());
    }

    @Test
    void darknessOnlyOnEnterEdge() {
        assertTrue(CompanionRecentActionMemory.recordDarknessEnter(player, 20L, true));
        assertFalse(CompanionRecentActionMemory.recordDarknessEnter(player, 40L, true));
        assertFalse(CompanionRecentActionMemory.recordDarknessEnter(player, 60L, false));
        // cooldown blocks immediate re-enter
        assertFalse(CompanionRecentActionMemory.recordDarknessEnter(player, 80L, true));
    }

    @Test
    void craftReadyWhenLastIngredientArrives() {
        Map<String, Integer> before = new LinkedHashMap<>();
        before.put("minecraft:diamond", 1);
        before.put("minecraft:stick", 1);
        Map<String, Integer> after = new LinkedHashMap<>(before);
        after.put("minecraft:diamond", 2);
        Optional<String> ready = CompanionNotableItemSupport.craftCompletedByGain(
                "minecraft:diamond", before, after);
        assertTrue(ready.isPresent());
        assertEquals("minecraft:diamond_sword", ready.get());
    }

    @Test
    void inventoryWatchRecordsFindAndCraftReady() {
        Map<String, Integer> snap1 = Map.of(
                "minecraft:stick", 1,
                "minecraft:diamond", 1);
        CompanionInventoryWatchSupport.observeCounts(player, 1L, snap1);
        Map<String, Integer> snap2 = Map.of(
                "minecraft:stick", 1,
                "minecraft:diamond", 2);
        CompanionInventoryWatchSupport.observeCounts(player, 40L, snap2);
        List<CompanionRecentAction> events = CompanionRecentActionMemory.peek(player, 40L);
        assertTrue(events.stream().anyMatch(a -> a.kind() == CompanionRecentActionKind.CRAFT_READY));
    }

    @Test
    void niceSwordFallback() {
        CompanionRecentAction craft = new CompanionRecentAction(
                CompanionRecentActionKind.ITEM_CRAFT, 1L,
                "player just crafted Diamond Sword", "minecraft:diamond_sword", true);
        assertEquals("NICE SWORD!", CompanionAiChatSupport.fallbackReactiveLine("Az", craft));
    }

    @Test
    void ambientPromptIncludesRecent() {
        CompanionRecentAction boom = new CompanionRecentAction(
                CompanionRecentActionKind.EXPLOSION, 1L, "an explosion nearby", null, true);
        String prompt = CompanionAiChatSupport.ambientPromptWithRecent("Az", boom, List.of(boom));
        assertTrue(prompt.contains("[react]"));
        assertTrue(prompt.contains("explosion") || prompt.contains("boom") || prompt.contains("TNT"));
    }

    @Test
    void itemFindCooldownIsAboutTwoWeeks() {
        assertEquals(14L * 24L * 60L * 60L * 1000L, CompanionRecentActionMemory.ITEM_FIND_COOLDOWN_MS);
        assertEquals(14L * 24L * 60L * 60L * 20L, CompanionRecentActionMemory.ITEM_FIND_COOLDOWN_TICKS);

        CompanionRecentActionMemory.testNowMs = 1_000_000L;
        assertTrue(CompanionRecentActionMemory.record(
                player, 100L, CompanionRecentActionKind.ITEM_FIND,
                "player found diamond", "minecraft:diamond", true));
        // Immediate second find blocked (wall-clock + tick cooldown)
        assertFalse(CompanionRecentActionMemory.record(
                player, 200L, CompanionRecentActionKind.ITEM_FIND,
                "player found emerald", "minecraft:emerald", true));
        // Still blocked just under 14 days later
        CompanionRecentActionMemory.testNowMs = 1_000_000L + CompanionRecentActionMemory.ITEM_FIND_COOLDOWN_MS - 1L;
        assertFalse(CompanionRecentActionMemory.record(
                player, 300L, CompanionRecentActionKind.ITEM_FIND,
                "player found gold", "minecraft:gold_ingot", true));
        // Allowed after full 14-day wall-clock cooldown (and far gameTime past tick cool)
        CompanionRecentActionMemory.testNowMs = 1_000_000L + CompanionRecentActionMemory.ITEM_FIND_COOLDOWN_MS;
        assertTrue(CompanionRecentActionMemory.record(
                player, 100L + CompanionRecentActionMemory.ITEM_FIND_COOLDOWN_TICKS,
                CompanionRecentActionKind.ITEM_FIND,
                "player found netherite", "minecraft:netherite_ingot", true));
    }

    @Test
    void tryClaimItemFindIsSingleFlightAcrossHundredCalls() {
        CompanionRecentActionMemory.testNowMs = 2_000_000L;
        int claimed = 0;
        for (int i = 0; i < 100; i++) {
            if (CompanionRecentActionMemory.tryClaimItemFind(player, 1000L + i)) {
                claimed++;
            }
        }
        assertEquals(1, claimed);
        assertFalse(CompanionRecentActionMemory.canClaimItemFind(player, 2000L));
    }

    @Test
    void notableHeuristics() {
        assertTrue(CompanionNotableItemSupport.isSword("minecraft:iron_sword"));
        assertTrue(CompanionNotableItemSupport.isNotablePickup("minecraft:diamond"));
        assertFalse(CompanionNotableItemSupport.isNotablePickup("minecraft:dirt"));
        assertEquals("Iron Sword", CompanionNotableItemSupport.prettyName("minecraft:iron_sword"));
    }
}
