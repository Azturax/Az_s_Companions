package com.azscompanions.ai;

import com.azscompanions.compat.ftb.FtbCompat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionAiActionTrustFtbTest {
    @AfterEach
    void resetFtb() {
        FtbCompat.clear();
    }

    @Test
    void sameTeamElevatesWhenConfigured() {
        CompanionAiSettings settings = new CompanionAiSettings().setTrustSameTeamAsOwner(true);
        assertEquals(CompanionAiActionTrust.OWNER,
                CompanionAiActionTrust.forSpeaker(false, true, settings));
        assertEquals(CompanionAiActionTrust.STRANGER,
                CompanionAiActionTrust.forSpeaker(false, true,
                        new CompanionAiSettings().setTrustSameTeamAsOwner(false)));
    }

    @Test
    void ownerAlwaysOwner() {
        assertEquals(CompanionAiActionTrust.OWNER,
                CompanionAiActionTrust.forSpeaker(true, false, new CompanionAiSettings()));
    }

    @Test
    void absentHooksAreSafe() {
        FtbCompat.clear();
        assertFalse(FtbCompat.arePlayersInSameTeam(UUID.randomUUID(), UUID.randomUUID()));
        assertTrue(FtbCompat.mayAsk(new Object()));
        assertTrue(FtbCompat.mayAiActions(new Object()));
        assertFalse(FtbCompat.shouldPreventBlockEdit(null, null, null));
        assertEquals("unavailable", FtbCompat.claimChunkAsOwner(new Object(), new Object(), 0, 0));
        assertFalse(FtbCompat.aiClaimEnabled());
        assertTrue(FtbCompat.chunksAllowPresence());
    }

    @Test
    void claimToolsAreOwnerOnly() {
        assertFalse(CompanionAiActionTrust.STRANGER.allows(CompanionAiActionNames.CLAIM_CHUNK));
        assertFalse(CompanionAiActionTrust.STRANGER.allows(CompanionAiActionNames.UNCLAIM_CHUNK));
        assertTrue(CompanionAiActionTrust.OWNER.allows(CompanionAiActionNames.CLAIM_CHUNK));
    }
}
