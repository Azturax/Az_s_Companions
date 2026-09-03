package com.azscompanions.perk;

import com.azscompanions.AzsCompanionsConstants;
import com.azscompanions.util.OwnableUuids;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Toggleable Wiggly dog for Mister Wiggly ({@code 5b0a2d0a-…}, default ON) and the flight
 * perk UUID ({@code 4274c47f-…}, default OFF) on Fabric.
 * Ground-follows when walking; floats only while the owner flies/elytra.
 * At most one owned toggle dog exists server-wide; extras are discarded each tick.
 */
public final class WigglyDogPerk {
    private WigglyDogPerk() {
    }

    public static void tick(ServerPlayer player) {
        if (player == null || player.level().isClientSide || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!WigglyDogPerkSupport.isEligible(player.getUUID())) {
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
        OwnableUuids.set(dog, player.getUUID());
        applyDogScale(dog);
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
                SpecialPlayerPerks.safeTeleportBeside(dog, player, 2.5d);
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
            player.displayClientMessage(Component.translatable("message.azscompanions.wiggly_dog_shown"), true);
        } else {
            despawnAll(player);
            player.displayClientMessage(Component.translatable("message.azscompanions.wiggly_dog_hidden"), true);
        }
        return show;
    }

    public static boolean isShown(ServerPlayer player) {
        return WigglyDogPerkSupport.isShownFromTags(player.getTags(), player.getUUID());
    }

    public static boolean isHidden(ServerPlayer player) {
        return !isShown(player);
    }

    private static void setShown(ServerPlayer player, boolean shown) {
        if (shown) {
            player.removeTag(WigglyDogPerkSupport.PLAYER_HIDDEN_TAG);
            player.addTag(WigglyDogPerkSupport.PLAYER_SHOWN_TAG);
        } else {
            player.removeTag(WigglyDogPerkSupport.PLAYER_SHOWN_TAG);
            // Required so default-ON recipients stay dismissed (empty tags ⇒ defaultsVisible).
            player.addTag(WigglyDogPerkSupport.PLAYER_HIDDEN_TAG);
        }
    }

    private static Wolf findOrCullOwned(ServerPlayer player) {
        MinecraftServer server = player.getServer();
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
        MinecraftServer server = player.getServer();
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
        return OwnableUuids.get(wolf);
    }

    private static boolean isToggleDog(Wolf wolf) {
        return wolf.getTags().contains(WigglyDogPerkSupport.ENTITY_TAG);
    }

    private static Wolf spawn(ServerLevel level, ServerPlayer player) {
        Wolf existing = findOrCullOwned(player);
        if (existing != null && existing.isAlive()) {
            return existing;
        }
        Wolf wolf = EntityType.WOLF.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (wolf == null) {
            return null;
        }
        wolf.snapTo(player.getX() + 0.8d, player.getY(), player.getZ() + 0.8d,
                player.getYRot(), 0.0f);
        wolf.setPersistenceRequired();
        wolf.setTame(true, true);
        OwnableUuids.set(wolf, player.getUUID());
        wolf.setCustomName(Component.literal(AzsCompanionsConstants.TOGGLE_WIGGLY_DOG_NAME));
        wolf.setCustomNameVisible(true);
        wolf.setOrderedToSit(false);
        var nbt = new CompoundTag();
        wolf.saveWithoutId(nbt);
        nbt.putByte("CollarColor", (byte) DyeColor.PINK.getId());
        wolf.load(nbt);
        wolf.addTag(WigglyDogPerkSupport.ENTITY_TAG);
        applyDogScale(wolf);
        clearGlow(wolf);
        level.addFreshEntity(wolf);
        return wolf;
    }

    private static void applyDogScale(Wolf wolf) {
        var attr = wolf.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
        if (attr == null) {
            return;
        }
        if (WigglyDogPerkSupport.scaleNeedsUpdate(attr.getBaseValue())) {
            attr.setBaseValue(WigglyDogPerkSupport.DOG_SCALE);
            wolf.refreshDimensions();
        }
    }

    private static void clearGlow(Wolf wolf) {
        if (wolf.hasEffect(MobEffects.GLOWING)) {
            wolf.removeEffect(MobEffects.GLOWING);
        }
    }
}
