package com.azscompanions.entity;

import com.azscompanions.config.ServerConfig;
import com.azscompanions.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.UUID;

public final class CompanionRecruitment {
    private CompanionRecruitment() {
    }

    /** Count loaded companions owned by this player across all dimensions. */
    public static long countOwned(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return 0;
        }
        UUID owner = player.getUUID();
        long owned = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof CompanionEntity companion && owner.equals(companion.getOwnerUuid())) {
                    owned++;
                }
            }
        }
        return owned;
    }

    /** Find a loaded companion by UUID owned by this player (any dimension). */
    @Nullable
    public static CompanionEntity findOwned(ServerPlayer player, UUID companionUuid) {
        MinecraftServer server = player.getServer();
        if (server == null || companionUuid == null) {
            return null;
        }
        UUID owner = player.getUUID();
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(companionUuid);
            if (entity instanceof CompanionEntity companion && owner.equals(companion.getOwnerUuid())) {
                return companion;
            }
        }
        return null;
    }

    /** Recruit a new Kon near the player. Does not open UI. Returns the entity or null. */
    @Nullable
    public static CompanionEntity recruit(ServerPlayer player, String definitionId) {
        ServerLevel level = player.serverLevel();
        if (countOwned(player) >= ServerConfig.MAX_COMPANIONS_PER_PLAYER.get()) {
            Component msg = Component.translatable("message.azscompanions.limit_reached");
            player.displayClientMessage(msg, true);
            return null;
        }

        ResourceLocation id = ResourceLocation.tryParse(definitionId);
        if (id == null) {
            id = CompanionRegistry.KON_ID;
        }
        CompanionDefinition definition = CompanionRegistry.getOrKon(id);
        CompanionEntity companion = ModEntities.COMPANION.get().create(level);
        if (companion == null) {
            return null;
        }
        companion.moveTo(player.getX() + 1, player.getY(), player.getZ() + 1, player.getYRot(), 0);
        companion.setOwner(player);
        companion.applyDefinition(definition);
        companion.applyOwnerAppearanceDefaults(player);
        companion.setHomePos(player.blockPosition());
        level.addFreshEntity(companion);
        return companion;
    }

    /** Resummon a companion previously stored on a charm. */
    @Nullable
    public static CompanionEntity spawnFromStored(ServerPlayer player, CompoundTag stored, UUID boundUuid) {
        ServerLevel level = player.serverLevel();
        CompanionEntity companion = ModEntities.COMPANION.get().create(level);
        if (companion == null) {
            return null;
        }
        companion.load(stored);
        companion.setUUID(boundUuid);
        companion.setOwner(player);
        companion.moveTo(player.getX() + 1, player.getY(), player.getZ() + 1, player.getYRot(), 0);
        companion.setMode(CompanionMode.FOLLOW);
        level.addFreshEntity(companion);
        return companion;
    }
}
