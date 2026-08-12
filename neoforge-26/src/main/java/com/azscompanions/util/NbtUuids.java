package com.azscompanions.util;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/** CompoundTag UUID helpers for 1.21.5 (putUUID/getUUID/hasUUID removed). */
public final class NbtUuids {
    private NbtUuids() {
    }

    public static void put(CompoundTag tag, String key, UUID uuid) {
        tag.store(key, UUIDUtil.CODEC, uuid);
    }

    public static boolean has(CompoundTag tag, String key) {
        return tag.read(key, UUIDUtil.CODEC).isPresent();
    }

    public static UUID get(CompoundTag tag, String key) {
        return tag.read(key, UUIDUtil.CODEC).orElseThrow();
    }

    public static Optional<UUID> getOptional(CompoundTag tag, String key) {
        return tag.read(key, UUIDUtil.CODEC);
    }

    @Nullable
    public static UUID getOrNull(CompoundTag tag, String key) {
        return tag.read(key, UUIDUtil.CODEC).orElse(null);
    }
}
