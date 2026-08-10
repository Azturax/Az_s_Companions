package com.azscompanions.compat.hosted;

import com.azscompanions.ai.CompanionAiRuntime;
import com.azscompanions.ai.CompanionAiSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HostedWorldCompatTest {
    @AfterEach
    void tearDown() {
        IntegratedMultiplayerCompat.clear();
        CompanionAiRuntime.get().clearServerContext();
        CompanionAiRuntime.get().applySettings(new CompanionAiSettings());
    }

    @Test
    void detectsKnownHostMods() {
        List<String> found = HostedWorldMods.detectPresent(id ->
                id.equals("essential") || id.equals("e4mc"));
        assertTrue(found.contains("essential"));
        assertTrue(found.contains("e4mc"));
        assertTrue(HostedWorldMods.includesEssential(found));
        assertEquals("Essential", HostedWorldMods.displayName("essential"));
    }

    @Test
    void uuidOwnerAlwaysMatches() {
        UUID id = UUID.randomUUID();
        assertTrue(PlayerIdentityCompat.isOwner(id, "Az", id, "Az", false));
        assertFalse(PlayerIdentityCompat.isOwner(id, "Az", UUID.randomUUID(), "Az", false));
    }

    @Test
    void nameFallbackOnlyWhenAllowed() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        assertFalse(PlayerIdentityCompat.isOwner(owner, "Host", other, "Host", false));
        assertTrue(PlayerIdentityCompat.isOwner(owner, "Host", other, "Host", true));
        assertFalse(PlayerIdentityCompat.isOwner(owner, "Host", other, "Guest", true));
    }

    @Test
    void dedicatedNeverEnablesNameFallback() {
        CompanionAiSettings settings = new CompanionAiSettings().setOwnerNameFallback(true);
        IntegratedMultiplayerCompat.refreshServerState(true, false, 4);
        assertFalse(IntegratedMultiplayerCompat.ownerNameFallbackEnabled(settings));
        assertFalse(IntegratedMultiplayerCompat.shouldForceSharedHostLlm(settings));
    }

    @Test
    void integratedHostForcesSharedLlmWhenConfigured() {
        CompanionAiSettings settings = new CompanionAiSettings()
                .setServerLlmOnly(false)
                .setIntegratedMultiplayerSharedLlm(true);
        CompanionAiRuntime.get().applySettings(settings);
        CompanionAiRuntime.get().markServerContext(false);
        IntegratedMultiplayerCompat.installDetectedMods(List.of("essential"));
        IntegratedMultiplayerCompat.refreshServerState(false, false, 1);
        assertTrue(IntegratedMultiplayerCompat.isIntegratedMultiplayerActive());
        assertTrue(CompanionAiRuntime.get().usesSharedServerLlm());
        assertTrue(CompanionAiRuntime.get().statusLine().contains("[hosted MP]"));
    }

    @Test
    void healsOwnerUuidOnNameMatch() {
        UUID oldId = UUID.randomUUID();
        UUID newId = UUID.randomUUID();
        AtomicReference<UUID> stored = new AtomicReference<>(oldId);
        AtomicReference<String> name = new AtomicReference<>("Streamer");
        CompanionAiSettings settings = new CompanionAiSettings().setOwnerNameFallback(true);
        IntegratedMultiplayerCompat.installDetectedMods(List.of("e4mc"));
        IntegratedMultiplayerCompat.refreshServerState(false, true, 2);
        assertTrue(IntegratedMultiplayerCompat.tryHealOwnerUuid(settings, new IntegratedMultiplayerCompat.UUIDHolder() {
            @Override
            public UUID getOwnerUuid() {
                return stored.get();
            }

            @Override
            public void setOwnerUuid(UUID uuid) {
                stored.set(uuid);
            }

            @Override
            public String getOwnerName() {
                return name.get();
            }

            @Override
            public void setOwnerName(String n) {
                name.set(n);
            }
        }, newId, "Streamer"));
        assertEquals(newId, stored.get());
    }
}
