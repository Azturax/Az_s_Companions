package com.azscompanions.perk;

import com.azscompanions.AzsCompanionsConstants;
import com.azscompanions.entity.CompanionFlightFollowSupport;
import com.azscompanions.entity.CompanionFollowDistances;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricCompanionMode;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * UUID-gated perks (Fabric):
 * flight + glow, and client-side Kon ears cosmetic (see {@link #hasKonEars}).
 */
public final class SpecialPlayerPerks {
    private static final int GLOW_DURATION_TICKS = 100;
    private static final int GLOW_REFRESH_BELOW = 40;
    /** Soft hover offset above the owner's feet while flying. */
    private static final double FLIGHT_HOVER_Y = CompanionFlightFollowSupport.HOVER_Y;
    /** Never land-teleport when already this close (matches ground MIN_TELEPORT_DISTANCE). */
    private static final double MIN_SAFE_LAND_TELEPORT = 24.0d;

    private SpecialPlayerPerks() {
    }

    public static boolean isSpecial(UUID uuid) {
        return uuid != null && AzsCompanionsConstants.SPECIAL_PERK_PLAYER_UUID.equals(uuid);
    }

    public static boolean isSpecial(Player player) {
        return player != null && isSpecial(player.getUUID());
    }

    /** Client cosmetic Kon ears for this UUID (render layer; synced by UUID check). */
    public static boolean hasKonEars(UUID uuid) {
        return uuid != null && AzsCompanionsConstants.KON_EARS_PLAYER_UUID.equals(uuid);
    }

    public static boolean hasKonEars(Player player) {
        return player != null && hasKonEars(player.getUUID());
    }

    /** True when the owner is in creative/survival flight or elytra gliding. */
    public static boolean isOwnerActivelyFlying(Player owner) {
        if (owner == null) {
            return false;
        }
        return owner.getAbilities().flying || owner.isFallFlying();
    }

    public static void applyPlayerPerks(ServerPlayer player) {
        if (isSpecial(player)) {
            var abilities = player.getAbilities();
            if (!abilities.mayfly) {
                abilities.mayfly = true;
                player.onUpdateAbilities();
            }
            ensureGlow(player);
        } else if (WigglyDogPerkSupport.isEligible(player.getUUID())) {
            clearGlow(player);
        }
        WolfyCompanionPerk.ensureFor(player);
        WigglyDogPerk.tick(player);
    }

    public static void applyCompanionPerks(Mob companion, UUID ownerUuid) {
        if (WigglyDogPerkSupport.isEligible(ownerUuid)) {
            clearGlow(companion);
        }
        if (companion instanceof FabricCompanionEntity orb && orb.getForm().isOrb()) {
            companion.setNoGravity(true);
            return;
        }
        if (isSpecial(ownerUuid)) {
            ensureGlow(companion);
            Player owner = companion.level().getPlayerByUUID(ownerUuid);
            if (isHoldingStayPosition(companion)) {
                if (companion.isNoGravity()) {
                    companion.setNoGravity(false);
                }
                return;
            }
            if (owner != null && isOwnerActivelyFlying(owner)) {
                companion.setNoGravity(true);
            } else if (owner != null) {
                landCompanionNearOwner(companion, owner);
            } else if (companion.isNoGravity()) {
                companion.setNoGravity(false);
            }
        } else if (companion.isNoGravity()) {
            companion.setNoGravity(false);
        }
    }

    /** Stay/Sit companions never teleport via special-perk snaps. */
    public static boolean isHoldingStayPosition(Mob companion) {
        if (companion instanceof FabricCompanionEntity c) {
            FabricCompanionMode mode = c.getMode();
            return mode == FabricCompanionMode.STAY || mode == FabricCompanionMode.SIT;
        }
        return false;
    }

    /**
     * Special-UUID flying companion follow — holds the personal-space ring (not the owner's hitbox).
     *
     * @return {@code true} if this method fully handled movement (owner flying);
     *         {@code false} when the owner is grounded so normal ground follow should run.
     */
    public static boolean tickCompanionFlightFollow(
            Mob companion,
            Player owner,
            double teleportDistance,
            double personalSpace) {
        if (owner == null || !isSpecial(owner.getUUID())) {
            return false;
        }
        if (isHoldingStayPosition(companion)) {
            if (companion.isNoGravity()) {
                companion.setNoGravity(false);
            }
            return true;
        }

        if (!isOwnerActivelyFlying(owner)) {
            landCompanionNearOwner(companion, owner);
            return false;
        }

        companion.setNoGravity(true);
        companion.getNavigation().stop();

        double space = Math.max(CompanionFollowDistances.PERSONAL_SPACE_MIN, personalSpace);
        double preferred = CompanionFollowDistances.preferredDistance(space);
        double dist = companion.distanceTo(owner);
        if (CompanionFlightFollowSupport.shouldFlightSnap(dist, teleportDistance)) {
            snapBesideOwner(companion, owner, preferred, space);
            return true;
        }

        double[] target = CompanionFlightFollowSupport.preferredFlightTarget(
                owner.getX(), owner.getY(), owner.getZ(),
                companion.getX(), companion.getZ(),
                preferred);
        double distToTarget = companion.position().distanceTo(new Vec3(target[0], target[1], target[2]));
        if (distToTarget < CompanionFlightFollowSupport.ARRIVE_EPSILON) {
            Vec3 motion = companion.getDeltaMovement();
            double[] hold = CompanionFlightFollowSupport.holdVelocity(
                    motion.x, motion.y, motion.z, target[1], companion.getY());
            companion.setDeltaMovement(hold[0], hold[1], hold[2]);
            companion.hasImpulse = true;
            companion.getLookControl().setLookAt(owner, 10.0f, companion.getMaxHeadXRot());
            return true;
        }

        double speed = CompanionFollowDistances.tooClose(dist, space)
                ? CompanionFlightFollowSupport.FLIGHT_SPEED_FAR
                : CompanionFlightFollowSupport.speedForDistance(distToTarget);
        double[] vel = CompanionFlightFollowSupport.velocityToward(
                companion.getX(), companion.getY(), companion.getZ(),
                target[0], target[1], target[2],
                speed);
        companion.setDeltaMovement(vel[0], vel[1], vel[2]);
        companion.hasImpulse = true;
        companion.getLookControl().setLookAt(owner, 10.0f, companion.getMaxHeadXRot());
        return true;
    }

    public static boolean tickCompanionFlightFollow(Mob companion, Player owner, double teleportDistance) {
        double space = CompanionFollowDistances.DEFAULT_PERSONAL_SPACE;
        if (companion instanceof FabricCompanionEntity c) {
            space = c.getPersonalSpace();
        }
        return tickCompanionFlightFollow(companion, owner, teleportDistance, space);
    }

    /**
     * Owner-relative flight follow for any mob (e.g. toggle Wiggly dog).
     * Flies only while the owner is actively flying; lands when grounded.
     */
    public static boolean tickOwnedMobFlightFollow(Mob mob, Player owner) {
        if (owner == null || mob == null) {
            return false;
        }
        if (!isOwnerActivelyFlying(owner)) {
            landCompanionNearOwner(mob, owner);
            return false;
        }

        mob.setNoGravity(true);
        mob.getNavigation().stop();

        double space = CompanionFollowDistances.DEFAULT_PERSONAL_SPACE;
        double preferred = CompanionFollowDistances.preferredDistance(space);
        double dist = mob.distanceTo(owner);
        if (CompanionFlightFollowSupport.shouldFlightSnap(dist, CompanionFollowDistances.TELEPORT_DISTANCE)) {
            snapBesideOwner(mob, owner, preferred, space);
            return true;
        }

        double[] target = CompanionFlightFollowSupport.preferredFlightTarget(
                owner.getX(), owner.getY(), owner.getZ(),
                mob.getX(), mob.getZ(),
                preferred);
        double distToTarget = mob.position().distanceTo(new Vec3(target[0], target[1], target[2]));
        if (distToTarget < CompanionFlightFollowSupport.ARRIVE_EPSILON) {
            Vec3 motion = mob.getDeltaMovement();
            double[] hold = CompanionFlightFollowSupport.holdVelocity(
                    motion.x, motion.y, motion.z, target[1], mob.getY());
            double bob = WigglyDogFlightSupport.bobDeltaY(mob.tickCount);
            mob.setDeltaMovement(hold[0], hold[1] + bob, hold[2]);
            mob.hasImpulse = true;
            mob.getLookControl().setLookAt(owner, 10.0f, mob.getMaxHeadXRot());
            return true;
        }

        double speed = CompanionFollowDistances.tooClose(dist, space)
                ? CompanionFlightFollowSupport.FLIGHT_SPEED_FAR
                : CompanionFlightFollowSupport.speedForDistance(distToTarget);
        double[] vel = CompanionFlightFollowSupport.velocityToward(
                mob.getX(), mob.getY(), mob.getZ(),
                target[0], target[1], target[2],
                speed);
        double bob = WigglyDogFlightSupport.bobDeltaY(mob.tickCount);
        mob.setDeltaMovement(vel[0], vel[1] + bob, vel[2]);
        mob.hasImpulse = true;
        mob.getLookControl().setLookAt(owner, 10.0f, mob.getMaxHeadXRot());
        return true;
    }

    private static void landCompanionNearOwner(Mob companion, Player owner) {
        if (companion.isNoGravity()) {
            companion.setNoGravity(false);
        }
        boolean floatingAbove = !companion.onGround() && companion.getY() > owner.getY() + 1.25d;
        double dist = companion.distanceTo(owner);
        if (floatingAbove && dist < MIN_SAFE_LAND_TELEPORT) {
            Vec3 motion = companion.getDeltaMovement();
            companion.setDeltaMovement(motion.x * 0.5d, Math.min(motion.y, -0.15d), motion.z * 0.5d);
            companion.hasImpulse = true;
            return;
        }
        if (dist >= MIN_SAFE_LAND_TELEPORT && (floatingAbove || dist > 15.0d)) {
            double preferred = CompanionFollowDistances.PREFERRED_DISTANCE;
            snapBesideOwner(companion, owner, preferred, CompanionFollowDistances.MIN_PERSONAL_SPACE);
        } else {
            Vec3 motion = companion.getDeltaMovement();
            if (Math.abs(motion.y) > 0.05d && motion.y > 0.0d) {
                companion.setDeltaMovement(motion.x * 0.6d, Math.min(motion.y, 0.0d), motion.z * 0.6d);
                companion.hasImpulse = true;
            }
        }
    }

    private static void snapBesideOwner(Mob companion, Player owner, double sideOffset, double personalSpace) {
        // Never snap under personal space — land on the preferred follow ring.
        if (companion.distanceTo(owner) < personalSpace) {
            return;
        }
        double offsetDist = Math.max(personalSpace, sideOffset);
        Vec3 away = companion.position().subtract(owner.position());
        if (away.horizontalDistanceSqr() < 1.0e-4d) {
            away = new Vec3(0.5d, 0.0d, 0.5d);
        }
        Vec3 offset = new Vec3(away.x, 0.0d, away.z).normalize().scale(offsetDist);
        companion.teleportTo(
                owner.getX() + offset.x,
                owner.getY() + FLIGHT_HOVER_Y,
                owner.getZ() + offset.z);
        companion.setDeltaMovement(Vec3.ZERO);
        companion.hasImpulse = true;
        companion.getLookControl().setLookAt(owner, 10.0f, companion.getMaxHeadXRot());
    }

    private static void ensureGlow(LivingEntity entity) {
        MobEffectInstance existing = entity.getEffect(MobEffects.GLOWING);
        if (existing == null || existing.getDuration() < GLOW_REFRESH_BELOW) {
            entity.addEffect(new MobEffectInstance(
                    MobEffects.GLOWING,
                    GLOW_DURATION_TICKS,
                    0,
                    true,
                    false,
                    true));
        }
    }

    private static void clearGlow(LivingEntity entity) {
        if (entity != null && entity.hasEffect(MobEffects.GLOWING)) {
            entity.removeEffect(MobEffects.GLOWING);
        }
    }
}
