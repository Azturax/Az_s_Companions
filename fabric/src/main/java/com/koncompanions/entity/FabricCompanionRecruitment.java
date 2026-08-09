package com.koncompanions.entity;

import com.koncompanions.config.FabricServerConfig;
import com.koncompanions.registry.FabricModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

public final class FabricCompanionRecruitment {
    private FabricCompanionRecruitment() {
    }

    public static boolean recruit(ServerPlayer player, String definitionId) {
        return recruitEntity(player, definitionId) != null;
    }

    public static FabricCompanionEntity recruitEntity(ServerPlayer player, String definitionId) {
        ServerLevel level = player.serverLevel();
        long owned = level.getEntitiesOfClass(FabricCompanionEntity.class,
                        new AABB(player.blockPosition()).inflate(512),
                        c -> player.getUUID().equals(c.getOwnerUuid()))
                .size();
        if (owned >= FabricServerConfig.MAX_COMPANIONS_PER_PLAYER) {
            Component msg = Component.translatable("message.koncompanions.limit_reached");
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
        companion.setHomePos(player.blockPosition());
        level.addFreshEntity(companion);
        companion.speakGreeting();
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
        companion.speakGreeting();
        return companion;
    }
}
