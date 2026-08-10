package com.azscompanions.compat.ftb;

import java.util.UUID;

/**
 * Soft FTB bridge. Loaders install a reflection implementation when FTB mods are present;
 * otherwise {@link #NOOP} keeps the mod running without FTB.
 */
public interface FtbCompatHooks {
    FtbCompatHooks NOOP = new FtbCompatHooks() {
    };

    default boolean teamsAvailable() {
        return false;
    }

    default boolean chunksAvailable() {
        return false;
    }

    default boolean ranksAvailable() {
        return false;
    }

    /** True when both UUIDs share an FTB team (party or otherwise). */
    default boolean arePlayersInSameTeam(UUID playerA, UUID playerB) {
        return false;
    }

    /**
     * True when FTB Chunks would prevent the player from editing or interacting with the block
     * (foreign claim). Does not affect walking/pathfinding — presence is always allowed.
     */
    default boolean shouldPreventBlockEdit(Object level, Object blockPos, Object serverPlayer) {
        return false;
    }

    /**
     * Claim a chunk for the player (uses their quota/team). Returns a short status string:
     * {@code ok}, {@code unavailable}, {@code failed:...}.
     */
    default String claimChunkAsPlayer(Object serverPlayer, Object resourceKeyDimension, int chunkX, int chunkZ) {
        return "unavailable";
    }

    /**
     * Unclaim a chunk owned by the player's team. Never steals others' claims
     * ({@code adminOverride=false}).
     */
    default String unclaimChunkAsPlayer(Object serverPlayer, Object resourceKeyDimension, int chunkX, int chunkZ) {
        return "unavailable";
    }

    /**
     * FTB Ranks boolean node. When the node is missing, returns {@code defaultIfMissing}.
     * When ranks are absent, returns {@code defaultIfMissing}.
     */
    default boolean hasPermission(Object serverPlayer, String node, boolean defaultIfMissing) {
        return defaultIfMissing;
    }
}
