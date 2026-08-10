package com.azscompanions.teamfight;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TeamFightSessionTest {
    @AfterEach
    void tearDown() {
        TeamFightSession.clearAll();
    }

    @Test
    void enableByDefaultOnlyOnCreate() {
        UUID id = UUID.randomUUID();
        TeamFightSession first = TeamFightSession.of(id, true);
        assertTrue(first.isEnabled());
        first.setEnabled(false);
        TeamFightSession again = TeamFightSession.of(id, true);
        assertFalse(again.isEnabled());
    }

    @Test
    void teamKillScoresOppositeTeamsOnly() {
        TeamFightSession session = TeamFightSession.of(UUID.randomUUID());
        session.setEnabled(true);
        session.recordFighter("Alice", "red", 100);
        assertTrue(session.tryRecordTeamKill("Alice", "red", "Bob", "blue"));
        assertEquals(1, session.scoreLeft());
        assertEquals(0, session.scoreRight());
        assertFalse(session.tryRecordTeamKill("Alice", "red", "Carol", "red"));
        assertEquals(1, session.scoreLeft());
    }

    @Test
    void hudSnapshotRoundTrip() {
        TeamFightSession session = TeamFightSession.of(UUID.randomUUID());
        session.setEnabled(true);
        session.addScore("red", 3);
        session.addBits("blue", 500);
        session.recordFighter("Zed", "blue", 500);
        String encoded = session.snapshot().encode();
        TeamFightHudSnapshot decoded = TeamFightHudSnapshot.decode(encoded);
        assertTrue(decoded.enabled());
        assertEquals(3, decoded.scoreLeft());
        assertEquals(500, decoded.bitsRight());
        assertTrue(decoded.topBits().contains("Zed"));
    }
}
