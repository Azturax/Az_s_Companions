package com.azscompanions.entity;

import com.azscompanions.config.FabricServerConfig;
import com.azscompanions.registry.FabricModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class FabricCompanionRecruitment {
    private FabricCompanionRecruitment() {
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
                if (entity instanceof FabricCompanionEntity companion && owner.equals(companion.getOwnerUuid())) {
                    owned++;
                }
            }
        }
        return owned;
    }

    public static FabricCompanionEntity findOwned(ServerPlayer player, UUID companionUuid) {
        MinecraftServer server = player.getServer();
        if (server == null || companionUuid == null) {
            return null;
        }
        UUID owner = player.getUUID();
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(companionUuid);
            if (entity instanceof FabricCompanionEntity companion && owner.equals(companion.getOwnerUuid())) {
                return companion;
            }
        }
        return null;
    }

    public static boolean recruit(ServerPlayer player, String definitionId) {
        return recruitEntity(player, definitionId) != null;
    }

    public static FabricCompanionEntity recruitEntity(ServerPlayer player, String definitionId) {
        ServerLevel level = player.serverLevel();
        if (countOwned(player) >= FabricServerConfig.MAX_COMPANIONS_PER_PLAYER) {
            Component msg = Component.translatable("message.azscompanions.limit_reached");
            player.displayClientMessage(msg, true);
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(definitionId);
        if (id == null) {
            id = FabricCompanionRegistry.KON_ID;
        }
        FabricCompanionDefinition definition = FabricCompanionRegistry.getOrKon(id);
        FabricCompanionEntity companion = FabricModEntities.COMPANION.create(level);
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

    public static FabricCompanionEntity spawnFromStored(ServerPlayer player, CompoundTag stored, UUID boundUuid) {
        ServerLevel level = player.serverLevel();
        FabricCompanionEntity companion = FabricModEntities.COMPANION.create(level);
        if (companion == null) {
            return null;
        }
        companion.load(stored);
        companion.setUUID(boundUuid);
        companion.setOwner(player);
        companion.moveTo(player.getX() + 1, player.getY(), player.getZ() + 1, player.getYRot(), 0);
        companion.setMode(FabricCompanionMode.FOLLOW);
        level.addFreshEntity(companion);
        return companion;
    }
}
