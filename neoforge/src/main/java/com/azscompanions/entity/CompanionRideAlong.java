package com.azscompanions.entity;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Loader-side ride-along: classify vehicles, befriend/mount, steer, sync dismount.
 */
public final class CompanionRideAlong {
    private CompanionRideAlong() {
    }

    public static CompanionRideAlongSupport.RideKind classify(@Nullable Entity entity) {
        if (entity == null || !entity.isAlive()) {
            return CompanionRideAlongSupport.RideKind.NONE;
        }
        if (entity instanceof Camel) {
            return CompanionRideAlongSupport.RideKind.CAMEL;
        }
        if (entity instanceof Llama) {
            return CompanionRideAlongSupport.RideKind.LLAMA;
        }
        if (entity instanceof AbstractHorse) {
            return CompanionRideAlongSupport.RideKind.HORSE;
        }
        if (entity instanceof Boat) {
            return CompanionRideAlongSupport.RideKind.BOAT;
        }
        if (entity instanceof Minecart) {
            return CompanionRideAlongSupport.RideKind.MINECART;
        }
        if (entity instanceof AbstractMinecart) {
            return CompanionRideAlongSupport.RideKind.NONE;
        }
        if (entity instanceof Pig) {
            return CompanionRideAlongSupport.RideKind.PIG;
        }
        if (entity instanceof Strider) {
            return CompanionRideAlongSupport.RideKind.STRIDER;
        }
        return CompanionRideAlongSupport.RideKind.NONE;
    }

    public static boolean isOwnerRidingSupported(Player owner) {
        return classify(owner.getVehicle()) != CompanionRideAlongSupport.RideKind.NONE;
    }

    @Nullable
    public static Entity findPreferredCandidate(
            Mob companion,
            Player owner,
            CompanionRideAlongSupport.RideKind want) {
        if (want == CompanionRideAlongSupport.RideKind.NONE) {
            return null;
        }
        Entity ownerVehicle = owner.getVehicle();
        UUID ownerId = owner.getUUID();
        double range = CompanionRideAlongSupport.SEARCH_RANGE;
        AABB box = companion.getBoundingBox().inflate(range);
        List<Entity> found = companion.level().getEntities(companion, box, e -> {
            if (!CompanionRideAlongSupport.kindsMatch(want, classify(e))) {
                return false;
            }
            if (e == ownerVehicle || e == companion) {
                return false;
            }
            boolean empty = e.getPassengers().isEmpty();
            boolean ownedByOther = isOwnedByOther(e, ownerId);
            return CompanionRideAlongSupport.isPreferredCandidate(empty, false, ownedByOther);
        });
        return found.stream()
                .min(Comparator.comparingDouble(companion::distanceToSqr))
                .orElse(null);
    }

    @Nullable
    public static Entity findApproachCandidate(
            Mob companion,
            Player owner,
            CompanionRideAlongSupport.RideKind want) {
        if (want == CompanionRideAlongSupport.RideKind.NONE) {
            return null;
        }
        Entity ownerVehicle = owner.getVehicle();
        double range = CompanionRideAlongSupport.SEARCH_RANGE;
        AABB box = companion.getBoundingBox().inflate(range);
        List<Entity> found = companion.level().getEntities(companion, box, e ->
                CompanionRideAlongSupport.kindsMatch(want, classify(e))
                        && e != ownerVehicle
                        && e != companion);
        return found.stream()
                .min(Comparator.comparingDouble(companion::distanceToSqr))
                .orElse(null);
    }

    public static boolean tryBefriendAndMount(Mob companion, Player owner, Entity candidate) {
        if (candidate == null || !candidate.isAlive() || companion.level().isClientSide) {
            return false;
        }
        if (candidate == owner.getVehicle() || !candidate.getPassengers().isEmpty()) {
            return false;
        }
        if (isOwnedByOther(candidate, owner.getUUID())) {
            return false;
        }
        befriendIfNeeded(candidate, owner);
        ensureSaddled(candidate);
        return companion.startRiding(candidate, true);
    }

    public static void befriendIfNeeded(Entity candidate, Player owner) {
        if (candidate instanceof AbstractHorse horse) {
            if (!horse.isTamed()) {
                horse.setTamed(true);
                horse.setOwnerUUID(owner.getUUID());
                Level level = horse.level();
                if (!level.isClientSide) {
                    // Entity event 7 = heart particles (same as successful tame).
                    level.broadcastEntityEvent(horse, (byte) 7);
                }
            } else if (horse.getOwnerUUID() == null) {
                horse.setOwnerUUID(owner.getUUID());
            }
        }
    }

    public static void ensureSaddled(Entity candidate) {
        if (!(candidate instanceof Saddleable saddleable)) {
            return;
        }
        if (saddleable.isSaddled() || !saddleable.isSaddleable()) {
            return;
        }
        saddleable.equipSaddle(new ItemStack(Items.SADDLE), SoundSource.NEUTRAL);
    }

    public static void steerVehicleTowardOwner(Entity vehicle, Player owner, double personalSpace) {
        if (vehicle == null || owner == null) {
            return;
        }
        double preferred = CompanionFollowDistances.preferredDistance(personalSpace);
        double dist = vehicle.distanceTo(owner);
        if (dist <= preferred + 0.5d) {
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().scale(0.6d));
            return;
        }
        double speed = CompanionRideAlongSupport.steerSpeedForDistance(dist);
        double[] vel = CompanionRideAlongSupport.steerVelocity(
                vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                owner.getX(), owner.getY(), owner.getZ(),
                preferred,
                speed);
        vehicle.setDeltaMovement(
                vel[0],
                vehicle instanceof Boat ? vel[1] * 0.35d : vehicle.getDeltaMovement().y * 0.6d + vel[1] * 0.4d,
                vel[2]);
        vehicle.hasImpulse = true;
        float yaw = (float) (Math.atan2(-vel[0], vel[2]) * (180.0d / Math.PI));
        vehicle.setYRot(yaw);
        if (vehicle instanceof LivingEntity living) {
            living.setYBodyRot(yaw);
            living.setYHeadRot(yaw);
        }
        if (vehicle instanceof Mob mob && dist > preferred + 2.0d) {
            mob.getNavigation().moveTo(owner, 1.25d);
        }
    }

    public static void stopRideAlong(Mob companion) {
        if (companion.isPassenger()) {
            companion.stopRiding();
        }
    }

    private static boolean isOwnedByOther(Entity entity, UUID ownerId) {
        if (!(entity instanceof OwnableEntity ownable)) {
            return false;
        }
        UUID uuid = ownable.getOwnerUUID();
        return uuid != null && !uuid.equals(ownerId);
    }
}
