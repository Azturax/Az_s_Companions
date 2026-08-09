package com.koncompanions.entity;

import com.koncompanions.config.ServerConfig;
import com.koncompanions.registry.ModEntities;
import com.koncompanions.voice.DialogueCategory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.UUID;

public final class CompanionRecruitment {
    private CompanionRecruitment() {
    }

    /** Recruit a new Kon near the player. Does not open UI. Returns the entity or null. */
    @Nullable
    public static CompanionEntity recruit(ServerPlayer player, String definitionId) {
        ServerLevel level = player.serverLevel();
        long owned = level.getEntitiesOfClass(CompanionEntity.class,
                        new AABB(player.blockPosition()).inflate(512),
                        c -> player.getUUID().equals(c.getOwnerUuid()))
                .size();
        if (owned >= ServerConfig.MAX_COMPANIONS_PER_PLAYER.get()) {
            Component msg = Component.translatable("message.koncompanions.limit_reached");
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
        companion.setHomePos(player.blockPosition());
        level.addFreshEntity(companion);
        companion.speak(DialogueCategory.GREETING);
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
        companion.speak(DialogueCategory.GREETING);
        return companion;
    }
}
