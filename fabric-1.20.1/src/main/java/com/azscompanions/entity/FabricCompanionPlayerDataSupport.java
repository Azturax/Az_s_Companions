package com.azscompanions.entity;

import com.azscompanions.perk.WolfyPerkSupport;
import com.azscompanions.world.FabricCompanionPlayerStore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import org.jetbrains.annotations.Nullable;
import java.util.UUID;

/** Server-side save/load of per-player companion settings and inventory (Fabric). */
public final class FabricCompanionPlayerDataSupport {
    private FabricCompanionPlayerDataSupport() {
    }

    public static void save(FabricCompanionEntity companion) {
        if (companion == null || companion.level().isClientSide) {
            return;
        }
        if (!CompanionPlayerPersistence.shouldPersist(
                false, companion.isFightSpawn(), companion.isChildCompanion(), companion.isCciSummoned())) {
            return;
        }
        UUID owner = companion.getOwnerUuid();
        MinecraftServer server = companion.level().getServer();
        if (owner == null || server == null) {
            return;
        }
        CompoundTag full = new CompoundTag();
        companion.saveWithoutId(full);
        CompoundTag payload = extractPayload(full);
        if (payload.isEmpty()) {
            return;
        }
        FabricCompanionPlayerStore.get(server).put(owner, keyOf(companion), payload);
    }

    public static void apply(FabricCompanionEntity companion) {
        if (companion == null || companion.level().isClientSide) {
            return;
        }
        if (!CompanionPlayerPersistence.shouldPersist(
                false, companion.isFightSpawn(), companion.isChildCompanion(), companion.isCciSummoned())) {
            return;
        }
        UUID owner = companion.getOwnerUuid();
        MinecraftServer server = companion.level().getServer();
        if (owner == null || server == null) {
            return;
        }
        CompoundTag payload = FabricCompanionPlayerStore.get(server).peek(owner, keyOf(companion));
        if (payload == null || payload.isEmpty()) {
            save(companion);
            return;
        }
        CompoundTag current = new CompoundTag();
        companion.saveWithoutId(current);
        mergePayload(current, payload);
        companion.getCompanionInventory().beginPersistentLoad();
        companion.beginApplyingPlayerPersistentData();
        try {
            companion.readAdditionalSaveData(current);
        } finally {
            companion.endApplyingPlayerPersistentData();
            companion.getCompanionInventory().endPersistentLoad();
        }
    }

    public static void saveAll(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof FabricCompanionEntity companion) {
                    save(companion);
                }
            }
        }
    }

    static String keyOf(FabricCompanionEntity companion) {
        return CompanionPlayerPersistence.companionKey(
                companion.isChildCompanion(),
                companion.getUUID(),
                companion.getDefinition().id().toString(),
                companion.getChatDisplayName(),
                WolfyPerkSupport.isWolfyName(companion.getChatDisplayName()));
    }

    static CompoundTag extractPayload(CompoundTag full) {
        CompoundTag out = new CompoundTag();
        if (full == null) {
            return out;
        }
        for (String key : CompanionPlayerPersistence.PAYLOAD_NBT_KEYS) {
            if (full.contains(key)) {
                Tag value = full.get(key);
                if (value != null) {
                    out.put(key, value.copy());
                }
            }
        }
        return out;
    }

    static void mergePayload(CompoundTag target, CompoundTag payload) {
        if (target == null || payload == null) {
            return;
        }
        for (String key : CompanionPlayerPersistence.PAYLOAD_NBT_KEYS) {
            if (!payload.contains(key)) {
                continue;
            }
            Tag incoming = payload.get(key);
            if (incoming == null) {
                continue;
            }
            if ("Inventory".equals(key) && !CompanionPlayerPersistence.shouldApplyInventory(
                    isEmptyInventory(incoming), isEmptyInventory(target.get("Inventory")))) {
                continue;
            }
            target.put(key, incoming.copy());
        }
    }

    static boolean isEmptyInventory(@Nullable Tag tag) {
        if (tag == null) {
            return true;
        }
        if (tag instanceof ListTag list) {
            return list.isEmpty();
        }
        if (tag instanceof CompoundTag compound) {
            if (compound.contains("Items", Tag.TAG_LIST)) {
                return compound.getList("Items", Tag.TAG_COMPOUND).isEmpty();
            }
            return compound.isEmpty();
        }
        return true;
    }
}
