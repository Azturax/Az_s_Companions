package com.azscompanions.world;

import com.azscompanions.entity.CompanionPlayerPersistence;
import com.azscompanions.util.NbtUuids;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Overworld SavedData: player UUID → companion-identity key → settings/inventory payload. */
public final class CompanionPlayerStore extends SavedData {
    public static final Codec<CompanionPlayerStore> CODEC = CompoundTag.CODEC.xmap(
            CompanionPlayerStore::loadFromTag,
            CompanionPlayerStore::saveToTag);

    public static final SavedDataType<CompanionPlayerStore> TYPE = new SavedDataType<>(
            CompanionPlayerPersistence.DATA_NAME,
            CompanionPlayerStore::new,
            CODEC,
            DataFixTypes.LEVEL);

    private final Map<UUID, Map<String, CompoundTag>> byPlayer = new HashMap<>();

    public CompanionPlayerStore() {
    }

    public static CompanionPlayerStore get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(TYPE);
    }

    private static CompanionPlayerStore loadFromTag(CompoundTag tag) {
        CompanionPlayerStore store = new CompanionPlayerStore();
        ListTag players = tag.getListOrEmpty(CompanionPlayerPersistence.TAG_PLAYERS);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag playerEntry = players.getCompoundOrEmpty(i);
            if (!NbtUuids.has(playerEntry, CompanionPlayerPersistence.TAG_PLAYER_UUID)) {
                continue;
            }
            UUID playerId = NbtUuids.get(playerEntry, CompanionPlayerPersistence.TAG_PLAYER_UUID);
            ListTag companions = playerEntry.getListOrEmpty(CompanionPlayerPersistence.TAG_COMPANIONS);
            Map<String, CompoundTag> byKey = new HashMap<>();
            for (int j = 0; j < companions.size(); j++) {
                CompoundTag companionEntry = companions.getCompoundOrEmpty(j);
                String key = companionEntry.getStringOr(CompanionPlayerPersistence.TAG_COMPANION_KEY, "");
                if (key.isBlank()) {
                    continue;
                }
                CompoundTag data = companionEntry.contains(CompanionPlayerPersistence.TAG_DATA)
                        ? companionEntry.getCompoundOrEmpty(CompanionPlayerPersistence.TAG_DATA)
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

    private CompoundTag saveToTag() {
        CompoundTag tag = new CompoundTag();
        ListTag players = new ListTag();
        for (Map.Entry<UUID, Map<String, CompoundTag>> player : byPlayer.entrySet()) {
            if (player.getValue() == null || player.getValue().isEmpty()) {
                continue;
            }
            CompoundTag playerEntry = new CompoundTag();
            NbtUuids.put(playerEntry, CompanionPlayerPersistence.TAG_PLAYER_UUID, player.getKey());
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
