package com.azscompanions.world;

import com.azscompanions.entity.CompanionPlayerPersistence;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Overworld SavedData: player UUID → companion-identity key → settings/inventory payload.
 * Survives logout, dimension travel, dismiss/re-summon, and companion entity recreation.
 */
public final class CompanionPlayerStore extends SavedData {
    private final Map<UUID, Map<String, CompoundTag>> byPlayer = new HashMap<>();

    public CompanionPlayerStore() {
    }

    public static CompanionPlayerStore get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(
                new Factory<>(CompanionPlayerStore::new, CompanionPlayerStore::load, DataFixTypes.LEVEL),
                CompanionPlayerPersistence.DATA_NAME);
    }

    public static CompanionPlayerStore load(CompoundTag tag, HolderLookup.Provider provider) {
        CompanionPlayerStore store = new CompanionPlayerStore();
        ListTag players = tag.getList(CompanionPlayerPersistence.TAG_PLAYERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag playerEntry = players.getCompound(i);
            if (!playerEntry.hasUUID(CompanionPlayerPersistence.TAG_PLAYER_UUID)) {
                continue;
            }
            UUID playerId = playerEntry.getUUID(CompanionPlayerPersistence.TAG_PLAYER_UUID);
            ListTag companions = playerEntry.contains(CompanionPlayerPersistence.TAG_COMPANIONS, Tag.TAG_LIST)
                    ? playerEntry.getList(CompanionPlayerPersistence.TAG_COMPANIONS, Tag.TAG_COMPOUND)
                    : new ListTag();
            Map<String, CompoundTag> byKey = new HashMap<>();
            for (int j = 0; j < companions.size(); j++) {
                CompoundTag companionEntry = companions.getCompound(j);
                String key = companionEntry.getString(CompanionPlayerPersistence.TAG_COMPANION_KEY);
                if (key == null || key.isBlank()) {
                    continue;
                }
                CompoundTag data = companionEntry.contains(CompanionPlayerPersistence.TAG_DATA, Tag.TAG_COMPOUND)
                        ? companionEntry.getCompound(CompanionPlayerPersistence.TAG_DATA)
                        : new CompoundTag();
                if (!data.isEmpty()) {
                    byKey.put(key, data.copy());
                }
            }
            if (!byKey.isEmpty()) {
                store.byPlayer.put(playerId, byKey);
            }
        }
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag players = new ListTag();
        for (Map.Entry<UUID, Map<String, CompoundTag>> player : byPlayer.entrySet()) {
            if (player.getValue() == null || player.getValue().isEmpty()) {
                continue;
            }
            CompoundTag playerEntry = new CompoundTag();
            playerEntry.putUUID(CompanionPlayerPersistence.TAG_PLAYER_UUID, player.getKey());
            ListTag companions = new ListTag();
            for (Map.Entry<String, CompoundTag> companion : player.getValue().entrySet()) {
                if (companion.getValue() == null || companion.getValue().isEmpty()) {
                    continue;
                }
                CompoundTag companionEntry = new CompoundTag();
                companionEntry.putString(CompanionPlayerPersistence.TAG_COMPANION_KEY, companion.getKey());
                companionEntry.put(CompanionPlayerPersistence.TAG_DATA, companion.getValue().copy());
                companions.add(companionEntry);
            }
            if (companions.isEmpty()) {
                continue;
            }
            playerEntry.put(CompanionPlayerPersistence.TAG_COMPANIONS, companions);
            players.add(playerEntry);
        }
        tag.put(CompanionPlayerPersistence.TAG_PLAYERS, players);
        return tag;
    }

    public void put(UUID playerId, String companionKey, CompoundTag payload) {
        if (playerId == null || companionKey == null || companionKey.isBlank()
                || payload == null || payload.isEmpty()) {
            return;
        }
        byPlayer.computeIfAbsent(playerId, id -> new HashMap<>()).put(companionKey, payload.copy());
        setDirty();
    }

    @Nullable
    public CompoundTag peek(UUID playerId, String companionKey) {
        if (playerId == null || companionKey == null) {
            return null;
        }
        Map<String, CompoundTag> byKey = byPlayer.get(playerId);
        if (byKey == null) {
            return null;
        }
        CompoundTag data = byKey.get(companionKey);
        return data == null || data.isEmpty() ? null : data.copy();
    }
}
