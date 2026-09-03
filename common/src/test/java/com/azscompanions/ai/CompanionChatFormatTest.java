package com.azscompanions.ai;

import com.azscompanions.AzsCompanionsConstants;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionChatFormatTest {
    @Test
    void bratUuidIsExactAndNotOtherPerks() {
        assertEquals(UUID.fromString("324ca5e2-c2e1-4b50-be3d-01198293e919"),
                AzsCompanionsConstants.BRAT_PLAYER_UUID);
        assertEquals("BRAT", AzsCompanionsConstants.BRAT_CHAT_PREFIX);
        assertTrue(AzsCompanionsConstants.isBratOwner(AzsCompanionsConstants.BRAT_PLAYER_UUID));
        assertFalse(AzsCompanionsConstants.isBratOwner(AzsCompanionsConstants.MISTER_WIGGLY_PLAYER_UUID));
        assertFalse(AzsCompanionsConstants.isBratOwner(null));
        assertFalse(AzsCompanionsConstants.isBratOwner(UUID.randomUUID()));
    }

    @Test
    void defaultLineKeepsAngleBracketStyle() {
        UUID other = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        assertEquals("<Kon> hello", CompanionChatFormat.formatLine(other, "Kon", "hello"));
        assertEquals("<Kon> hello", CompanionChatFormat.formatLine(null, "Kon", "hello"));
        assertEquals("<Companion> hi", CompanionChatFormat.formatLine(other, "  ", "hi"));
        assertNull(CompanionChatFormat.rankPrefix(other));
    }

    @Test
    void bratOwnerGetsCompactRankTag() {
        UUID brat = AzsCompanionsConstants.BRAT_PLAYER_UUID;
        assertEquals("[BRAT] <Kon> hello", CompanionChatFormat.formatLine(brat, "Kon", "hello"));
        assertEquals("[BRAT] <Bits> yo",
                CompanionChatFormat.formatLine(null, brat, "Bits", "yo"));
        assertEquals("<Kon> hello",
                CompanionChatFormat.formatLine(null, UUID.randomUUID(), "Kon", "hello"));
        assertEquals("BRAT", CompanionChatFormat.rankPrefix(brat));
    }

    @Test
    void listenLoopStillSeesBratLinesAsCompanionReplies() {
        String bratLine = CompanionChatFormat.formatLine(
                AzsCompanionsConstants.BRAT_PLAYER_UUID, "Kon", "hello");
        assertEquals("<Kon> hello", CompanionChatFormat.stripRankPrefix(bratLine));
        assertTrue(CompanionAiChatSupport.looksLikeCompanionReply(bratLine));
        assertTrue(CompanionChatListenSupport.shouldIgnoreChat(bratLine));
        assertFalse(CompanionAiChatSupport.looksLikeCompanionReply("hello from a player"));
    }
}
