package com.azscompanions.entity;

import com.azscompanions.util.OwnableUuids;

import com.azscompanions.config.ServerConfig;
import com.azscompanions.item.CharmData;
import com.azscompanions.item.CompanionCharmItem;
import com.azscompanions.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.UUID;

public final class CompanionRecruitment {
    private CompanionRecruitment() {
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
                if (entity instanceof CompanionEntity companion
                        && owner.equals(OwnableUuids.get(companion))
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
                if (entity instanceof CompanionEntity companion
                        && companion.isAlive()
                        && leaderUuid.equals(companion.getLeaderUuid())
                        && owner.equals(OwnableUuids.get(companion))) {
                    count++;
                }
            }
        }
        return count;
    }

    @Nullable
    public static CompanionEntity findOwned(ServerPlayer player, UUID companionUuid) {
        MinecraftServer server = player.getServer();
        if (server == null || companionUuid == null) {
            return null;
        }
        UUID owner = player.getUUID();
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(companionUuid);
            if (entity instanceof CompanionEntity companion && owner.equals(OwnableUuids.get(companion))) {
                return companion;
            }
        }
        return null;
    }

    @Nullable
    public static CompanionEntity findAnyPrimary(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return null;
        }
        UUID owner = player.getUUID();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof CompanionEntity companion
                        && companion.isAlive()
                        && owner.equals(OwnableUuids.get(companion))
                        && !companion.isFightSpawn()) {
                    return companion;
                }
            }
        }
        return null;
    }

    public static void cullExtraPrimaries(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        UUID owner = player.getUUID();
        java.util.List<CompanionEntity> primaries = new java.util.ArrayList<>();
        java.util.Map<UUID, CompanionEntity> byId = new java.util.HashMap<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof CompanionEntity companion)
                        || !companion.isAlive()
                        || !owner.equals(OwnableUuids.get(companion))) {
                    continue;
                }
                CompanionEntity seen = byId.putIfAbsent(companion.getUUID(), companion);
                if (seen != null && seen != companion) {
                    companion.discard();
                    continue;
                }
                if (!companion.isFightSpawn()) {
                    primaries.add(companion);
                }
            }
        }
        int max = ServerConfig.MAX_COMPANIONS_PER_PLAYER.get();
        if (primaries.size() <= max) {
            return;
        }
        UUID bound = null;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (CompanionCharmItem.isCharm(stack)) {
                bound = CharmData.getBoundUuid(stack);
                break;
            }
        }
        CompanionEntity keep = CompanionSpawnGuardSupport.pickPrimaryToKeep(
                primaries, bound, CompanionEntity::getUUID, c -> c.distanceToSqr(player));
        for (CompanionEntity extra : primaries) {
            if (extra != keep) {
                extra.discard();
            }
        }
    }

    @Nullable
    public static CompanionEntity resolveLeader(ServerPlayer player, CompanionEntity candidate) {
        if (candidate == null || !candidate.isAlive()) {
            return null;
        }
        if (!candidate.isChildCompanion()) {
            return candidate;
        }
        CompanionEntity parent = findOwned(player, candidate.getLeaderUuid());
        if (parent != null && parent.isAlive() && !parent.isChildCompanion()) {
            return parent;
        }
        return null;
    }

    @Nullable
    public static CompanionEntity recruit(ServerPlayer player, String definitionId) {
        ServerLevel level = player.serverLevel();
        if (!com.azscompanions.compat.ftb.FtbCompat.maySpawn(player)) {
            player.displayClientMessage(Component.literal("You lack permission to spawn companions (FTB Ranks)."), true);
            return null;
        }
        if (countOwned(player) >= ServerConfig.MAX_COMPANIONS_PER_PLAYER.get()) {
            player.displayClientMessage(Component.translatable("message.azscompanions.limit_reached"), true);
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(definitionId);
        if (id == null) {
            id = CompanionRegistry.KON_ID;
        }
        CompanionDefinition definition = CompanionRegistry.getOrKon(id);
        CompanionEntity companion = ModEntities.COMPANION.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (companion == null) {
            return null;
        }
        companion.snapTo(player.getX() + 1, player.getY(), player.getZ() + 1, player.getYRot(), 0);
        companion.setOwner(player);
        companion.applyDefinition(definition);
        companion.applyOwnerAppearanceDefaults(player);
        companion.setHomePos(player.blockPosition());
        companion.setMaxChildren(ServerConfig.MAX_CHILD_COMPANIONS_PER_LEADER.get());
        level.addFreshEntity(companion);
        return companion;
    }

    /**
     * CCI / streamer temporary companion. Does not count toward maxCompanionsPerPlayer,
     * is not charm-bound, and does not replace the owner's persistent Kon.
     */
    @Nullable
    public static CompanionEntity spawnCciSummon(ServerPlayer player, String definitionId) {
        if (!com.azscompanions.compat.ftb.FtbCompat.maySpawn(player)) {
            return null;
        }
        ServerLevel level = player.serverLevel();
        ResourceLocation id = ResourceLocation.tryParse(definitionId);
        if (id == null) {
            id = CompanionRegistry.KON_ID;
        }
        CompanionDefinition definition = CompanionRegistry.getOrKon(id);
        CompanionEntity companion = ModEntities.COMPANION.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
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
        companion.setMode(CompanionMode.FOLLOW);
        companion.setHomePos(player.blockPosition());
        companion.setNameTagVisible(true);
        if (!level.addFreshEntity(companion)) {
            return null;
        }
        return companion;
    }

    /**
     * Team-fight leader spawn (does not count toward maxCompanionsPerPlayer).
     * Shared by CCI {@code companion_spawn_leader}.
     */
    @Nullable
    public static CompanionEntity spawnFightLeader(ServerPlayer player) {
        if (!com.azscompanions.compat.ftb.FtbCompat.maySpawn(player)
                || !com.azscompanions.compat.ftb.FtbCompat.mayTeamfight(player)) {
            return null;
        }
        ServerLevel level = player.serverLevel();
        CompanionDefinition definition = CompanionRegistry.getOrKon(CompanionRegistry.KON_ID);
        CompanionEntity companion = ModEntities.COMPANION.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
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
        companion.setMode(CompanionMode.FOLLOW);
        companion.setHomePos(player.blockPosition());
        companion.setMaxChildren(ServerConfig.MAX_CHILD_COMPANIONS_PER_LEADER.get());
        level.addFreshEntity(companion);
        return companion;
    }

    /**
     * Child/Bit spawn under a leader. Shared by CCI {@code companion_spawn_child} and cake feed.
     */
    @Nullable
    public static CompanionEntity spawnChild(ServerPlayer player, CompanionEntity leader) {
        if (leader == null || !leader.isAlive()) {
            return null;
        }
        if (!com.azscompanions.compat.ftb.FtbCompat.maySpawn(player)) {
            return null;
        }
        CompanionEntity root = resolveLeader(player, leader);
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
        CompanionDefinition definition = CompanionRegistry.getOrKon(CompanionRegistry.KON_ID);
        CompanionEntity child = ModEntities.COMPANION.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
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
        child.setMode(CompanionMode.FOLLOW);
        // Inherit identity hints from parent; keep Bit scale/name defaults (CCI may override).
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

    @Nullable
    public static CompanionEntity spawnFromStored(ServerPlayer player, CompoundTag stored, UUID boundUuid) {
        CompanionEntity existing = findOwned(player, boundUuid);
        if (existing != null && existing.isAlive()) {
            return existing;
        }
        ServerLevel level = player.serverLevel();
        CompanionEntity companion = ModEntities.COMPANION.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (companion == null) {
            return null;
        }
        companion.load(stored);
        companion.setUUID(boundUuid);
        companion.setOwner(player);
        companion.snapTo(player.getX() + 1, player.getY(), player.getZ() + 1, player.getYRot(), 0);
        if (!CompanionPlayerPersistence.snapshotHasMode(stored.contains("Mode"))) {
            companion.setMode(CompanionMode.FOLLOW);
        }
        if (!level.addFreshEntity(companion)) {
            CompanionEntity again = findOwned(player, boundUuid);
            return again != null && again.isAlive() ? again : null;
        }
        companion.safeTeleportNear(player.blockPosition());
        CompanionPlayerDataSupport.apply(companion);
        return companion;
    }

    /** Restore a Bit snapshot near its parent leader (FIFO call from StoredChildren). */
    @Nullable
    public static CompanionEntity spawnStoredChild(
            ServerPlayer player, CompanionEntity parent, CompoundTag stored, UUID childUuid) {
        if (parent == null || !parent.isAlive() || stored == null) {
            return null;
        }
        if (!com.azscompanions.compat.ftb.FtbCompat.maySpawn(player)) {
            return null;
        }
        ServerLevel level = parent.level() instanceof ServerLevel leaderLevel
                ? leaderLevel
                : player.serverLevel();
        CompanionEntity child = ModEntities.COMPANION.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
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
            child.setMode(CompanionMode.FOLLOW);
        }
        level.addFreshEntity(child);
        CompanionPlayerDataSupport.apply(child);
        return child;
    }
}
