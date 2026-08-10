package com.azscompanions.teamfight;

import com.azscompanions.cci.CciCompanionParams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TeamFightChatParserTest {
    @Test
    void parsesCheerLineIntoKeyValues() {
        String msg = TeamFightChatParser.toCciMessage("Alice cheered 500 bits for zombie on red");
        assertTrue(msg.contains("bits=500"));
        assertTrue(msg.contains("team=red"));
        assertTrue(msg.contains("form=zombie"));
        assertTrue(msg.contains("name=Alice"));
    }

    @Test
    void leavesStructuredMessagesAlone() {
        CciCompanionParams params = TeamFightChatParser.parseChatOrMessage("bits=750;team=blue;name=Bob");
        assertEquals(750, params.bitsOr(0));
        assertEquals("blue", params.teamOr(""));
        assertEquals("Bob", params.displayName());
    }

    @Test
    void mapsLeftRightToDefaultTeams() {
        String msg = TeamFightChatParser.toCciMessage("Carol 1 sub left");
        assertTrue(msg.contains("subs=1"));
        assertTrue(msg.contains("team=" + TeamFightDefaults.TEAM_LEFT));
    }
}
