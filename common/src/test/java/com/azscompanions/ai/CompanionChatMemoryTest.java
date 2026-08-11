package com.azscompanions.ai;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionChatMemoryTest {
    @AfterEach
    void tearDown() {
        CompanionAiRuntime.get().clearServerContext();
        CompanionAiRuntime.get().chatMemory().clearAll();
    }

    @Test
    void historiesAreIsolatedByCompanionUuid() {
        CompanionChatMemory memory = new CompanionChatMemory();
        UUID a = UUID.fromString("00000000-0000-0000-0000-00000000000a");
        UUID b = UUID.fromString("00000000-0000-0000-0000-00000000000b");
        memory.recordExchange(a, "Player (en): hi A", "hello from A", 16);
        memory.recordExchange(b, "Player (en): hi B", "hello from B", 16);

        List<CompanionChatMemory.Turn> histA = memory.snapshot(a, 16);
        List<CompanionChatMemory.Turn> histB = memory.snapshot(b, 16);
        assertEquals(2, histA.size());
        assertEquals(2, histB.size());
        assertTrue(histA.get(1).content().contains("A"));
        assertTrue(histB.get(1).content().contains("B"));
        assertFalse(histA.stream().anyMatch(t -> t.content().contains("B")));
        assertFalse(histB.stream().anyMatch(t -> t.content().contains("A")));
    }

    @Test
    void trimsToMemoryMaxMessages() {
        CompanionChatMemory memory = new CompanionChatMemory();
        UUID id = UUID.randomUUID();
        for (int i = 0; i < 10; i++) {
            memory.recordExchange(id, "u" + i, "a" + i, 4);
        }
        List<CompanionChatMemory.Turn> snap = memory.snapshot(id, 4);
        assertEquals(4, snap.size());
        assertEquals("u8", snap.get(0).content());
        assertEquals("a9", snap.get(3).content());
    }

    @Test
    void configRoundTripMemoryKeys() {
        CompanionAiSettings s = new CompanionAiSettings()
                .setPerCompanionMemory(false)
                .setMemoryMaxMessages(8);
        JsonObject json = CompanionAiConfigIO.toJson(s);
        CompanionAiSettings loaded = CompanionAiConfigIO.fromJson(json);
        assertFalse(loaded.perCompanionMemory());
        assertEquals(8, loaded.memoryMaxMessages());
        assertTrue(new CompanionAiSettings().perCompanionMemory());
        assertEquals(12, new CompanionAiSettings().memoryMaxMessages());
    }

    @Test
    void systemPromptIncludesAttitudeAndIndependence() {
        CompanionAiSettings s = new CompanionAiSettings();
        String prompt = s.formatSystemPrompt("Kon", "player", "Parent", true, true, "HOSTILE");
        assertTrue(prompt.contains("Kon"));
        assertTrue(prompt.contains("HOSTILE"));
        assertTrue(prompt.contains("independent mind"));
        assertTrue(prompt.contains("Parent"));
        assertTrue(prompt.contains("chat history is your own"));
        assertFalse(prompt.contains("other companions' recent"));
    }

    @Test
    void statusLineMentionsSeparateMinds() {
        CompanionAiRuntime runtime = CompanionAiRuntime.get();
        runtime.applySettings(new CompanionAiSettings().setPerCompanionMemory(true));
        runtime.markServerContext(true);
        assertTrue(runtime.statusLine().contains("[separate minds]"));
        runtime.applySettings(new CompanionAiSettings().setPerCompanionMemory(false));
        assertFalse(runtime.statusLine().contains("[separate minds]"));
    }
}
