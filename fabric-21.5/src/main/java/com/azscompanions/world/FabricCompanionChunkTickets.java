package com.azscompanions.world;

import com.azscompanions.config.FabricServerConfig;
import com.azscompanions.entity.CompanionChunkLoading;
import com.azscompanions.entity.FabricCompanionEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fabric entity chunk tickets for summoned companions and child Bits.
 * Uses a non-expiring {@link TicketType} so AI/follow/sleep keep running off-player.
 * 1.21.5 tickets have no payload key — refcount companions per chunk.
 */
public final class FabricCompanionChunkTickets {
    /**
     * Radius 2 → entity-ticking ticket level for the companion's chunk.
     * (Vanilla distance manager: ticket level ≈ 33 − radius.)
     */
    private static final int TICKET_RADIUS = 2;

    /** Distinct from {@link TicketType#FORCED} (persist=true) so /forceload is untouched. */
    public static final TicketType COMPANION_TICKET = new TicketType(
            TicketType.NO_TIMEOUT, false, TicketType.TicketUse.LOADING_AND_SIMULATION);

    private static final Map<UUID, Held> HELD = new ConcurrentHashMap<>();
    private static final Map<ChunkKey, AtomicInteger> CHUNK_REFS = new ConcurrentHashMap<>();

    private FabricCompanionChunkTickets() {
    }

    /**
     * Sync ticket to the companion's current chunk. Call from server tick while alive.
     * Parents and children use the same path; both count toward the per-player cap.
     */
    public static void tick(FabricCompanionEntity companion, ServerLevel level) {
        if (!FabricServerConfig.COMPANION_CHUNK_LOADING || !companion.isChunkLoadingEnabled()) {
            release(companion);
            return;
        }
        if (!companion.isAlive()) {
            release(companion);
            return;
        }
        UUID ownerUuid = companion.getOwnerUuid();
        if (!ownerInSameDimension(level, ownerUuid)) {
            release(companion);
            return;
        }

        ChunkPos pos = companion.chunkPosition();
        UUID id = companion.getUUID();
        Held held = HELD.get(id);
        if (held != null
                && held.dimension.equals(level.dimension())
                && held.chunk.equals(pos)) {
            return;
        }

        if (held == null) {
            if (!CompanionChunkLoading.tryAcquire(
                    ownerUuid, id, FabricServerConfig.MAX_FORCED_CHUNKS_PER_PLAYER)) {
                return;
            }
        } else {
            releaseChunkTicket(level, held);
            if (!held.dimension.equals(level.dimension()) && level.getServer() != null) {
                ServerLevel oldLevel = level.getServer().getLevel(held.dimension);
                if (oldLevel != null) {
                    releaseChunkTicket(oldLevel, held);
                }
            }
        }

        acquireChunkTicket(level, pos);
        HELD.put(id, new Held(level.dimension(), pos, ownerUuid));
    }

    public static void release(FabricCompanionEntity companion) {
        if (companion == null) {
            return;
        }
        release(companion.getUUID(), companion.level() instanceof ServerLevel sl ? sl : null, companion);
    }

    public static void release(UUID companionId, ServerLevel hintLevel, FabricCompanionEntity entity) {
        Held held = HELD.remove(companionId);
        UUID owner = held != null ? held.ownerUuid : (entity != null ? entity.getOwnerUuid() : null);
        CompanionChunkLoading.release(owner, companionId);
        if (held == null) {
            return;
        }
        ServerLevel level = hintLevel;
        if (level == null || !level.dimension().equals(held.dimension)) {
            if (hintLevel != null && hintLevel.getServer() != null) {
                level = hintLevel.getServer().getLevel(held.dimension);
            }
        }
        if (level != null) {
            releaseChunkTicket(level, held);
        }
    }

    private static void acquireChunkTicket(ServerLevel level, ChunkPos pos) {
        ChunkKey key = new ChunkKey(level.dimension(), pos);
        AtomicInteger refs = CHUNK_REFS.computeIfAbsent(key, k -> new AtomicInteger(0));
        if (refs.getAndIncrement() == 0) {
            level.getChunkSource().addTicketWithRadius(COMPANION_TICKET, pos, TICKET_RADIUS);
        }
    }

    private static void releaseChunkTicket(ServerLevel level, Held held) {
        ChunkKey key = new ChunkKey(held.dimension, held.chunk);
        AtomicInteger refs = CHUNK_REFS.get(key);
        if (refs == null) {
            return;
        }
        if (refs.decrementAndGet() <= 0) {
            CHUNK_REFS.remove(key, refs);
            level.getChunkSource().removeTicketWithRadius(COMPANION_TICKET, held.chunk, TICKET_RADIUS);
        }
    }

    private static boolean ownerInSameDimension(ServerLevel level, UUID ownerUuid) {
        if (ownerUuid == null || level.getServer() == null) {
            return true;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUuid);
        if (owner == null) {
            return false;
        }
        return owner.level() == level;
    }

    private record Held(ResourceKey<Level> dimension, ChunkPos chunk, UUID ownerUuid) {
    }

    private record ChunkKey(ResourceKey<Level> dimension, ChunkPos chunk) {
    }
}
