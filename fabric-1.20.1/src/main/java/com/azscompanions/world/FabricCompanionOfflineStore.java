package com.azscompanions.world;

import com.azscompanions.entity.CompanionLogoutPersistence;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fabric offline companion parking (player persistent-data equivalent).
 * Stored on the overworld data storage so snapshots survive disconnect.
 */
public final class FabricCompanionOfflineStore extends SavedData {
    public static final String DATA_NAME = "azscompanions_logout_companions";

    private static final String TAG_PLAYERS = "Players";
    private static final String TAG_PLAYER = "Player";
    private static final String TAG_COMPANIONS = "Companions";

    private final Map<UUID, ListTag> byPlayer = new HashMap<>();

    public FabricCompanionOfflineStore() {
    }

    public static FabricCompanionOfflineStore get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(FabricCompanionOfflineStore::load, FabricCompanionOfflineStore::new, DATA_NAME);
    }

    public static FabricCompanionOfflineStore load(CompoundTag tag) {
        FabricCompanionOfflineStore store = new FabricCompanionOfflineStore();
        ListTag players = tag.getList(TAG_PLAYERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag entry = players.getCompound(i);
            if (!entry.hasUUID(TAG_PLAYER)) {
                continue;
            }
            UUID playerId = entry.getUUID(TAG_PLAYER);
            ListTag companions = entry.contains(TAG_COMPANIONS, Tag.TAG_LIST)
                    ? entry.getList(TAG_COMPANIONS, Tag.TAG_COMPOUND)
                    : new ListTag();
            if (!companions.isEmpty()) {
                store.byPlayer.put(playerId, companions.copy());
            }
        }
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag players = new ListTag();
        for (Map.Entry<UUID, ListTag> e : byPlayer.entrySet()) {
            if (e.getValue() == null || e.getValue().isEmpty()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putUUID(TAG_PLAYER, e.getKey());
            entry.put(TAG_COMPANIONS, e.getValue().copy());
            players.add(entry);
        }
        tag.put(TAG_PLAYERS, players);
        return tag;
    }

    public ListTag take(UUID playerId) {
        ListTag list = byPlayer.remove(playerId);
        setDirty();
        return list != null ? list : new ListTag();
    }

    public void put(UUID playerId, ListTag companions) {
        if (companions == null || companions.isEmpty()) {
            byPlayer.remove(playerId);
        } else {
            byPlayer.put(playerId, companions.copy());
        }
        setDirty();
    }

    /** Stable key shared with NeoForge player persistent data ({@link CompanionLogoutPersistence#PLAYER_LIST_TAG}). */
    public static String playerListTag() {
        return CompanionLogoutPersistence.PLAYER_LIST_TAG;
    }
}
