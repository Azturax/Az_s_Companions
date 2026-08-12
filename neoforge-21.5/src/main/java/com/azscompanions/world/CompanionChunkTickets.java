package com.azscompanions.world;

import com.azscompanions.util.OwnableUuids;

import com.azscompanions.AzsCompanions;
import com.azscompanions.config.ServerConfig;
import com.azscompanions.entity.CompanionChunkLoading;
import com.azscompanions.entity.CompanionEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NeoForge entity chunk tickets for summoned companions and child Bits.
 * Keeps the occupied chunk entity-ticking while the companion is alive.
 */
public final class CompanionChunkTickets {
    public static final TicketController CONTROLLER = new TicketController(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "companion"),
            CompanionChunkTickets::validateTickets);

    private static final Map<UUID, Held> HELD = new ConcurrentHashMap<>();

    private CompanionChunkTickets() {
    }

    public static void register(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }

    /**
     * Sync ticket to the companion's current chunk. Call from server tick while alive.
     * Parents and children use the same path; both count toward {@code maxForcedChunksPerPlayer}.
     */
    public static void tick(CompanionEntity companion, ServerLevel level) {
        if (!ServerConfig.COMPANION_CHUNK_LOADING.get() || !companion.isChunkLoadingEnabled()) {
            release(companion);
            return;
        }
        if (!companion.isAlive()) {
            release(companion);
            return;
        }
        UUID ownerUuid = OwnableUuids.get(companion);
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
            int max = ServerConfig.MAX_FORCED_CHUNKS_PER_PLAYER.get();
            if (!CompanionChunkLoading.tryAcquire(ownerUuid, id, max)) {
                return;
            }
        } else {
            force(level, companion, held.chunk, false);
            if (!held.dimension.equals(level.dimension())) {
                ServerLevel oldLevel = level.getServer().getLevel(held.dimension);
                if (oldLevel != null) {
                    force(oldLevel, companion, held.chunk, false);
                }
            }
        }

        force(level, companion, pos, true);
        HELD.put(id, new Held(level.dimension(), pos, ownerUuid));
    }

    /** Release on despawn, death, charm store, or remove. */
    public static void release(CompanionEntity companion) {
        if (companion == null) {
            return;
        }
        release(companion.getUUID(), companion.level() instanceof ServerLevel sl ? sl : null, companion);
    }

    public static void release(UUID companionId, ServerLevel hintLevel, CompanionEntity entity) {
        Held held = HELD.remove(companionId);
        UUID owner = held != null ? held.ownerUuid : (entity != null ? OwnableUuids.get(entity) : null);
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
            if (entity != null) {
                force(level, entity, held.chunk, false);
            } else {
                CONTROLLER.forceChunk(level, companionId, held.chunk.x, held.chunk.z, false, true);
            }
        }
    }

    private static void force(ServerLevel level, CompanionEntity companion, ChunkPos pos, boolean add) {
        CONTROLLER.forceChunk(level, companion, pos.x, pos.z, add, true);
    }

    private static boolean ownerInSameDimension(ServerLevel level, UUID ownerUuid) {
        if (ownerUuid == null || level.getServer() == null) {
            return true;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUuid);
        if (owner == null) {
            // Owner offline: companions are parked on logout; no ticket needed.
            return false;
        }
        return owner.level() == level;
    }

    /**
     * Drop persisted tickets on world load; living companions re-acquire on tick.
     * Avoids stale force-loads after charm-store / crash without a clean release.
     */
    private static void validateTickets(ServerLevel level, TicketHelper helper) {
        Set<UUID> owners = Set.copyOf(helper.getEntityTickets().keySet());
        for (UUID id : owners) {
            helper.removeAllTickets(id);
            CompanionChunkLoading.release(null, id);
        }
        HELD.entrySet().removeIf(e -> {
            if (e.getValue().dimension.equals(level.dimension())) {
                CompanionChunkLoading.release(e.getValue().ownerUuid(), e.getKey());
                return true;
            }
            return false;
        });
    }

    private record Held(ResourceKey<Level> dimension, ChunkPos chunk, UUID ownerUuid) {
    }
}
