package com.azscompanions.entity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared policy for companion entity chunk tickets (parents and child Bits).
 * Loader modules hold the actual Fabric / NeoForge tickets; this tracks per-owner caps.
 * <p>
 * Not FTB claims — vanilla/mod force-load tickets so AI/follow/sleep keep running.
 */
public final class CompanionChunkLoading {
    /** Default: keep companions ticking when the player walks away (server cost note in config). */
    public static final boolean DEFAULT_ENABLED = true;
    /**
     * Max forced chunks per owner (each summoned companion/Bit uses one).
     * Default covers a primary + child Bits + a little fight-spawn headroom.
     */
    public static final int DEFAULT_MAX_FORCED_CHUNKS_PER_PLAYER = 16;

    /** owner → companion entity UUIDs that currently hold a chunk ticket */
    private static final Map<UUID, Set<UUID>> HELD_BY_OWNER = new ConcurrentHashMap<>();

    private CompanionChunkLoading() {
    }

    public static int clampMaxForcedChunks(int max) {
        return Math.max(1, Math.min(64, max));
    }

    /** How many companions (including children) for this owner currently hold tickets. */
    public static int heldCount(UUID ownerUuid) {
        if (ownerUuid == null) {
            return 0;
        }
        Set<UUID> set = HELD_BY_OWNER.get(ownerUuid);
        return set == null ? 0 : set.size();
    }

    public static boolean isHolding(UUID ownerUuid, UUID companionUuid) {
        if (ownerUuid == null || companionUuid == null) {
            return false;
        }
        Set<UUID> set = HELD_BY_OWNER.get(ownerUuid);
        return set != null && set.contains(companionUuid);
    }

    /**
     * Reserve a cap slot for {@code companionUuid} under {@code ownerUuid}.
     * @return false if over cap (and not already holding)
     */
    public static boolean tryAcquire(UUID ownerUuid, UUID companionUuid, int maxForcedChunks) {
        if (companionUuid == null) {
            return false;
        }
        UUID owner = ownerUuid != null ? ownerUuid : companionUuid;
        int cap = clampMaxForcedChunks(maxForcedChunks);
        Set<UUID> set = HELD_BY_OWNER.computeIfAbsent(owner, u -> ConcurrentHashMap.newKeySet());
        if (set.contains(companionUuid)) {
            return true;
        }
        if (set.size() >= cap) {
            return false;
        }
        return set.add(companionUuid);
    }

    public static void release(UUID ownerUuid, UUID companionUuid) {
        if (companionUuid == null) {
            return;
        }
        if (ownerUuid != null) {
            releaseFromOwner(ownerUuid, companionUuid);
        }
        // Also scrub if owner key was the companion itself or changed.
        for (Map.Entry<UUID, Set<UUID>> e : HELD_BY_OWNER.entrySet()) {
            if (e.getValue().remove(companionUuid) && e.getValue().isEmpty()) {
                HELD_BY_OWNER.remove(e.getKey(), e.getValue());
            }
        }
    }

    private static void releaseFromOwner(UUID ownerUuid, UUID companionUuid) {
        Set<UUID> set = HELD_BY_OWNER.get(ownerUuid);
        if (set == null) {
            return;
        }
        set.remove(companionUuid);
        if (set.isEmpty()) {
            HELD_BY_OWNER.remove(ownerUuid, set);
        }
    }

    /** Test / server-stop helper. */
    public static void clearAll() {
        HELD_BY_OWNER.clear();
    }
}
