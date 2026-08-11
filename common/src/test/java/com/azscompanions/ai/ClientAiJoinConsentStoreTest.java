package com.azscompanions.ai;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientAiJoinConsentStoreTest {
    @TempDir
    Path temp;

    @AfterEach
    void tearDown() {
        ClientAiJoinConsent.resetForTests();
    }

    @Test
    void persistsAcceptAndDismissAcrossReload() throws Exception {
        Path file = temp.resolve(ClientAiJoinConsentStore.FILE_NAME);
        ClientAiJoinConsent.configureStore(file);

        ClientAiJoinConsent.markAccepted("play.example.com");
        ClientAiJoinConsent.markDismissed("integrated");
        assertFalse(ClientAiJoinConsent.shouldPrompt("play.example.com"));
        assertFalse(ClientAiJoinConsent.shouldPrompt("INTEGRATED"));

        ClientAiJoinConsent.resetForTests();
        ClientAiJoinConsent.configureStore(file);

        assertEquals(ClientAiJoinConsent.Decision.ACCEPTED,
                ClientAiJoinConsent.decisionFor("Play.Example.Com"));
        assertEquals(ClientAiJoinConsent.Decision.DISMISSED,
                ClientAiJoinConsent.decisionFor("integrated"));
        assertTrue(ClientAiJoinConsent.shouldPrompt("other.server"));
    }

    @Test
    void unknownKeyIsNotPersisted() throws Exception {
        Path file = temp.resolve(ClientAiJoinConsentStore.FILE_NAME);
        ClientAiJoinConsent.configureStore(file);
        ClientAiJoinConsent.markDismissed("unknown");
        ClientAiJoinConsent.markAccepted("  ");
        assertTrue(ClientAiJoinConsent.shouldPrompt("unknown"));

        ClientAiJoinConsent.resetForTests();
        ClientAiJoinConsent.configureStore(file);
        assertTrue(ClientAiJoinConsent.shouldPrompt("unknown"));
    }

    @Test
    void parseDecisionAliases() {
        assertEquals(ClientAiJoinConsent.Decision.ACCEPTED,
                ClientAiJoinConsentStore.parseDecision("yes"));
        assertEquals(ClientAiJoinConsent.Decision.DISMISSED,
                ClientAiJoinConsentStore.parseDecision("no"));
        assertEquals(ClientAiJoinConsent.Decision.UNDECIDED,
                ClientAiJoinConsentStore.parseDecision("maybe"));
    }
}
