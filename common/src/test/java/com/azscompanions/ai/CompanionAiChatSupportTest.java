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
        assertTrue(CompanionAiChatSupport.looksLikeCompanionReply("… Kon is thinking"));
        assertFalse(CompanionAiChatSupport.looksLikeCompanionReply("hello world"));
    }

    @Test
    void idleIntervalWithinRange() {
        Random r = new Random(1L);
        for (int i = 0; i < 20; i++) {
            int v = CompanionAiChatSupport.nextIdleIntervalSeconds(90, 240, r);
            assertTrue(v >= 90 && v <= 240);
        }
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
        assertEquals(40.0d, loaded.chatReactRange(), 0.01);
        assertTrue(loaded.idleChat());
        assertEquals(60, loaded.idleChatSecondsMin());
        assertEquals(120, loaded.idleChatSecondsMax());
        assertTrue(loaded.callPlayerWhenAway());
        assertEquals(45, loaded.callPlayerAfterSeconds());
        assertEquals(32.0d, loaded.callPlayerDistance(), 0.01);
        assertFalse(loaded.serverLlmOnly());
        assertTrue(new CompanionAiSettings().serverLlmOnly());
        assertTrue(new CompanionAiSettings().integratedMultiplayerSharedLlm());
        assertTrue(new CompanionAiSettings().ownerNameFallback());
    }

    @Test
    void sharedServerLlmFlagOnDedicatedHost() {
        CompanionAiRuntime runtime = CompanionAiRuntime.get();
        runtime.clearServerContext();
        runtime.applySettings(new CompanionAiSettings().setServerLlmOnly(false));
        assertFalse(runtime.usesSharedServerLlm());
        runtime.markServerContext(true);
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
    void chatReactionAliasKey() {
        JsonObject root = new JsonObject();
        root.addProperty("chatReaction", "global");
        assertEquals(ChatListenMode.GLOBAL, CompanionAiConfigIO.fromJson(root).chatListenMode());
    }
}
