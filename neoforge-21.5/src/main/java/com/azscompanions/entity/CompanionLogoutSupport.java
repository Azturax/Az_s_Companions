package com.azscompanions.entity;

import com.azscompanions.util.OwnableUuids;

import com.azscompanions.util.NbtUuids;

import com.azscompanions.item.CharmData;
import com.azscompanions.item.CompanionCharmItem;
import com.azscompanions.perk.MisterWigglySidekick;
import com.azscompanions.registry.ModItems;
import com.azscompanions.world.CompanionIdentityStore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Despawn owned companions on logout (persisted) and restore them near the player on login.
 * Charm-bound companions also mirror into charm NBT with {@link CharmData#storeCompanionForLogout}.
 */
public final class CompanionLogoutSupport {
    private CompanionLogoutSupport() {
    }

    public static void parkOwnedCompanions(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        UUID owner = player.getUUID();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof CompanionEntity companion
                        && companion.isAlive()
                        && owner.equals(OwnableUuids.get(companion))
                        && !CompanionLogoutPersistence.shouldParkOnLogout(companion.isCciSummoned())) {
                    companion.discard();
                }
            }
        }
        List<CompanionEntity> roots = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof CompanionEntity companion
                        && companion.isAlive()
                        && owner.equals(OwnableUuids.get(companion))
                        && !companion.isChildCompanion()) {
                    roots.add(companion);
                }
            }
        }

        ListTag parked = new ListTag();
        ItemStack charm = findCharm(player);
        UUID charmBound = charm != null ? CharmData.getBoundUuid(charm) : null;

        for (CompanionEntity companion : roots) {
            companion.storeAllLivingChildren();
            MisterWigglySidekick.despawnFor(companion);
            CompoundTag data = new CompoundTag();
            companion.saveWithoutId(data);
            UUID id = companion.getUUID();
            parked.add(entry(id, data));
            CompanionIdentityStore.get(server).putIdentity(id, data);
            if (charm != null && id.equals(charmBound)) {
                CharmData.storeCompanionForLogout(charm, data.copy(), id);
            }
            companion.discard();
        }

        // Orphan Bits whose parent was already gone.
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof CompanionEntity companion)
                        || !companion.isAlive()
                        || !owner.equals(OwnableUuids.get(companion))) {
                    continue;
                }
                CompoundTag data = new CompoundTag();
                companion.saveWithoutId(data);
                parked.add(entry(companion.getUUID(), data));
                MisterWigglySidekick.despawnFor(companion);
                companion.discard();
            }
        }

        CompoundTag persistent = player.getPersistentData();
        if (persistent.contains(CompanionLogoutPersistence.PLAYER_LIST_TAG)) {
            ListTag prior = persistent.getListOrEmpty(CompanionLogoutPersistence.PLAYER_LIST_TAG);
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
        }
        if (!parked.isEmpty()) {
            persistent.put(CompanionLogoutPersistence.PLAYER_LIST_TAG, parked);
        }
    }

    

    public static void restoreParkedCompanions(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        ListTag parked = persistent.contains(CompanionLogoutPersistence.PLAYER_LIST_TAG)
                ? persistent.getListOrEmpty(CompanionLogoutPersistence.PLAYER_LIST_TAG)
                : new ListTag();

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
            if (CompanionRecruitment.findOwned(player, id) != null) {
                restored.add(id);
                continue;
            }
            CompanionEntity spawned = CompanionRecruitment.spawnFromStored(player, data.copy(), id);
            if (spawned != null) {
                MisterWigglySidekick.ensureFor(spawned);
                restored.add(id);
            } else {
                remaining.add(entry.copy());
            }
        }

        ItemStack charm = findCharm(player);
        if (charm != null) {
            UUID bound = CharmData.getBoundUuid(charm);
            if (bound != null && restored.contains(bound) && CharmData.hasStoredCompanion(charm)) {
                CharmData.clearStoredCompanion(charm);
            } else if (CharmData.isLogoutParked(charm) && CharmData.hasStoredCompanion(charm) && bound != null) {
                if (CompanionRecruitment.findOwned(player, bound) == null) {
                    CompoundTag stored = CharmData.peekStoredCompanion(charm);
                    if (stored != null) {
                        CompanionEntity spawned = CompanionRecruitment.spawnFromStored(player, stored.copy(), bound);
                        if (spawned != null) {
                            MisterWigglySidekick.ensureFor(spawned);
                            CharmData.clearStoredCompanion(charm);
                            restored.add(bound);
                        }
                    }
                } else {
                    CharmData.clearStoredCompanion(charm);
                }
            }
        }

        if (remaining.isEmpty()) {
            persistent.remove(CompanionLogoutPersistence.PLAYER_LIST_TAG);
        } else {
            persistent.put(CompanionLogoutPersistence.PLAYER_LIST_TAG, remaining);
        }
    }

    private static CompoundTag entry(UUID id, CompoundTag data) {
        CompoundTag entry = new CompoundTag();
        NbtUuids.put(entry, CompanionLogoutPersistence.ENTRY_UUID, id);
        entry.put(CompanionLogoutPersistence.ENTRY_DATA, data);
        return entry;
    }

    @Nullable
    private static ItemStack findCharm(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (CompanionCharmItem.isCharm(stack) || stack.is(ModItems.COMPANION_CHARM.get())) {
                return stack;
            }
        }
        return null;
    }
}
