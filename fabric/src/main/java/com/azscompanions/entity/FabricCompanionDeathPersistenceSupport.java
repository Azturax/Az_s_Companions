package com.azscompanions.entity;

import com.azscompanions.config.FabricServerConfig;
import com.azscompanions.item.FabricCharmData;
import com.azscompanions.item.FabricCompanionCharmItem;
import com.azscompanions.registry.FabricModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import java.util.UUID;

/** Fabric: keep inventory/Bits on death via charm / parent StoredChildren. */
public final class FabricCompanionDeathPersistenceSupport {
    private FabricCompanionDeathPersistenceSupport() {
    }

    public static void persistOnDeath(FabricCompanionEntity companion) {
        if (companion.level().isClientSide || !(companion.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!CompanionInventoryPersistence.shouldKeepInventoryOnDeath(FabricServerConfig.KEEP_INVENTORY_ON_DEATH)) {
            return;
        }
        if (companion.isChildCompanion()) {
            persistBitToParent(companion, serverLevel);
            return;
        }
        persistParentToCharm(companion, serverLevel);
    }

    private static void persistBitToParent(FabricCompanionEntity bit, ServerLevel level) {
        FabricCompanionEntity parent = findParent(bit, level);
        if (parent == null) {
            return;
        }
        if (!CompanionInventoryPersistence.shouldStoreBitOnParent(true, parent.isAlive(), true)) {
            return;
        }
        parent.storeDyingChildSnapshot(bit);
    }

    @Nullable
    private static FabricCompanionEntity findParent(FabricCompanionEntity bit, ServerLevel level) {
        UUID leader = bit.getLeaderUuid();
        if (leader == null) {
            return null;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return null;
        }
        for (ServerLevel other : server.getAllLevels()) {
            Entity entity = other.getEntity(leader);
            if (entity instanceof FabricCompanionEntity parent && parent.isAlive() && !parent.isChildCompanion()) {
                return parent;
            }
        }
        return null;
    }

    private static void persistParentToCharm(FabricCompanionEntity companion, ServerLevel level) {
        UUID ownerId = companion.getOwnerUuid();
        MinecraftServer server = level.getServer();
        if (ownerId == null || server == null) {
            return;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner == null) {
            return;
        }
        companion.storeAllLivingChildren();
        CompoundTag data = new CompoundTag();
        companion.saveWithoutId(data);
        ItemStack charm = findBoundCharm(owner, companion.getUUID());
        if (charm == null) {
            charm = findAnyCharm(owner);
        }
        if (charm != null) {
            FabricCharmData.storeCompanion(charm, data, companion.getUUID());
        }
        FabricCompanionLogoutSupport.mergeDeathSnapshot(owner, companion.getUUID(), data);
        FabricCompanionPlayerDataSupport.save(companion);
    }

    @Nullable
    private static ItemStack findBoundCharm(Player player, UUID companionUuid) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!(FabricCompanionCharmItem.isCharm(stack) || stack.is(FabricModItems.COMPANION_CHARM))) {
                continue;
            }
            UUID bound = FabricCharmData.getBoundUuid(stack);
            if (companionUuid.equals(bound)) {
                return stack;
            }
        }
        return null;
    }

    @Nullable
    private static ItemStack findAnyCharm(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (FabricCompanionCharmItem.isCharm(stack) || stack.is(FabricModItems.COMPANION_CHARM)) {
                return stack;
            }
        }
        return null;
    }
}
