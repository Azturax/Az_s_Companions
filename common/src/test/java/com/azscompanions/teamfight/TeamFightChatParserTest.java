package com.azscompanions.teamfight;

import com.azscompanions.cci.CciCompanionParams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TeamFightChatParserTest {
    @Test
    void passesStructuredMessagesThrough() {
        String msg = TeamFightChatParser.toCciMessage("amount=500;user=Alice;form=zombie;team=red");
        assertTrue(msg.contains("amount=500"));
        assertTrue(msg.contains("user=Alice"));
        assertTrue(msg.contains("team=red"));
    }

    @Test
    void ignoresUnstructuredFreeform() {
        assertEquals("", TeamFightChatParser.toCciMessage("Alice cheered 500 bits for zombie on red"));
    }

    @Test
    void parseChatOrMessageReadsAmount() {
        CciCompanionParams params = TeamFightChatParser.parseChatOrMessage("amount=750;team=blue;user=Bob");
        assertEquals(750, params.supportAmountOr(0));
        assertEquals("blue", params.teamOr(""));
        assertEquals("Bob", params.interactionUser());
    }
}
