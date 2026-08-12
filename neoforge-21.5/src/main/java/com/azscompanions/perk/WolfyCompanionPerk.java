package com.azscompanions.perk;

import com.azscompanions.util.OwnableUuids;

import com.azscompanions.AzsCompanionsConstants;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionForm;
import com.azscompanions.entity.CompanionRecruitment;
import com.azscompanions.entity.CompanionRegistry;
import com.azscompanions.item.CharmData;
import com.azscompanions.item.CompanionCharmItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * UUID-gated Wolfy perk (NeoForge): one-time wolf-form companion named Wolfy.
 */
public final class WolfyCompanionPerk {
    private WolfyCompanionPerk() {
    }

    public static void ensureFor(ServerPlayer player) {
        if (player == null || player.level().isClientSide) {
            return;
        }
        if (!WolfyPerkSupport.isWolfyOwner(player.getUUID())) {
            return;
        }
        if (player.getTags().contains(WolfyPerkSupport.PLAYER_GRANTED_TAG)) {
            return;
        }
        if (player.tickCount % 40 != 0) {
            return;
        }
        if (findExistingWolfy(player) || hasStoredWolfy(player)) {
            player.addTag(WolfyPerkSupport.PLAYER_GRANTED_TAG);
            return;
        }
        CompanionEntity spawned = spawnWolfy(player);
        if (spawned != null) {
            player.addTag(WolfyPerkSupport.PLAYER_GRANTED_TAG);
        }
    }

    private static CompanionEntity spawnWolfy(ServerPlayer player) {
        CompanionEntity companion = CompanionRecruitment.recruit(
                player, CompanionRegistry.KON_ID.toString());
        if (companion == null) {
            return null;
        }
        companion.setForm(CompanionForm.WOLF);
        companion.setFormVariant(WolfyPerkSupport.BROWN_WOLF_VARIANT_ID);
        companion.setCustomDisplayName(AzsCompanionsConstants.WOLFY_COMPANION_NAME);
        companion.setSkinPath("");
        companion.setNameTagVisible(true);
        return companion;
    }

    private static boolean findExistingWolfy(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        UUID owner = player.getUUID();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof CompanionEntity companion
                        && owner.equals(OwnableUuids.get(companion))
                        && WolfyPerkSupport.isWolfyName(companion.getChatDisplayName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasStoredWolfy(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!CompanionCharmItem.isCharm(stack) || !CharmData.hasStoredCompanion(stack)) {
                continue;
            }
            CompoundTag stored = CharmData.peekStoredCompanion(stack);
            if (stored == null) {
                continue;
            }
            String name = stored.contains("CustomNameOverride")
                    ? stored.getStringOr("CustomNameOverride", "")
                    : "";
            boolean flag = stored.contains(WolfyPerkSupport.COMPANION_NBT_FLAG)
                    && stored.getBooleanOr(WolfyPerkSupport.COMPANION_NBT_FLAG, false);
            if (WolfyPerkSupport.looksLikeStoredWolfy(name, flag)) {
                return true;
            }
        }
        return false;
    }
}
