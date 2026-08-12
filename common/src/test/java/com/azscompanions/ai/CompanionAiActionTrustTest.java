package com.azscompanions.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionAiActionTrustTest {
    @Test
    void strangerAllowsSocialBlocksGrief() {
        assertTrue(CompanionAiActionTrust.STRANGER.allows(CompanionAiActionNames.DANCE));
        assertTrue(CompanionAiActionTrust.STRANGER.allows(CompanionAiActionNames.COME_HERE));
        assertTrue(CompanionAiActionTrust.STRANGER.allows(CompanionAiActionNames.SAY));
        assertFalse(CompanionAiActionTrust.STRANGER.allows(CompanionAiActionNames.MINE));
        assertFalse(CompanionAiActionTrust.STRANGER.allows(CompanionAiActionNames.DROP));
        assertFalse(CompanionAiActionTrust.STRANGER.allows(CompanionAiActionNames.FOLLOW));
        assertFalse(CompanionAiActionTrust.STRANGER.allows(CompanionAiActionNames.PICKUP));
    }

    @Test
    void filterDropsBlocked() {
        List<CompanionAiAction> in = List.of(
                new CompanionAiAction(CompanionAiActionNames.DANCE, null),
                new CompanionAiAction(CompanionAiActionNames.MINE, null),
                new CompanionAiAction(CompanionAiActionNames.DROP, null)
        );
        List<CompanionAiAction> out = CompanionAiActionTrust.STRANGER.filter(in);
        assertEquals(1, out.size());
        assertEquals(CompanionAiActionNames.DANCE, out.get(0).name());
    }

    @Test
    void ownerKeepsAll() {
        List<CompanionAiAction> in = List.of(new CompanionAiAction(CompanionAiActionNames.MINE, null));
        assertEquals(1, CompanionAiActionTrust.OWNER.filter(in).size());
    }
}
