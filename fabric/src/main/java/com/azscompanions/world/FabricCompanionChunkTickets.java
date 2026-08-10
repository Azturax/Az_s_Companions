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

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabric entity chunk tickets for summoned companions and child Bits.
 * Uses a non-expiring {@link TicketType} so AI/follow/sleep keep running off-player.
 */
public final class FabricCompanionChunkTickets {
    /**
     * Radius 2 → entity-ticking ticket level for the companion's chunk.
     * (Vanilla distance manager: ticket level ≈ 33 − radius.)
     */
    private static final int TICKET_RADIUS = 2;

    public static final TicketType<UUID> COMPANION_TICKET =
            TicketType.create("azscompanions:companion", Comparator.<UUID>naturalOrder());

    private static final Map<UUID, Held> HELD = new ConcurrentHashMap<>();

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
            removeTicket(level, id, held);
            if (!held.dimension.equals(level.dimension()) && level.getServer() != null) {
                ServerLevel oldLevel = level.getServer().getLevel(held.dimension);
                if (oldLevel != null) {
                    removeTicket(oldLevel, id, held);
                }
            }
        }

        level.getChunkSource().addRegionTicket(COMPANION_TICKET, pos, TICKET_RADIUS, id);
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
            removeTicket(level, companionId, held);
        }
    }

    private static void removeTicket(ServerLevel level, UUID companionId, Held held) {
        level.getChunkSource().removeRegionTicket(COMPANION_TICKET, held.chunk, TICKET_RADIUS, companionId);
    }

    private static boolean ownerInSameDimension(ServerLevel level, UUID ownerUuid) {
        if (ownerUuid == null || level.getServer() == null) {
            return true;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUuid);
        if (owner == null) {
            return true;
        }
        return owner.level() == level;
    }

    private record Held(ResourceKey<Level> dimension, ChunkPos chunk, UUID ownerUuid) {
    }
}
