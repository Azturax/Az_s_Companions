package com.azscompanions.entity;

import com.azscompanions.config.ServerConfig;
import com.azscompanions.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.UUID;

public final class CompanionRecruitment {
    private CompanionRecruitment() {
    }

    /** Primary companions only (excludes fight spawns / children). */
    public static long countOwned(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return 0;
        }
        UUID owner = player.getUUID();
        long owned = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof CompanionEntity companion
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
        MinecraftServer server = player.level().getServer();
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
                        && owner.equals(companion.getOwnerUuid())) {
                    count++;
                }
            }
        }
        return count;
    }

    @Nullable
    public static CompanionEntity findOwned(ServerPlayer player, UUID companionUuid) {
        MinecraftServer server = player.level().getServer();
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
        ServerLevel level = (ServerLevel) player.level();
        if (!com.azscompanions.compat.ftb.FtbCompat.maySpawn(player)) {
            player.sendOverlayMessage(Component.literal("You lack permission to spawn companions (FTB Ranks)."));
            return null;
        }
        if (countOwned(player) >= ServerConfig.MAX_COMPANIONS_PER_PLAYER.get()) {
            player.sendOverlayMessage(Component.translatable("message.azscompanions.limit_reached"));
            return null;
        }
        Identifier id = Identifier.tryParse(definitionId);
        if (id == null) {
            id = CompanionRegistry.KON_ID;
        }
        CompanionDefinition definition = CompanionRegistry.getOrKon(id);
        CompanionEntity companion = ModEntities.COMPANION.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (companion == null) {
            return null;
        }
        companion.snapTo(player.getX() + 1, player.getY(), player.getZ() + 1, player.getYRot(), 0);
        companion.setOwner(player);
        companion.applyDefinition(definition);
        companion.applyOwnerAppearanceDefaults(player);
        companion.setHomePos(player.blockPosition());
        level.addFreshEntity(companion);
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
        ServerLevel level = (ServerLevel) player.level();
        CompanionDefinition definition = CompanionRegistry.getOrKon(CompanionRegistry.KON_ID);
        CompanionEntity companion = ModEntities.COMPANION.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (companion == null) {
            return null;
        }
        double angle = level.getRandom().nextDouble() * Math.PI * 2.0d;
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
        int maxChildren = ServerConfig.MAX_CHILD_COMPANIONS_PER_LEADER.get();
        if (countChildrenOf(player, root.getUUID()) >= maxChildren) {
            return null;
        }
        ServerLevel level = (ServerLevel) player.level();
        if (root.level() instanceof ServerLevel leaderLevel) {
            level = leaderLevel;
        }
        CompanionDefinition definition = CompanionRegistry.getOrKon(CompanionRegistry.KON_ID);
        CompanionEntity child = ModEntities.COMPANION.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (child == null) {
            return null;
        }
        double angle = level.getRandom().nextDouble() * Math.PI * 2.0d;
        double dist = 1.2d + level.getRandom().nextDouble() * 1.5d;
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
        ServerLevel level = (ServerLevel) player.level();
        CompanionEntity companion = ModEntities.COMPANION.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (companion == null) {
            return null;
        }
        try (ProblemReporter.ScopedCollector problems = new ProblemReporter.ScopedCollector(companion.problemPath(), com.azscompanions.AzsCompanions.LOGGER)) {
            ValueInput input = TagValueInput.create(problems, level.registryAccess(), stored);
            companion.load(input);
        }
        companion.setUUID(boundUuid);
        companion.setOwner(player);
        companion.snapTo(player.getX() + 1, player.getY(), player.getZ() + 1, player.getYRot(), 0);
        companion.setMode(CompanionMode.FOLLOW);
        level.addFreshEntity(companion);
        return companion;
    }
}
