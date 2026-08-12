package com.azscompanions.entity;

import com.azscompanions.item.FabricCharmData;
import com.azscompanions.item.FabricCompanionCharmItem;
import com.azscompanions.registry.FabricModItems;
import com.azscompanions.util.NbtUuids;
import com.azscompanions.world.FabricCompanionIdentityStore;
import com.azscompanions.world.FabricCompanionOfflineStore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Fabric: despawn owned companions on logout (persisted) and restore near the player on login.
 */
public final class FabricCompanionLogoutSupport {
    private FabricCompanionLogoutSupport() {
    }

    public static void parkOwnedCompanions(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        UUID owner = player.getUUID();
        List<FabricCompanionEntity> roots = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof FabricCompanionEntity companion
                        && companion.isAlive()
                        && owner.equals(companion.getOwnerUuid())
                        && !companion.isChildCompanion()) {
                    roots.add(companion);
                }
            }
        }

        ListTag parked = new ListTag();
        ItemStack charm = findCharm(player);
        UUID charmBound = charm != null ? FabricCharmData.getBoundUuid(charm) : null;

        for (FabricCompanionEntity companion : roots) {
            companion.storeAllLivingChildren();
            CompoundTag data = new CompoundTag();
            companion.saveWithoutId(data);
            UUID id = companion.getUUID();
            parked.add(entry(id, data));
            FabricCompanionIdentityStore.get(server).putIdentity(id, data);
            if (charm != null && id.equals(charmBound)) {
                FabricCharmData.storeCompanionForLogout(charm, data.copy(), id);
            }
            companion.discard();
        }

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof FabricCompanionEntity companion)
                        || !companion.isAlive()
                        || !owner.equals(companion.getOwnerUuid())) {
                    continue;
                }
                CompoundTag data = new CompoundTag();
                companion.saveWithoutId(data);
                parked.add(entry(companion.getUUID(), data));
                companion.discard();
            }
        }

        FabricCompanionOfflineStore store = FabricCompanionOfflineStore.get(server);
        ListTag prior = store.take(owner);
        Set<UUID> parkedIds = new HashSet<>();
        for (int i = 0; i < parked.size(); i++) {
            CompoundTag e = parked.getCompoundOrEmpty(i);
            if (NbtUuids.has(e, CompanionLogoutPersistence.ENTRY_UUID)) {
                parkedIds.add(NbtUuids.get(e, CompanionLogoutPersistence.ENTRY_UUID));
            }
        }
        for (int i = 0; i < prior.size(); i++) {
            CompoundTag e = prior.getCompoundOrEmpty(i);
            if (!NbtUuids.has(e, CompanionLogoutPersistence.ENTRY_UUID)) {
                continue;
            }
            UUID id = NbtUuids.get(e, CompanionLogoutPersistence.ENTRY_UUID);
            if (!parkedIds.contains(id)) {
                parked.add(e.copy());
                parkedIds.add(id);
            }
        }
        if (!parked.isEmpty()) {
            store.put(owner, parked);
        }
    }

    public static void restoreParkedCompanions(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        FabricCompanionOfflineStore store = FabricCompanionOfflineStore.get(server);
        ListTag parked = store.take(player.getUUID());

        Set<UUID> restored = new HashSet<>();
        ListTag remaining = new ListTag();
        for (int i = 0; i < parked.size(); i++) {
            CompoundTag entry = parked.getCompoundOrEmpty(i);
            UUID id = NbtUuids.has(entry, CompanionLogoutPersistence.ENTRY_UUID)
                    ? NbtUuids.get(entry, CompanionLogoutPersistence.ENTRY_UUID)
                    : null;
            CompoundTag data = entry.contains(CompanionLogoutPersistence.ENTRY_DATA)
                    ? entry.getCompoundOrEmpty(CompanionLogoutPersistence.ENTRY_DATA)
                    : null;
            if (id == null || data == null) {
                continue;
            }
            if (FabricCompanionRecruitment.findOwned(player, id) != null) {
                restored.add(id);
                continue;
            }
            FabricCompanionEntity spawned = FabricCompanionRecruitment.spawnFromStored(player, data.copy(), id);
            if (spawned != null) {
                restored.add(id);
            } else {
                remaining.add(entry.copy());
            }
        }

        ItemStack charm = findCharm(player);
        if (charm != null) {
            UUID bound = FabricCharmData.getBoundUuid(charm);
            if (bound != null && restored.contains(bound) && FabricCharmData.hasStoredCompanion(charm)) {
                FabricCharmData.clearStoredCompanion(charm);
            } else if (FabricCharmData.isLogoutParked(charm) && FabricCharmData.hasStoredCompanion(charm) && bound != null) {
                if (FabricCompanionRecruitment.findOwned(player, bound) == null) {
                    CompoundTag stored = FabricCharmData.peekStoredCompanion(charm);
                    if (stored != null) {
                        FabricCompanionEntity spawned =
                                FabricCompanionRecruitment.spawnFromStored(player, stored.copy(), bound);
                        if (spawned != null) {
                            FabricCharmData.clearStoredCompanion(charm);
                            restored.add(bound);
                        }
                    }
                } else {
                    FabricCharmData.clearStoredCompanion(charm);
                }
            }
        }

        if (!remaining.isEmpty()) {
            store.put(player.getUUID(), remaining);
        }
    }

    private static CompoundTag entry(UUID id, CompoundTag data) {
        CompoundTag entry = new CompoundTag();
        NbtUuids.put(entry, CompanionLogoutPersistence.ENTRY_UUID, id);
        entry.put(CompanionLogoutPersistence.ENTRY_DATA, data);
        return entry;
    }

    private static ItemStack findCharm(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (FabricCompanionCharmItem.isCharm(stack) || stack.getItem() == FabricModItems.COMPANION_CHARM) {
                return stack;
            }
        }
        return null;
    }
}
