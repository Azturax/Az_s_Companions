package com.azscompanions.world;

import com.azscompanions.ai.CompanionPersona;
import com.azscompanions.entity.CompanionDimensionTravel;
import com.azscompanions.entity.CompanionIdentityPersistence;
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

/**
 * World-save map of companion UUID → full/identity snapshot (global for the save).
 * Backup for charm recovery; dimension travel uses live entity teleport, not this store.
 */
public final class FabricCompanionIdentityStore extends SavedData {
    public static final Codec<FabricCompanionIdentityStore> CODEC = CompoundTag.CODEC.xmap(
            FabricCompanionIdentityStore::loadFromTag,
            FabricCompanionIdentityStore::saveToTag);

    public static final SavedDataType<FabricCompanionIdentityStore> TYPE = new SavedDataType<>(
            CompanionIdentityPersistence.DATA_NAME,
            FabricCompanionIdentityStore::new,
            CODEC,
            DataFixTypes.LEVEL);

    private final Map<UUID, CompoundTag> byCompanion = new HashMap<>();

    public FabricCompanionIdentityStore() {
    }

    public static FabricCompanionIdentityStore get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(TYPE);
    }

    private static FabricCompanionIdentityStore loadFromTag(CompoundTag tag) {
        FabricCompanionIdentityStore store = new FabricCompanionIdentityStore();
        ListTag entries = tag.getListOrEmpty(CompanionIdentityPersistence.TAG_ENTRIES);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompoundOrEmpty(i);
            if (!NbtUuids.has(entry, CompanionIdentityPersistence.ENTRY_UUID)) {
                continue;
            }
            UUID id = NbtUuids.get(entry, CompanionIdentityPersistence.ENTRY_UUID);
            CompoundTag data = entry.contains(CompanionIdentityPersistence.ENTRY_DATA)
                    ? entry.getCompoundOrEmpty(CompanionIdentityPersistence.ENTRY_DATA)
                    : new CompoundTag();
            if (!data.isEmpty()) {
                store.byCompanion.put(id, data.copy());
            }
        }
        return store;
    }

    private CompoundTag saveToTag() {
        CompoundTag tag = new CompoundTag();
        ListTag entries = new ListTag();
        for (Map.Entry<UUID, CompoundTag> e : byCompanion.entrySet()) {
            if (e.getValue() == null || e.getValue().isEmpty()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            NbtUuids.put(entry, CompanionIdentityPersistence.ENTRY_UUID, e.getKey());
            entry.put(CompanionIdentityPersistence.ENTRY_DATA, e.getValue().copy());
            entries.add(entry);
        }
        tag.put(CompanionIdentityPersistence.TAG_ENTRIES, entries);
        return tag;
    }

    public void putIdentity(UUID companionId, CompoundTag fullOrIdentity) {
        if (companionId == null || fullOrIdentity == null || fullOrIdentity.isEmpty()) {
            return;
        }
        byCompanion.put(companionId, fullOrIdentity.copy());
        setDirty();
    }

    @Nullable
    public CompoundTag peekIdentity(UUID companionId) {
        if (companionId == null) {
            return null;
        }
        CompoundTag data = byCompanion.get(companionId);
        return data == null || data.isEmpty() ? null : data.copy();
    }

    public static CompoundTag extractIdentity(CompoundTag full) {
        CompoundTag out = new CompoundTag();
        if (full == null) {
            return out;
        }
        for (String key : CompanionDimensionTravel.IDENTITY_NBT_KEYS) {
            if (full.contains(key)) {
                out.put(key, full.get(key).copy());
            }
        }
        return out;
    }

    public static void mergeIdentityInto(CompoundTag target, CompoundTag identity) {
        if (target == null || identity == null || identity.isEmpty()) {
            return;
        }
        for (String key : CompanionDimensionTravel.IDENTITY_NBT_KEYS) {
            if (identity.contains(key)) {
                target.put(key, identity.get(key).copy());
            }
        }
        boolean initialized = identity.contains(CompanionPersona.NBT_INITIALIZED)
                && identity.getBooleanOr(CompanionPersona.NBT_INITIALIZED, false);
        if (CompanionDimensionTravel.identityMarksPersonaInitialized(
                initialized,
                nonBlank(target, CompanionPersona.NBT_WHO),
                nonBlank(target, CompanionPersona.NBT_WHAT),
                nonBlank(target, CompanionPersona.NBT_HOW),
                nonBlank(target, CompanionPersona.NBT_SPEECH),
                nonBlank(target, CompanionPersona.NBT_RELATIONSHIP),
                nonBlank(target, CompanionPersona.NBT_QUIRKS))) {
            target.putBoolean(CompanionPersona.NBT_INITIALIZED, true);
        }
    }

    private static boolean nonBlank(CompoundTag tag, String key) {
        return tag.contains(key) && !tag.getStringOr(key, "").isBlank();
    }
}
