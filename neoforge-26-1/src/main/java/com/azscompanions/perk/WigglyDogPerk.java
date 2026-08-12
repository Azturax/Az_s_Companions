package com.azscompanions.perk;

import com.azscompanions.AzsCompanionsConstants;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Toggleable Wiggly dog for Mister Wiggly ({@code 5b0a2d0a-…}, default ON) and the flight
 * perk UUID ({@code 4274c47f-…}, default OFF) on NeoForge 26.2.
 * While Mister Wiggly has a summoned companion, the sidekick owns the Wiggly slot.
 * At most one owned toggle dog exists server-wide; extras are discarded each tick.
 */
public final class WigglyDogPerk {
    private WigglyDogPerk() {
    }

    public static void tick(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!WigglyDogPerkSupport.isEligible(player.getUUID())) {
            return;
        }
        if (MisterWigglySidekick.isWigglyOwner(player.getUUID())
                && MisterWigglySidekick.hasSummonedCompanion(player)) {
            despawnAll(player);
            return;
        }
        if (!isShown(player)) {
            despawnAll(player);
            return;
        }
        Wolf dog = findOrCullOwned(player);
        if (dog == null || !dog.isAlive()) {
            dog = spawn(level, player);
            if (dog == null) {
                return;
            }
        }
        dog.setOwner(player);
        clearGlow(dog);
        if (SpecialPlayerPerks.isOwnerActivelyFlying(player)) {
            dog.setOrderedToSit(false);
            SpecialPlayerPerks.tickOwnedMobFlightFollow(dog, player);
        } else {
            if (dog.isNoGravity()) {
                dog.setNoGravity(false);
            }
            if (WigglyDogFlightSupport.shouldFlipSit(dog.tickCount) && dog.distanceTo(player) < 4.0d) {
                dog.setOrderedToSit(!dog.isOrderedToSit());
            }
            if (dog.distanceTo(player) > 24.0d) {
                dog.teleportTo(player.getX() + 0.6d, player.getY(), player.getZ() + 0.6d);
                dog.setDeltaMovement(Vec3.ZERO);
                dog.setOrderedToSit(false);
            }
        }
    }

    /** Toggle show/hide. Returns true when the dog is now visible. */
    public static boolean toggle(ServerPlayer player) {
        if (player == null || !WigglyDogPerkSupport.isEligible(player.getUUID())) {
            return false;
        }
        boolean show = !isShown(player);
        setShown(player, show);
        if (show) {
            tick(player);
            player.sendOverlayMessage(Component.translatable("message.azscompanions.wiggly_dog_shown"));
        } else {
            despawnAll(player);
            player.sendOverlayMessage(Component.translatable("message.azscompanions.wiggly_dog_hidden"));
        }
        return show;
    }

    public static boolean isShown(ServerPlayer player) {
        var data = player.getPersistentData();
        boolean hidden = data.getBooleanOr(WigglyDogPerkSupport.PLAYER_HIDDEN_TAG, false);
        if (hidden) {
            return false;
        }
        // Absent shown key → recipient default (ON for Mister Wiggly, OFF otherwise).
        boolean shown = data.getBooleanOr(
                WigglyDogPerkSupport.PLAYER_SHOWN_TAG,
                WigglyDogPerkSupport.defaultsVisible(player.getUUID()));
        return WigglyDogPerkSupport.isShownFromPersistentFlags(
                true, shown, true, false, player.getUUID());
    }

    public static boolean isHidden(ServerPlayer player) {
        return !isShown(player);
    }

    private static void setShown(ServerPlayer player, boolean shown) {
        player.getPersistentData().putBoolean(WigglyDogPerkSupport.PLAYER_HIDDEN_TAG, !shown);
        player.getPersistentData().putBoolean(WigglyDogPerkSupport.PLAYER_SHOWN_TAG, shown);
    }

    private static Wolf findOrCullOwned(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return null;
        }
        UUID owner = player.getUUID();
        List<Wolf> owned = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Wolf wolf && wolf.isAlive() && isToggleDog(wolf)
                        && owner.equals(ownerOf(wolf))) {
                    owned.add(wolf);
                }
            }
        }
        if (owned.isEmpty()) {
            return null;
        }
        ServerLevel playerLevel = (ServerLevel) player.level();
        Wolf keep = WigglyDogPerkSupport.pickOneToKeep(owned, wolf -> {
            double dimPenalty = wolf.level() == playerLevel ? 0.0d : 1.0e12d;
            return dimPenalty + wolf.distanceToSqr(player);
        });
        for (Wolf wolf : owned) {
            if (wolf != keep) {
                wolf.discard();
            }
        }
        return keep;
    }

    private static void despawnAll(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        UUID owner = player.getUUID();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Wolf wolf && isToggleDog(wolf) && owner.equals(ownerOf(wolf))) {
                    wolf.discard();
                }
            }
        }
    }

    private static UUID ownerOf(Wolf wolf) {
        return wolf.getPersistentData().read(WigglyDogPerkSupport.OWNER_TAG, UUIDUtil.CODEC).orElse(null);
    }

    private static boolean isToggleDog(Wolf wolf) {
        return wolf.getPersistentData().getBooleanOr(WigglyDogPerkSupport.ENTITY_TAG, false);
    }

    private static Wolf spawn(ServerLevel level, ServerPlayer player) {
        Wolf existing = findOrCullOwned(player);
        if (existing != null && existing.isAlive()) {
            return existing;
        }
        Wolf wolf = EntityType.WOLF.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (wolf == null) {
            return null;
        }
        wolf.snapTo(player.getX() + 0.8d, player.getY(), player.getZ() + 0.8d,
                player.getYRot(), 0.0f);
        wolf.setPersistenceRequired();
        wolf.setTame(true, true);
        wolf.setOwner(player);
        wolf.setCustomName(Component.literal(AzsCompanionsConstants.TOGGLE_WIGGLY_DOG_NAME));
        wolf.setCustomNameVisible(true);
        wolf.setOrderedToSit(false);
        wolf.getPersistentData().putBoolean(WigglyDogPerkSupport.ENTITY_TAG, true);
        wolf.getPersistentData().store(WigglyDogPerkSupport.OWNER_TAG, UUIDUtil.CODEC, player.getUUID());
        clearGlow(wolf);
        level.addFreshEntity(wolf);
        return wolf;
    }

    private static void clearGlow(Wolf wolf) {
        if (wolf.hasEffect(MobEffects.GLOWING)) {
            wolf.removeEffect(MobEffects.GLOWING);
        }
    }
}
