package com.azscompanions.compat.hosted;

import com.azscompanions.ai.CompanionAiSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Soft-compat for Essential / e4mc / World Host / Open-to-LAN style hosted worlds.
 * <p>
 * The host runs an <em>integrated</em> server ({@code isDedicatedServer=false}) while remote
 * friends join. Ownership, AI ask/listen, and CCI stay server-authoritative on that host process.
 * Dedicated servers are unchanged — name fallback and hosted-world LLM override stay off there.
 */
public final class IntegratedMultiplayerCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("azscompanions/hosted");

    private static volatile List<String> presentHostMods = List.of();
    private static final AtomicBoolean integratedMultiplayerActive = new AtomicBoolean(false);
    private static final AtomicBoolean dedicatedServer = new AtomicBoolean(false);
    private static final AtomicBoolean loggedActive = new AtomicBoolean(false);

    private IntegratedMultiplayerCompat() {
    }

    /**
     * Called once from loader bootstrap with soft-detected host-mod ids.
     */
    public static void installDetectedMods(List<String> present) {
        presentHostMods = present == null ? List.of() : List.copyOf(present);
        if (!presentHostMods.isEmpty()) {
            LOGGER.info(
                    "Hosted-world soft-compat: detected {} (Essential/e4mc/World Host/LAN). "
                            + "Friends joining the integrated host use this process's AI + ownership.",
                    HostedWorldMods.describe(presentHostMods));
        }
    }

    public static List<String> presentHostMods() {
        return presentHostMods;
    }

    public static boolean anyHostModPresent() {
        return !presentHostMods.isEmpty();
    }

    public static boolean essentialPresent() {
        return HostedWorldMods.includesEssential(presentHostMods);
    }

    /**
     * Refresh runtime flags from the live Minecraft server (loader code).
     *
     * @param dedicated    {@code server.isDedicatedServer()}
     * @param published    vanilla Open-to-LAN ({@code server.isPublished()}); Essential may not set this
     * @param playerCount  online players on this integrated/dedicated server
     */
    public static void refreshServerState(boolean dedicated, boolean published, int playerCount) {
        dedicatedServer.set(dedicated);
        if (dedicated) {
            if (integratedMultiplayerActive.getAndSet(false) && loggedActive.getAndSet(false)) {
                LOGGER.debug("Hosted-world mode cleared (dedicated server)");
            }
            return;
        }
        boolean remotePlayers = playerCount > 1;
        boolean active = published || remotePlayers || anyHostModPresent();
        boolean was = integratedMultiplayerActive.getAndSet(active);
        if (active && !was) {
            if (loggedActive.compareAndSet(false, true)) {
                LOGGER.info(
                        "Integrated multiplayer active (published={}, players={}, hostMods={}). "
                                + "Companion AI uses the host config; joining clients need no local LLM keys. "
                                + "CCI applies on the host streamer client.",
                        published, playerCount, HostedWorldMods.describe(presentHostMods));
            }
        } else if (!active && was) {
            loggedActive.set(false);
        }
    }

    public static void clear() {
        integratedMultiplayerActive.set(false);
        dedicatedServer.set(false);
        loggedActive.set(false);
    }

    /** True while an integrated host is in “friends can join” territory. Never true on dedicated. */
    public static boolean isIntegratedMultiplayerActive() {
        return !dedicatedServer.get() && integratedMultiplayerActive.get();
    }

    public static boolean isDedicatedServer() {
        return dedicatedServer.get();
    }

    /**
     * When {@link CompanionAiSettings#integratedMultiplayerSharedLlm()} is on, treat the integrated
     * host like a shared-LLM multiplayer server even if {@code serverLlmOnly=false}.
     * Default for that setting is false (opt-in) — does not silently favor server LLM.
     */
    public static boolean shouldForceSharedHostLlm(CompanionAiSettings settings) {
        if (settings == null || dedicatedServer.get()) {
            return false;
        }
        if (!settings.integratedMultiplayerSharedLlm()) {
            return false;
        }
        return isIntegratedMultiplayerActive();
    }

    /**
     * Soft owner-name match for UUID remaps. Off on dedicated servers always.
     */
    public static boolean ownerNameFallbackEnabled(CompanionAiSettings settings) {
        if (settings == null || !settings.ownerNameFallback() || dedicatedServer.get()) {
            return false;
        }
        return isIntegratedMultiplayerActive();
    }

    /**
     * Heal companion owner UUID when the same profile name rejoins with a different UUID
     * (offline↔online remap). No-op on dedicated.
     *
     * @return true if UUID was updated
     */
    public static boolean tryHealOwnerUuid(
            CompanionAiSettings settings,
            UUIDHolder companion,
            UUID playerUuid,
            String playerName
    ) {
        if (companion == null || playerUuid == null || !ownerNameFallbackEnabled(settings)) {
            return false;
        }
        UUID stored = companion.getOwnerUuid();
        String storedName = companion.getOwnerName();
        if (!PlayerIdentityCompat.shouldHealOwnerUuid(stored, playerUuid, storedName, playerName)) {
            return false;
        }
        companion.setOwnerUuid(playerUuid);
        companion.setOwnerName(PlayerIdentityCompat.normalizeName(playerName));
        LOGGER.info(
                "Healed companion owner UUID for {} (name match after hosted-world remap)",
                PlayerIdentityCompat.normalizeName(playerName));
        return true;
    }

    /** Minimal owner accessors so entities can heal without a Minecraft type in common. */
    public interface UUIDHolder {
        UUID getOwnerUuid();

        void setOwnerUuid(UUID uuid);

        String getOwnerName();

        void setOwnerName(String name);
    }
}
