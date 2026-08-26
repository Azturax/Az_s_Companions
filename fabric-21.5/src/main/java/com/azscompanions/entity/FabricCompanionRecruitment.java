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

    /** Primary companions only (excludes fight spawns / children). */
    public static long countOwned(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return 0;
        }
        UUID owner = player.getUUID();
        long owned = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof FabricCompanionEntity companion
                        && owner.equals(companion.getOwnerUuid())
                        && !companion.isFightSpawn()) {
                    owned++;
                }
            }
        }
        return owned;
    }

    public static int countChildrenOf(ServerPlayer player, UUID leaderUuid) {
        if (leaderUuid == null) {
            return 0;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return 0;
        }
        UUID owner = player.getUUID();
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof FabricCompanionEntity companion
                        && companion.isAlive()
                        && leaderUuid.equals(companion.getLeaderUuid())
                        && owner.equals(companion.getOwnerUuid())) {
                    count++;
                }
            }
        }
        return count;
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

    public static FabricCompanionEntity resolveLeader(ServerPlayer player, FabricCompanionEntity candidate) {
        if (candidate == null || !candidate.isAlive()) {
            return null;
        }
        if (!candidate.isChildCompanion()) {
            return candidate;
        }
        FabricCompanionEntity parent = findOwned(player, candidate.getLeaderUuid());
        if (parent != null && parent.isAlive() && !parent.isChildCompanion()) {
            return parent;
        }
        return null;
    }

    public static boolean recruit(ServerPlayer player, String definitionId) {
        return recruitEntity(player, definitionId) != null;
    }

    public static FabricCompanionEntity recruitEntity(ServerPlayer player, String definitionId) {
        ServerLevel level = player.serverLevel();
        if (!com.azscompanions.compat.ftb.FtbCompat.maySpawn(player)) {
            player.displayClientMessage(Component.literal("You lack permission to spawn companions (FTB Ranks)."), true);
            return null;
        }
        if (countOwned(player) >= FabricServerConfig.MAX_COMPANIONS_PER_PLAYER) {
            player.displayClientMessage(Component.translatable("message.azscompanions.limit_reached"), true);
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(definitionId);
        if (id == null) {
            id = FabricCompanionRegistry.KON_ID;
        }
        FabricCompanionDefinition definition = FabricCompanionRegistry.getOrKon(id);
        FabricCompanionEntity companion = FabricModEntities.COMPANION.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (companion == null) {
            return null;
        }
        companion.snapTo(player.getX() + 1, player.getY(), player.getZ() + 1, player.getYRot(), 0);
        companion.setOwner(player);
        companion.applyDefinition(definition);
        companion.applyOwnerAppearanceDefaults(player);
        companion.setHomePos(player.blockPosition());
        companion.setMaxChildren(FabricServerConfig.MAX_CHILD_COMPANIONS_PER_LEADER);
        level.addFreshEntity(companion);
        return companion;
    }

    /**
     * CCI / streamer temporary companion. Does not count toward maxCompanionsPerPlayer,
     * is not charm-bound, and does not replace the owner's persistent Kon.
     */
    public static FabricCompanionEntity spawnCciSummon(ServerPlayer player, String definitionId) {
        if (!com.azscompanions.compat.ftb.FtbCompat.maySpawn(player)) {
            return null;
        }
        ServerLevel level = player.serverLevel();
        ResourceLocation id = ResourceLocation.tryParse(definitionId);
        if (id == null) {
            id = FabricCompanionRegistry.KON_ID;
        }
        FabricCompanionDefinition definition = FabricCompanionRegistry.getOrKon(id);
        FabricCompanionEntity companion = FabricModEntities.COMPANION.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (companion == null) {
            return null;
        }
        double angle = level.random.nextDouble() * Math.PI * 2.0d;
        companion.snapTo(
                player.getX() + Math.cos(angle) * 2.0d,
                player.getY(),
                player.getZ() + Math.sin(angle) * 2.0d,
                player.getYRot(), 0);
        companion.setOwner(player);
        companion.applyDefinition(definition);
        companion.setFightSpawn(true);
        companion.setMode(FabricCompanionMode.FOLLOW);
        companion.setHomePos(player.blockPosition());
        companion.setNameTagVisible(true);
        if (!level.addFreshEntity(companion)) {
            return null;
        }
        return companion;
    }

    /** Team-fight leader spawn (CCI {@code companion_spawn_leader}). */
    public static FabricCompanionEntity spawnFightLeader(ServerPlayer player) {
        if (!com.azscompanions.compat.ftb.FtbCompat.maySpawn(player)
                || !com.azscompanions.compat.ftb.FtbCompat.mayTeamfight(player)) {
            return null;
        }
        ServerLevel level = player.serverLevel();
        FabricCompanionDefinition definition = FabricCompanionRegistry.getOrKon(FabricCompanionRegistry.KON_ID);
        FabricCompanionEntity companion = FabricModEntities.COMPANION.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (companion == null) {
            return null;
        }
        double angle = level.random.nextDouble() * Math.PI * 2.0d;
        companion.snapTo(
                player.getX() + Math.cos(angle) * 2.0d,
                player.getY(),
                player.getZ() + Math.sin(angle) * 2.0d,
                player.getYRot(), 0);
        companion.setOwner(player);
        companion.applyDefinition(definition);
        companion.setFightSpawn(true);
        companion.setAttitude(CompanionAttitude.HOSTILE);
        companion.setMode(FabricCompanionMode.FOLLOW);
        companion.setHomePos(player.blockPosition());
        companion.setMaxChildren(FabricServerConfig.MAX_CHILD_COMPANIONS_PER_LEADER);
        level.addFreshEntity(companion);
        return companion;
    }

    /** Child/Bit spawn — CCI {@code companion_spawn_child} and cake feed. */
    public static FabricCompanionEntity spawnChild(ServerPlayer player, FabricCompanionEntity leader) {
        if (leader == null || !leader.isAlive()) {
            return null;
        }
        if (!com.azscompanions.compat.ftb.FtbCompat.maySpawn(player)) {
            return null;
        }
        FabricCompanionEntity root = resolveLeader(player, leader);
        if (root == null) {
            return null;
        }
        int maxChildren = root.getMaxChildren();
        if (countChildrenOf(player, root.getUUID()) + root.getStoredChildCount() >= maxChildren) {
            return null;
        }
        ServerLevel level = player.serverLevel();
        if (root.level() instanceof ServerLevel leaderLevel) {
            level = leaderLevel;
        }
        FabricCompanionDefinition definition = FabricCompanionRegistry.getOrKon(FabricCompanionRegistry.KON_ID);
        FabricCompanionEntity child = FabricModEntities.COMPANION.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (child == null) {
            return null;
        }
        double angle = level.random.nextDouble() * Math.PI * 2.0d;
        double dist = 1.2d + level.random.nextDouble() * 1.5d;
        child.snapTo(root.getX() + Math.cos(angle) * dist, root.getY(),
                root.getZ() + Math.sin(angle) * dist, root.getYRot(), 0);
        child.setOwner(player);
        child.applyDefinition(definition);
        child.setFightSpawn(true);
        child.setLeaderUuid(root.getUUID());
        child.setHomePos(root.blockPosition());
        child.setMode(FabricCompanionMode.FOLLOW);
        child.setAttitude(root.getAttitude());
        child.setTeamId(root.getTeamId() == null ? "" : root.getTeamId());
        child.setForm(root.getForm());
        child.setFormVariant(root.getFormVariant());
        child.setSkinPath(root.getSkinPath() == null ? "" : root.getSkinPath());
        child.setArmorVisible(root.isArmorVisible());
        child.setBodyScale(CompanionChildLimits.DEFAULT_BODY_SCALE);
        child.setCustomDisplayName(CompanionChildLimits.DEFAULT_NAME);
        child.inheritSpacingFrom(root);
        level.addFreshEntity(child);
        return child;
    }

    public static FabricCompanionEntity spawnFromStored(ServerPlayer player, CompoundTag stored, UUID boundUuid) {
        ServerLevel level = player.serverLevel();
        FabricCompanionEntity companion = FabricModEntities.COMPANION.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (companion == null) {
            return null;
        }
        companion.load(stored);
        companion.setUUID(boundUuid);
        companion.setOwner(player);
        companion.snapTo(player.getX() + 1, player.getY(), player.getZ() + 1, player.getYRot(), 0);
        if (!CompanionPlayerPersistence.snapshotHasMode(stored.contains("Mode"))) {
            companion.setMode(FabricCompanionMode.FOLLOW);
        }
        level.addFreshEntity(companion);
        FabricCompanionPlayerDataSupport.apply(companion);
        return companion;
    }

    public static FabricCompanionEntity spawnStoredChild(
            ServerPlayer player, FabricCompanionEntity parent, CompoundTag stored, UUID childUuid) {
        if (parent == null || !parent.isAlive() || stored == null) {
            return null;
        }
        if (!com.azscompanions.compat.ftb.FtbCompat.maySpawn(player)) {
            return null;
        }
        ServerLevel level = parent.level() instanceof ServerLevel leaderLevel
                ? leaderLevel
                : player.serverLevel();
        FabricCompanionEntity child = FabricModEntities.COMPANION.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (child == null) {
            return null;
        }
        child.load(stored);
        child.setUUID(childUuid);
        child.setOwner(player);
        child.setLeaderUuid(parent.getUUID());
        child.setFightSpawn(true);
        double angle = level.random.nextDouble() * Math.PI * 2.0d;
        double dist = 1.2d + level.random.nextDouble() * 1.5d;
        child.snapTo(parent.getX() + Math.cos(angle) * dist, parent.getY(),
                parent.getZ() + Math.sin(angle) * dist, parent.getYRot(), 0);
        child.setHomePos(parent.blockPosition());
        if (!CompanionPlayerPersistence.snapshotHasMode(stored.contains("Mode"))) {
            child.setMode(FabricCompanionMode.FOLLOW);
        }
        level.addFreshEntity(child);
        FabricCompanionPlayerDataSupport.apply(child);
        return child;
    }
}
