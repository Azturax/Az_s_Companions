package com.azscompanions.ai;

import com.azscompanions.admin.AdminAiConfigSnapshot;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionChatEventConfigTest {
    private final UUID player = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @AfterEach
    void tearDown() {
        CompanionRecentActionMemory.clearAll();
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
    void itemFindGateBlocksBuiltinButCustomStillFansOut() {
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
        CompanionChatEventSupport.observe(
                player, 100L, CompanionRecentActionKind.ITEM_FIND,
                "player found diamond", "minecraft:diamond", true);
        assertFalse(CompanionRecentActionMemory.peek(player, 100L).stream()
                .anyMatch(a -> a.kind() == CompanionRecentActionKind.ITEM_FIND));
        assertTrue(CompanionRecentActionMemory.peek(player, 100L).stream()
                .anyMatch(a -> a.kind() == CompanionRecentActionKind.CUSTOM
                        && "gem".equals(a.customEventId())));
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
}
