package com.azscompanions.entity;

import com.azscompanions.perk.MisterWigglySidekick;
import com.azscompanions.world.CompanionChunkTickets;
import com.azscompanions.world.CompanionIdentityStore;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Teleports owned companions with the owner across <em>any</em> dimension change
 * (vanilla Nether/End and modded dims). Uses entity dimension teleport APIs — no
 * logout-style discard/respawn — so persona/model stay continuous for the world save.
 */
public final class CompanionDimensionTravelSupport {

    private CompanionDimensionTravelSupport() {
    }

    public static void followOwnerAcrossDimensions(
            ServerPlayer player,
            ResourceKey<Level> from,
            ResourceKey<Level> to
    ) {
        if (player == null || player.getServer() == null) {
            return;
        }
        if (!CompanionDimensionTravel.isDimensionChange(from, to)) {
            return;
        }
        ServerLevel dest = (ServerLevel) player.level();
        if (dest == null || !dest.dimension().equals(to)) {
            dest = player.getServer().getLevel(to);
        }
        if (dest == null) {
            return;
        }

        MinecraftServer server = player.getServer();
        UUID owner = player.getUUID();
        CompanionIdentityStore identityStore = CompanionIdentityStore.get(server);

        List<CompanionEntity> owned = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof CompanionEntity companion
                        && companion.isAlive()
                        && owner.equals(companion.getOwnerUuid())) {
                    owned.add(companion);
                }
            }
        }
        // Parents first, then Bits — keeps leadership in the destination before children arrive.
        owned.sort(Comparator.comparing((CompanionEntity c) -> c.isChildCompanion())
                .thenComparing(c -> c.getUUID().toString()));

        int index = 0;
        for (CompanionEntity companion : owned) {
            teleportWithOwner(player, dest, companion, identityStore, index++);
        }
    }

    private static void teleportWithOwner(
            ServerPlayer player,
            ServerLevel dest,
            CompanionEntity companion,
            CompanionIdentityStore identityStore,
            int ringIndex
    ) {
        if (companion == null || !companion.isAlive()) {
            return;
        }

        rememberIdentity(companion, identityStore);

        companion.stopRiding();
        companion.ejectPassengers();
        CompanionChunkTickets.release(companion);

        BlockPos near = offsetNearOwner(player, ringIndex);
        double x = near.getX() + 0.5d;
        double y = near.getY();
        double z = near.getZ() + 0.5d;
        float yRot = player.getYRot();
        float xRot = player.getXRot();

        if (companion.level() == dest) {
            companion.teleportTo(x, y, z);
            companion.setYRot(yRot);
            companion.setXRot(xRot);
            MisterWigglySidekick.ensureFor(companion);
            return;
        }

        Entity moved = companion.changeDimension(dest);
        if (moved instanceof CompanionEntity arrived) {
            arrived.teleportTo(x, y, z);
            arrived.setYRot(yRot);
            arrived.setXRot(xRot);
            MisterWigglySidekick.ensureFor(arrived);
            rememberIdentity(arrived, identityStore);
        } else {
            companion.teleportTo(x, y, z);
            companion.setYRot(yRot);
            companion.setXRot(xRot);
            MisterWigglySidekick.ensureFor(companion);
            rememberIdentity(companion, identityStore);
        }
    }

    private static BlockPos offsetNearOwner(ServerPlayer player, int ringIndex) {
        int ring = 2 + (ringIndex % 4);
        double angle = (Math.PI * 2.0d * (ringIndex % 8)) / 8.0d;
        int ox = (int) Math.round(Math.cos(angle) * ring);
        int oz = (int) Math.round(Math.sin(angle) * ring);
        BlockPos base = player.blockPosition().offset(ox, 0, oz);
        ServerLevel level = (ServerLevel) player.level();
        if (level.getBlockState(base).isAir() && level.getBlockState(base.below()).isSolid()) {
            return base;
        }
        return player.blockPosition().offset(ox, 0, oz);
    }

    public static void rememberIdentity(CompanionEntity companion, CompanionIdentityStore store) {
        if (companion == null || store == null) {
            return;
        }
        CompoundTag data = new CompoundTag();
        companion.saveWithoutId(data);
        store.putIdentity(companion.getUUID(), data);
    }

    public static void rememberIdentity(ServerPlayer player, CompanionEntity companion) {
        if (player == null || player.getServer() == null || companion == null) {
            return;
        }
        rememberIdentity(companion, CompanionIdentityStore.get(player.getServer()));
    }
}
