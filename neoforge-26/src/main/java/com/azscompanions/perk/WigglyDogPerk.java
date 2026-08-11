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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Toggleable Wiggly dog for {@link AzsCompanionsConstants#WOLFY_PLAYER_UUID} (NeoForge 26.2).
 * Ground-follows when walking; floats only while the owner flies/elytra.
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
        if (isHidden(player)) {
            despawnAll(player);
            return;
        }
        Wolf dog = findNear(level, player);
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
        boolean show = isHidden(player);
        setHidden(player, !show);
        if (show) {
            tick(player);
            player.sendOverlayMessage(Component.translatable("message.azscompanions.wiggly_dog_shown"));
        } else {
            despawnAll(player);
            player.sendOverlayMessage(Component.translatable("message.azscompanions.wiggly_dog_hidden"));
        }
        return show;
    }

    public static boolean isHidden(ServerPlayer player) {
        return player.getPersistentData().getBooleanOr(WigglyDogPerkSupport.PLAYER_HIDDEN_TAG, false);
    }

    private static void setHidden(ServerPlayer player, boolean hidden) {
        player.getPersistentData().putBoolean(WigglyDogPerkSupport.PLAYER_HIDDEN_TAG, hidden);
    }

    private static Wolf findNear(ServerLevel level, ServerPlayer player) {
        UUID owner = player.getUUID();
        AABB box = player.getBoundingBox().inflate(96.0d);
        List<Wolf> nearby = level.getEntitiesOfClass(Wolf.class, box, WigglyDogPerk::isToggleDog);
        for (Wolf wolf : nearby) {
            if (owner.equals(ownerOf(wolf))) {
                return wolf;
            }
        }
        return null;
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
        Wolf wolf = EntityTypes.WOLF.create(level, EntitySpawnReason.MOB_SUMMONED);
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
