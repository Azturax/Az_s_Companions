package com.azscompanions.ai;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionAskResolveTest {
    @Test
    void sanitizeStripsUnsafeChars() {
        assertEquals("kon", CompanionAskResolve.sanitizeToken("Kon"));
        assertEquals("mr_wiggly", CompanionAskResolve.sanitizeToken("Mr Wiggly!"));
        assertEquals("bit2", CompanionAskResolve.sanitizeToken("Bit-2"));
        assertEquals("", CompanionAskResolve.sanitizeToken("!!!"));
    }

    @Test
    void namesMatchIsOwnerScopedExactSanitized() {
        assertTrue(CompanionAskResolve.namesMatch("Kon", "kon"));
        assertTrue(CompanionAskResolve.namesMatch("Kon!", "Kon"));
        assertFalse(CompanionAskResolve.namesMatch("Kon", "KonBit"));
        assertFalse(CompanionAskResolve.namesMatch("Alice", "Bob"));
    }

    @Test
    void parseNamedAskChatKeepsMultiSentence() {
        var ok = CompanionAskResolve.parseNamedAskChat(
                "Kon ask Hello there. Please come here. Then mine stone.");
        assertTrue(ok.isPresent());
        assertEquals("Kon", ok.get().companionName());
        assertEquals("Hello there. Please come here. Then mine stone.", ok.get().message());
    }

    @Test
    void parseNamedAskChat() {
        var ok = CompanionAskResolve.parseNamedAskChat("Kon ask hello there");
        assertTrue(ok.isPresent());
        assertEquals("Kon", ok.get().companionName());
        assertEquals("hello there", ok.get().message());

        assertTrue(CompanionAskResolve.parseNamedAskChat("kon ASK hi").isPresent());
        assertTrue(CompanionAskResolve.parseNamedAskChat("/kon ask hi").isEmpty());
        assertTrue(CompanionAskResolve.parseNamedAskChat("just chatting").isEmpty());
        assertTrue(CompanionAskResolve.parseNamedAskChat("Kon ask").isEmpty());
    }

    @Test
    void resolveGreedyAskPrefersOwnedName() {
        Set<String> owned = Set.of("kon", "bit");
        CompanionAskResolve.NameMatcher matcher = q ->
                owned.contains(CompanionAskResolve.sanitizeToken(q));

        var named = CompanionAskResolve.resolveGreedyAsk("Kon Hello friend", matcher);
        assertEquals(CompanionAskResolve.AskTarget.Kind.NAMED, named.kind());
        assertEquals("Kon", named.companionName());
        assertEquals("Hello friend", named.message());

        var nearest = CompanionAskResolve.resolveGreedyAsk("Hello friend", matcher);
        assertEquals(CompanionAskResolve.AskTarget.Kind.NEAREST, nearest.kind());
        assertEquals("Hello friend", nearest.message());

        assertFalse(CompanionAskResolve.resolveGreedyAsk("  ", matcher).isValid());
    }
}
