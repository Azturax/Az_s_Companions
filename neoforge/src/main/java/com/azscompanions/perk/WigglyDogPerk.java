package com.azscompanions.perk;

import com.azscompanions.AzsCompanionsConstants;
import com.azscompanions.entity.CompanionLogoutPersistence;
import com.azscompanions.entity.CompanionSafeTeleportSupport;
import com.azscompanions.entity.CompanionSpawnGuardSupport;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Toggleable Wiggly dog for Mister Wiggly ({@code 5b0a2d0a-…}, default ON) and the flight
 * perk UUID ({@code 4274c47f-…}, default OFF). Ground-follows when walking; floats only
 * while the owner flies/elytra. Separate from the Wolfy grant.
 * <p>
 * While Mister Wiggly has a summoned companion, the companion sidekick owns the single
 * Wiggly slot (toggle dog is despawned) so at most one Wiggly exists.
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
        cullAllOwnedWigglyNamed(player);
        // Prefer companion-of-companion sidekick so Mister Wiggly never has two Wigglys.
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
            if (CompanionSpawnGuardSupport.inLoginGrace(player.tickCount)) {
                return;
            }
            dog = spawn(level, player);
            if (dog == null) {
                return;
            }
        }
        rememberUuid(player, dog.getUUID());
        dog.setOwnerUUID(player.getUUID());
        dog.setInvulnerable(true);
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

    /** At most one wolf named/tagged Wiggly per owner (toggle + sidekick share the slot). */
    private static void cullAllOwnedWigglyNamed(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        UUID owner = player.getUUID();
        boolean preferSidekick = MisterWigglySidekick.isWigglyOwner(owner)
                && MisterWigglySidekick.hasSummonedCompanion(player);
        List<Wolf> owned = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Wolf wolf && wolf.isAlive()
                        && isAnyOwnedWiggly(wolf, owner)) {
                    owned.add(wolf);
                }
            }
        }
        if (owned.size() <= 1) {
            return;
        }
        ServerLevel playerLevel = (ServerLevel) player.level();
        Wolf keep = WigglyDogPerkSupport.pickOneToKeep(owned, wolf -> WigglyDogPerkSupport.keepScore(
                preferSidekick,
                isSidekick(wolf),
                isToggleDog(wolf),
                (wolf.level() == playerLevel ? 0.0d : 1.0e12d) + wolf.distanceToSqr(player)));
        for (Wolf wolf : owned) {
            if (wolf != keep) {
                wolf.discard();
            }
        }
    }

    public static void parkFor(ServerPlayer player) {
        Wolf dog = findOrCullOwned(player);
        if (dog != null && dog.isAlive()) {
            rememberUuid(player, dog.getUUID());
        }
        despawnAll(player);
    }

    private static void rememberUuid(ServerPlayer player, UUID dogUuid) {
        if (player == null || dogUuid == null) {
            return;
        }
        player.getPersistentData().putUUID(CompanionLogoutPersistence.WIGGLY_ENTITY_UUID, dogUuid);
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
        CompoundTag data = wolf.getPersistentData();
        if (data.hasUUID(WigglyDogPerkSupport.OWNER_TAG)) {
            return data.getUUID(WigglyDogPerkSupport.OWNER_TAG);
        }
        return wolf.getOwnerUUID();
    }

    private static boolean isSidekick(Wolf wolf) {
        return wolf.getPersistentData().getBoolean(MisterWigglySidekick.TAG_SIDEKICK);
    }

    private static boolean isToggleDog(Wolf wolf) {
        if (isSidekick(wolf)) {
            return false;
        }
        String name = wolf.getCustomName() != null ? wolf.getCustomName().getString() : null;
        return wolf.getPersistentData().getBoolean(WigglyDogPerkSupport.ENTITY_TAG)
                || wolf.getTags().contains(WigglyDogPerkSupport.ENTITY_TAG)
                || WigglyDogPerkSupport.isToggleDogName(name);
    }

    private static boolean isAnyOwnedWiggly(Wolf wolf, UUID owner) {
        UUID wolfOwner = ownerOf(wolf);
        String name = wolf.getCustomName() != null ? wolf.getCustomName().getString() : null;
        return WigglyDogPerkSupport.looksLikeOwnedWiggly(
                wolf.getPersistentData().getBoolean(WigglyDogPerkSupport.ENTITY_TAG)
                        || wolf.getTags().contains(WigglyDogPerkSupport.ENTITY_TAG),
                isSidekick(wolf),
                name,
                owner,
                wolfOwner);
    }

    private static Wolf spawn(ServerLevel level, ServerPlayer player) {
        Wolf existing = findOrCullOwned(player);
        if (existing != null && existing.isAlive()) {
            return existing;
        }
        Wolf wolf = EntityType.WOLF.create(level);
        if (wolf == null) {
            return null;
        }
        double[] behind = CompanionSafeTeleportSupport.behindOwner(player.getYRot(), 2.5d);
        wolf.moveTo(player.getX() + behind[0], player.getY(), player.getZ() + behind[1],
                player.getYRot(), 0.0f);
        wolf.setPersistenceRequired();
        wolf.setTame(true, true);
        wolf.setOwnerUUID(player.getUUID());
        wolf.setCustomName(Component.literal(AzsCompanionsConstants.TOGGLE_WIGGLY_DOG_NAME));
        wolf.setCustomNameVisible(true);
        wolf.setOrderedToSit(false);
        wolf.setInvulnerable(true);
        var nbt = new CompoundTag();
        wolf.saveWithoutId(nbt);
        nbt.putByte("CollarColor", (byte) DyeColor.PINK.getId());
        wolf.load(nbt);
        wolf.getPersistentData().putBoolean(WigglyDogPerkSupport.ENTITY_TAG, true);
        wolf.getPersistentData().putUUID(WigglyDogPerkSupport.OWNER_TAG, player.getUUID());
        wolf.addTag(WigglyDogPerkSupport.ENTITY_TAG);
        clearGlow(wolf);
        if (!level.addFreshEntity(wolf)) {
            return findOrCullOwned(player);
        }
        rememberUuid(player, wolf.getUUID());
        return wolf;
    }

    private static void clearGlow(Wolf wolf) {
        if (wolf.hasEffect(MobEffects.GLOWING)) {
            wolf.removeEffect(MobEffects.GLOWING);
        }
    }
}
