package com.azscompanions.ai;

import com.azscompanions.admin.AdminAiConfigSnapshot;
import com.azscompanions.entity.CompanionBedSleepSupport;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionChatEventConfigTest {
    private final UUID player = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @AfterEach
    void tearDown() {
        CompanionRecentActionMemory.clearAll();
        CompanionInventoryWatchSupport.clearAll();
        CompanionAiRuntime.get().applySettings(new CompanionAiSettings());
    }

    @Test
    void defaultsKeepReactiveAndItemFindOn() {
        CompanionAiSettings s = new CompanionAiSettings();
        assertTrue(s.idleChat());
        assertTrue(s.reactiveChat());
        assertTrue(s.itemFindChat());
        assertTrue(s.customChatEvents().isEmpty());
    }

    @Test
    void jsonRoundTripCustomEventsAndFlags() {
        CompanionCustomChatEvent ev = new CompanionCustomChatEvent()
                .setId("diamond_cheer")
                .setEnabled(true)
                .setTrigger("item_find")
                .setItemId("minecraft:diamond")
                .setPrompt("Celebrate the diamond briefly.")
                .setFallback("A diamond!")
                .setCooldownSeconds(120)
                .setPriority(80);
        CompanionAiSettings s = new CompanionAiSettings()
                .setReactiveChat(false)
                .setItemFindChat(false)
                .setCustomChatEvents(List.of(ev));
        JsonObject json = CompanionAiConfigIO.toJson(s);
        CompanionAiSettings loaded = CompanionAiConfigIO.fromJson(json);
        assertFalse(loaded.reactiveChat());
        assertFalse(loaded.itemFindChat());
        assertEquals(1, loaded.customChatEvents().size());
        assertEquals("diamond_cheer", loaded.customChatEvents().get(0).id());
        assertEquals("minecraft:diamond", loaded.customChatEvents().get(0).itemId());
    }

    @Test
    void itemFindGateAllowsOneCustomWhenBuiltinOff() {
        CompanionAiRuntime.get().applySettings(new CompanionAiSettings()
                .setReactiveChat(true)
                .setItemFindChat(false)
                .setCustomChatEvents(List.of(new CompanionCustomChatEvent()
                        .setId("gem")
                        .setTrigger("item_find")
                        .setItemId("minecraft:diamond")
                        .setFallback("Sparkly!")
                        .setCooldownSeconds(10))));
        assertFalse(CompanionChatEventSupport.allowBuiltinReactive(CompanionRecentActionKind.ITEM_FIND));
        CompanionRecentActionMemory.testNowMs = 5_000_000L;
        assertTrue(CompanionChatEventSupport.observe(
                player, 100L, CompanionRecentActionKind.ITEM_FIND,
                "player found diamond", "minecraft:diamond", true));
        assertFalse(CompanionRecentActionMemory.peek(player, 100L).stream()
                .anyMatch(a -> a.kind() == CompanionRecentActionKind.ITEM_FIND));
        assertTrue(CompanionRecentActionMemory.peek(player, 100L).stream()
                .anyMatch(a -> a.kind() == CompanionRecentActionKind.CUSTOM
                        && "gem".equals(a.customEventId())));
        // Second find in the same window â€” hard blocked (no custom burst)
        assertFalse(CompanionChatEventSupport.observe(
                player, 120L, CompanionRecentActionKind.ITEM_FIND,
                "player found emerald", "minecraft:emerald", true));
    }

    @Test
    void rapidInventoryTicksCannotSpamFinds() {
        CompanionRecentActionMemory.testNowMs = 9_000_000L;
        CompanionInventoryWatchSupport.observeCounts(player, 1L, Map.of("minecraft:diamond", 1));
        int observeHits = 0;
        for (int i = 0; i < 200; i++) {
            long t = 40L + i;
            CompanionInventoryWatchSupport.observeCounts(
                    player, t, Map.of("minecraft:diamond", 2 + i));
            if (!CompanionRecentActionMemory.canClaimItemFind(player, t + 1L)) {
                observeHits++;
            }
        }
        assertTrue(observeHits >= 199, "find gate should stay closed after first claim");
        assertFalse(CompanionRecentActionMemory.canClaimItemFind(player, 300L));
        assertEquals(1L, CompanionRecentActionMemory.peek(player, 50L).stream()
                .filter(a -> a.kind() == CompanionRecentActionKind.ITEM_FIND)
                .count());
    }

    @Test
    void logoutDoesNotResetFindCooldown() {
        CompanionRecentActionMemory.testNowMs = 3_000_000L;
        assertTrue(CompanionRecentActionMemory.tryClaimItemFind(player, 50L));
        CompanionRecentActionMemory.clearPlayer(player);
        assertFalse(CompanionRecentActionMemory.tryClaimItemFind(player, 60L));
    }

    @Test
    void adminSnapshotWiresReactiveAndItemFind() {
        CompanionAiSettings base = new CompanionAiSettings()
                .setReactiveChat(false)
                .setItemFindChat(false);
        AdminAiConfigSnapshot snap = AdminAiConfigSnapshot.fromSettings(base);
        assertFalse(snap.reactiveChat());
        assertFalse(snap.itemFindChat());
        snap.setReactiveChat(true).setItemFindChat(true);
        assertTrue(snap.mergeInto(base).reactiveChat());
        assertTrue(snap.toWireJson().contains("\"reactiveChat\":true"));
        assertTrue(snap.toWireJson().contains("\"itemFindChat\":true"));
    }

    @Test
    void bedSupportPrefersClaimThenSearchesKonOnly() {
        AtomicInteger probes = new AtomicInteger();
        CompanionBedSleepSupport.IntPos origin = new CompanionBedSleepSupport.IntPos(0, 64, 0);
        CompanionBedSleepSupport.IntPos claimed = new CompanionBedSleepSupport.IntPos(2, 64, 0);
        CompanionBedSleepSupport.IntPos other = new CompanionBedSleepSupport.IntPos(5, 64, 0);

        CompanionBedSleepSupport.CompanionBedProbe okClaim = pos -> {
            probes.incrementAndGet();
            return pos.equals(claimed) || pos.equals(other);
        };
        assertEquals(claimed, CompanionBedSleepSupport.resolveSleepBed(origin, claimed, okClaim));

        CompanionBedSleepSupport.CompanionBedProbe claimBroken = pos -> pos.equals(other);
        assertTrue(CompanionBedSleepSupport.isClaimInvalid(claimed, claimBroken));
        CompanionBedSleepSupport.IntPos found =
                CompanionBedSleepSupport.resolveSleepBed(origin, claimed, claimBroken);
        assertNotNull(found);
        assertEquals(other, found);

        assertNull(CompanionBedSleepSupport.resolveSleepBed(
                origin, claimed, pos -> false));
        assertTrue(CompanionBedSleepSupport.isKonBedId("azscompanions:kon_bed"));
        assertFalse(CompanionBedSleepSupport.isKonBedId("minecraft:red_bed"));
    }
}
