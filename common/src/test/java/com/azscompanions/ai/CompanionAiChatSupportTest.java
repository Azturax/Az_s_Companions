package com.azscompanions.ai;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionAiChatSupportTest {
    @Test
    void chatListenModeAliases() {
        assertEquals(ChatListenMode.OFF, ChatListenMode.fromConfig("off"));
        assertEquals(ChatListenMode.PLAYER, ChatListenMode.fromConfig("player"));
        assertEquals(ChatListenMode.PLAYER, ChatListenMode.fromConfig("owner"));
        assertEquals(ChatListenMode.GLOBAL, ChatListenMode.fromConfig("global"));
        assertEquals(ChatListenMode.GLOBAL, ChatListenMode.fromConfig("all"));
        assertTrue(ChatListenMode.PLAYER.listens());
        assertFalse(ChatListenMode.OFF.listens());
    }

    @Test
    void ignoresSlashAndCompanionShapedLines() {
        assertTrue(CompanionAiChatSupport.shouldIgnoreChatMessage("/home"));
        assertTrue(CompanionAiChatSupport.shouldIgnoreChatMessage("  "));
        assertTrue(CompanionAiChatSupport.shouldIgnoreChatMessage("Kon ask hello"));
        assertFalse(CompanionAiChatSupport.shouldIgnoreChatMessage("hello"));
        assertTrue(CompanionAiChatSupport.looksLikeCompanionReply("<Kon> hi"));
        assertTrue(CompanionAiChatSupport.looksLikeCompanionReply("[BRAT] <Kon> hi"));
        assertTrue(CompanionAiChatSupport.looksLikeCompanionReply("… Kon is thinking"));
        assertFalse(CompanionAiChatSupport.looksLikeCompanionReply("hello world"));
    }

    @Test
    void idleIntervalWithinRange() {
        Random r = new Random(1L);
        for (int i = 0; i < 20; i++) {
            int v = CompanionAiChatSupport.nextIdleIntervalSeconds(75, 180, r);
            assertTrue(v >= 75 && v <= 180);
        }
    }

    @Test
    void legacyChattyIdleBoundsRemapToRare() {
        int[] bounds = CompanionAiChatSupport.effectiveIdleBounds(75, 180);
        assertEquals(CompanionAiChatSupport.DEFAULT_IDLE_CHAT_SECONDS_MIN, bounds[0]);
        assertEquals(CompanionAiChatSupport.DEFAULT_IDLE_CHAT_SECONDS_MAX, bounds[1]);
        Random r = new Random(2L);
        for (int i = 0; i < 20; i++) {
            int v = CompanionAiChatSupport.nextRareIdleIntervalSeconds(75, 180, r::nextInt);
            assertTrue(v >= 480 && v <= 1200);
        }
        int ticks = CompanionAiChatSupport.nextIdleDelayTicks(75, 180, 1.0d, r::nextInt);
        assertTrue(ticks >= 480 * 20 && ticks <= 1200 * 20);
    }

    @Test
    void playerGateSerializesCompanionsAndAvoidsRepeat() {
        java.util.UUID player = java.util.UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        CompanionAiChatSupport.clearPlayerChatGates();
        assertFalse(CompanionAiChatSupport.playerAmbientTooRecent(player, 1000L, false));
        CompanionAiChatSupport.recordAmbientSpeak(player, 1000L, "Quiet stretch. I'm okay with that.");
        assertTrue(CompanionAiChatSupport.playerAmbientTooRecent(player, 1000L + 20L, false));
        assertTrue(CompanionAiChatSupport.playerAmbientTooRecent(
                player, 1000L + CompanionAiChatSupport.DEFAULT_PLAYER_AMBIENT_GAP_SECONDS * 20L - 1L, false));
        assertFalse(CompanionAiChatSupport.playerAmbientTooRecent(
                player, 1000L + CompanionAiChatSupport.DEFAULT_PLAYER_AMBIENT_GAP_SECONDS * 20L, false));
        assertTrue(CompanionAiChatSupport.isSameAsLastLine(player, "Quiet stretch. I'm okay with that."));
        assertFalse(CompanionAiChatSupport.isSameAsLastLine(player, "Different line."));
        CompanionAiChatSupport.clearPlayerChatGates();
    }

    @Test
    void shortenSpokenLineDropsExtraLines() {
        assertEquals("Just this.", CompanionAiChatSupport.shortenSpokenLine("Just this.\nAnd more.\nEven more."));
        String longLine = "x".repeat(200);
        String shortened = CompanionAiChatSupport.shortenSpokenLine(longLine);
        assertEquals(CompanionAiChatSupport.MAX_SPOKEN_LINE_CHARS, shortened.length());
        assertTrue(shortened.endsWith("…"));
    }

    @Test
    void configRoundTripIncludesChatKeys() {
        CompanionAiSettings s = new CompanionAiSettings()
                .setChatListenMode(ChatListenMode.PLAYER)
                .setChatReactRange(40)
                .setIdleChat(true)
                .setIdleChatSecondsMin(60)
                .setIdleChatSecondsMax(120)
                .setCallPlayerWhenAway(true)
                .setCallPlayerAfterSeconds(45)
                .setCallPlayerDistance(32)
                .setServerLlmOnly(false);
        JsonObject json = CompanionAiConfigIO.toJson(s);
        CompanionAiSettings loaded = CompanionAiConfigIO.fromJson(json);
        assertEquals(ChatListenMode.PLAYER, loaded.chatListenMode());
        assertTrue(json.has("chatListenMode"));
        assertEquals("player", json.get("chatListenMode").getAsString());
        assertTrue(json.has("globalTalk"));
        assertTrue(loaded.globalTalk());
        assertEquals(40.0d, loaded.chatReactRange(), 0.01);
        assertTrue(loaded.idleChat());
        assertEquals(60, loaded.idleChatSecondsMin());
        assertEquals(120, loaded.idleChatSecondsMax());
        assertTrue(loaded.callPlayerWhenAway());
        assertEquals(45, loaded.callPlayerAfterSeconds());
        assertEquals(32.0d, loaded.callPlayerDistance(), 0.01);
        assertFalse(loaded.serverLlmOnly());
        assertFalse(new CompanionAiSettings().serverLlmOnly());
        assertFalse(new CompanionAiSettings().integratedMultiplayerSharedLlm());
        assertTrue(new CompanionAiSettings().ownerNameFallback());
    }

    @Test
    void spokeTooRecentlyUsesCooldown() {
        assertTrue(CompanionAiChatSupport.spokeTooRecently(20, 45));
        assertFalse(CompanionAiChatSupport.spokeTooRecently(45 * 20, 45));
        assertFalse(CompanionAiChatSupport.spokeTooRecently(-1, 45));
    }

    @Test
    void idleChatDefaultsOn() {
        assertTrue(new CompanionAiSettings().idleChat());
        assertEquals(480, new CompanionAiSettings().idleChatSecondsMin());
        assertEquals(1200, new CompanionAiSettings().idleChatSecondsMax());
        assertEquals(12, new CompanionAiSettings().chatReactCooldownSeconds());
        assertEquals(2, new CompanionAiSettings().maxParallelRequests());
        assertEquals(8, new CompanionAiSettings().connectTimeoutSeconds());
    }

    @Test
    void backgroundPromptDetection() {
        assertTrue(CompanionAiChatSupport.isBackgroundPrompt("[ambient] hello"));
        assertTrue(CompanionAiChatSupport.isBackgroundPrompt("[react] boom"));
        assertTrue(CompanionAiChatSupport.isBackgroundPrompt("[call] where"));
        assertFalse(CompanionAiChatSupport.isBackgroundPrompt("How are you?"));
    }

    @Test
    void latencyConfigRoundTrip() {
        CompanionAiSettings s = new CompanionAiSettings()
                .setConnectTimeoutSeconds(5)
                .setMaxParallelRequests(3);
        JsonObject json = CompanionAiConfigIO.toJson(s);
        CompanionAiSettings loaded = CompanionAiConfigIO.fromJson(json);
        assertEquals(5, loaded.connectTimeoutSeconds());
        assertEquals(3, loaded.maxParallelRequests());
        assertEquals(5, json.get("connectTimeoutSeconds").getAsInt());
        assertEquals(3, json.get("maxParallelRequests").getAsInt());
    }

    @Test
    void sharedServerLlmFlagOptInOnly() {
        CompanionAiRuntime runtime = CompanionAiRuntime.get();
        runtime.clearServerContext();
        runtime.applySettings(new CompanionAiSettings().setServerLlmOnly(false));
        assertFalse(runtime.usesSharedServerLlm());
        runtime.markServerContext(true);
        // Dedicated alone no longer forces shared — Use server LLM must be ON
        assertFalse(runtime.usesSharedServerLlm());
        runtime.applySettings(new CompanionAiSettings().setServerLlmOnly(true));
        assertTrue(runtime.usesSharedServerLlm());
        assertTrue(runtime.statusLine().contains("[server LLM shared]"));
        runtime.clearServerContext();
        assertFalse(runtime.usesSharedServerLlm());
        runtime.applySettings(new CompanionAiSettings().setServerLlmOnly(true));
        runtime.markServerContext(false);
        assertTrue(runtime.usesSharedServerLlm());
        runtime.clearServerContext();
    }

    @Test
    void chatListenAndGlobalTalkRoundTrip() {
        JsonObject root = new JsonObject();
        root.addProperty("chatReaction", "player");
        root.addProperty("chatListenMode", "global");
        root.addProperty("nameListen", true);
        root.addProperty("globalTalk", false);
        root.addProperty("enableAiActions", true);
        CompanionAiSettings loaded = CompanionAiConfigIO.fromJson(root);
        assertEquals(ChatListenMode.GLOBAL, loaded.chatListenMode());
        assertTrue(loaded.nameListen());
        assertFalse(loaded.globalTalk());
        assertFalse(loaded.enableAiActions());
    }
}
