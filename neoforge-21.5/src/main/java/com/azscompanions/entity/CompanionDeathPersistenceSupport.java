package com.azscompanions.entity;

import com.azscompanions.config.ServerConfig;
import com.azscompanions.item.CharmData;
import com.azscompanions.item.CompanionCharmItem;
import com.azscompanions.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * On death, keep inventory/Bits by snapshotting into charm (parents) or parent StoredChildren (Bits)
 * instead of vanilla equipment drops.
 */
public final class CompanionDeathPersistenceSupport {
    private CompanionDeathPersistenceSupport() {
    }

    public static void persistOnDeath(CompanionEntity companion) {
        if (companion.level().isClientSide || !(companion.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!CompanionInventoryPersistence.shouldKeepInventoryOnDeath(ServerConfig.KEEP_INVENTORY_ON_DEATH.get())) {
            return;
        }

        if (companion.isChildCompanion()) {
            persistBitToParent(companion, serverLevel);
            CompanionPlayerDataSupport.save(companion);
            return;
        }

        persistParentToCharm(companion, serverLevel);
        CompanionPlayerDataSupport.save(companion);
    }

    private static void persistBitToParent(CompanionEntity bit, ServerLevel level) {
        CompanionEntity parent = findParent(bit, level);
        if (parent == null) {
            return;
        }
        if (!CompanionInventoryPersistence.shouldStoreBitOnParent(true, parent.isAlive(), true)) {
            return;
        }
        parent.storeDyingChildSnapshot(bit);
    }

    @Nullable
    private static CompanionEntity findParent(CompanionEntity bit, ServerLevel level) {
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
            if (entity instanceof CompanionEntity parent && parent.isAlive() && !parent.isChildCompanion()) {
                return parent;
            }
        }
        return null;
    }

    private static void persistParentToCharm(CompanionEntity companion, ServerLevel level) {
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
            CharmData.storeCompanion(charm, data, companion.getUUID());
        }
    }

    @Nullable
    private static ItemStack findBoundCharm(Player player, UUID companionUuid) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!(CompanionCharmItem.isCharm(stack) || stack.is(ModItems.COMPANION_CHARM.get()))) {
                continue;
            }
            UUID bound = CharmData.getBoundUuid(stack);
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
            if (CompanionCharmItem.isCharm(stack) || stack.is(ModItems.COMPANION_CHARM.get())) {
                return stack;
            }
        }
        return null;
    }
}
