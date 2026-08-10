package com.azscompanions.compat.ftb;

import com.azscompanions.ai.CompanionAiActionTrust;
import com.azscompanions.ai.CompanionAiSettings;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Entry point for optional FTB Teams / Chunks / Ranks integration.
 * Safe when FTB is not installed (hooks stay {@link FtbCompatHooks#NOOP}).
 * <p>
 * FTB Chunks: companions may <em>walk</em> into any claim; only block/container
 * interactions are gated by {@link #chunksBlockInteractionEnabled()}.
 */
public final class FtbCompat {
    private static volatile FtbCompatHooks hooks = FtbCompatHooks.NOOP;
    private static volatile Supplier<CompanionAiSettings> settingsSupplier = CompanionAiSettings::new;

    private FtbCompat() {
    }

    public static void install(FtbCompatHooks installed, Supplier<CompanionAiSettings> settings) {
        hooks = installed == null ? FtbCompatHooks.NOOP : installed;
        if (settings != null) {
            settingsSupplier = settings;
        }
    }

    public static void clear() {
        hooks = FtbCompatHooks.NOOP;
        settingsSupplier = CompanionAiSettings::new;
    }

    public static FtbCompatHooks hooks() {
        return hooks;
    }

    public static CompanionAiSettings settings() {
        CompanionAiSettings s = settingsSupplier.get();
        return s == null ? new CompanionAiSettings() : s;
    }

    public static boolean teamsEnabled() {
        CompanionAiSettings s = settings();
        return s.ftbTeamsCompat() && hooks.teamsAvailable();
    }

    /**
     * When true, companions may pathfind / stand in claimed chunks.
     * Always true for gameplay (FTB does not block entity presence); config is documentation + future hooks.
     */
    public static boolean chunksAllowPresence() {
        return settings().ftbChunksAllowPresence();
    }

    /** Block mine/place/build/container/use in claims the owner cannot edit/interact. */
    public static boolean chunksBlockInteractionEnabled() {
        CompanionAiSettings s = settings();
        return s.ftbChunksBlockInteraction() && hooks.chunksAvailable();
    }

    /** @deprecated use {@link #chunksBlockInteractionEnabled()} */
    @Deprecated
    public static boolean chunksProtectEnabled() {
        return chunksBlockInteractionEnabled();
    }

    /** Owner AI may claim/unclaim via FTB Chunks when mod present and config on. */
    public static boolean aiClaimEnabled() {
        CompanionAiSettings s = settings();
        return s.ftbChunksAiClaim() && hooks.chunksAvailable();
    }

    public static boolean ranksEnabled() {
        CompanionAiSettings s = settings();
        return s.ftbRanksCompat() && hooks.ranksAvailable();
    }

    public static boolean arePlayersInSameTeam(UUID a, UUID b) {
        if (a == null || b == null || a.equals(b) || !teamsEnabled()) {
            return false;
        }
        try {
            return hooks.arePlayersInSameTeam(a, b);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isSameTeamAsOwner(UUID ownerId, UUID playerId) {
        return arePlayersInSameTeam(ownerId, playerId);
    }

    public static CompanionAiActionTrust resolveTrust(boolean speakerIsOwner, UUID ownerId, UUID speakerId) {
        return CompanionAiActionTrust.forSpeaker(speakerIsOwner, isSameTeamAsOwner(ownerId, speakerId), settings());
    }

    /**
     * True when the player must not edit/interact at the position (foreign claim).
     * Never used to block walking into the chunk.
     */
    public static boolean shouldPreventBlockEdit(Object level, Object blockPos, Object serverPlayer) {
        if (!chunksBlockInteractionEnabled() || level == null || blockPos == null || serverPlayer == null) {
            return false;
        }
        try {
            return hooks.shouldPreventBlockEdit(level, blockPos, serverPlayer);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static String claimChunkAsOwner(Object serverPlayer, Object dimensionKey, int chunkX, int chunkZ) {
        if (!aiClaimEnabled() || serverPlayer == null || dimensionKey == null) {
            return "unavailable";
        }
        try {
            return hooks.claimChunkAsPlayer(serverPlayer, dimensionKey, chunkX, chunkZ);
        } catch (Throwable t) {
            return "failed:exception";
        }
    }

    public static String unclaimChunkAsOwner(Object serverPlayer, Object dimensionKey, int chunkX, int chunkZ) {
        if (!aiClaimEnabled() || serverPlayer == null || dimensionKey == null) {
            return "unavailable";
        }
        try {
            return hooks.unclaimChunkAsPlayer(serverPlayer, dimensionKey, chunkX, chunkZ);
        } catch (Throwable t) {
            return "failed:exception";
        }
    }

    public static boolean mayAsk(Object serverPlayer) {
        return checkRank(serverPlayer, settings().ftbPermAiAsk(), true);
    }

    public static boolean mayAiActions(Object serverPlayer) {
        return checkRank(serverPlayer, settings().ftbPermAiActions(), true);
    }

    public static boolean mayCci(Object serverPlayer) {
        return checkRank(serverPlayer, settings().ftbPermCci(), true);
    }

    public static boolean mayTeamfight(Object serverPlayer) {
        return checkRank(serverPlayer, settings().ftbPermTeamfight(), true);
    }

    public static boolean maySpawn(Object serverPlayer) {
        return checkRank(serverPlayer, settings().ftbPermSpawn(), true);
    }

    private static boolean checkRank(Object serverPlayer, String node, boolean defaultIfMissing) {
        if (!ranksEnabled() || serverPlayer == null || node == null || node.isBlank()) {
            return defaultIfMissing;
        }
        try {
            return hooks.hasPermission(serverPlayer, node.trim(), defaultIfMissing);
        } catch (Throwable ignored) {
            return defaultIfMissing;
        }
    }
}
