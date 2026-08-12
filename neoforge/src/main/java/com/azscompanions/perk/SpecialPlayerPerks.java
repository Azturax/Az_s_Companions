package com.azscompanions.perk;

import com.azscompanions.AzsCompanionsConstants;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionFlightFollowSupport;
import com.azscompanions.entity.CompanionFollowDistances;
import com.azscompanions.entity.CompanionMode;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * UUID-gated perks (NeoForge):
 * flight (no auto-glow), toggle Wiggly (Mister Wiggly default ON; flight UUID opt-in), Kon ears.
 */
public final class SpecialPlayerPerks {
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

    public static boolean isMisterWiggly(UUID uuid) {
        return MisterWigglySidekick.isWigglyOwner(uuid);
    }

    public static boolean isMisterWiggly(Player player) {
        return player != null && isMisterWiggly(player.getUUID());
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
            // Flight only — auto-glow removed for this UUID. Wiggly dog is Mister Wiggly's perk.
            clearGlow(player);
        }
        WolfyCompanionPerk.ensureFor(player);
        WigglyDogPerk.tick(player);
    }

    public static void applyCompanionPerks(Mob companion, UUID ownerUuid) {
        if (isSpecial(ownerUuid)) {
            clearGlow(companion);
            Player owner = companion.level().getPlayerByUUID(ownerUuid);
            // Stay/Sit: hold position like a sitting wolf — no flight follow / land snaps.
            if (isHoldingStayPosition(companion)) {
                if (companion.isNoGravity()) {
                    companion.setNoGravity(false);
                }
                return;
            }
            if (owner != null && isOwnerActivelyFlying(owner)) {
                companion.setNoGravity(true);
            } else if (owner != null) {
                // Runs every companion tick so landing works even if follow goal stopped.
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
        if (companion instanceof CompanionEntity c) {
            CompanionMode mode = c.getMode();
            return mode == CompanionMode.STAY || mode == CompanionMode.SIT || c.isSitting();
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
            return true; // fully handled: stay put
        }
        return tickOwnedMobFlightFollow(companion, owner, teleportDistance, personalSpace, false);
    }

    public static boolean tickCompanionFlightFollow(Mob companion, Player owner, double teleportDistance) {
        double space = CompanionFollowDistances.DEFAULT_PERSONAL_SPACE;
        if (companion instanceof CompanionEntity c) {
            space = c.getPersonalSpace();
        }
        return tickCompanionFlightFollow(companion, owner, teleportDistance, space);
    }

    /**
     * Owner-relative flight follow for any mob (e.g. toggle Wiggly dog).
     * Flies only while the owner is actively flying; lands when grounded.
     *
     * @return {@code true} if flight movement was applied; {@code false} if grounded follow should run.
     */
    public static boolean tickOwnedMobFlightFollow(Mob mob, Player owner) {
        return tickOwnedMobFlightFollow(
                mob,
                owner,
                CompanionFollowDistances.TELEPORT_DISTANCE,
                CompanionFollowDistances.DEFAULT_PERSONAL_SPACE,
                true);
    }

    private static boolean tickOwnedMobFlightFollow(
            Mob mob,
            Player owner,
            double teleportDistance,
            double personalSpace,
            boolean playfulBob) {
        if (owner == null || mob == null) {
            return false;
        }
        if (!isOwnerActivelyFlying(owner)) {
            landCompanionNearOwner(mob, owner);
            return false;
        }

        mob.setNoGravity(true);
        mob.getNavigation().stop();

        double space = Math.max(CompanionFollowDistances.PERSONAL_SPACE_MIN, personalSpace);
        double preferred = CompanionFollowDistances.preferredDistance(space);
        double dist = mob.distanceTo(owner);
        if (CompanionFlightFollowSupport.shouldFlightSnap(dist, teleportDistance)) {
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
            double bob = playfulBob ? WigglyDogFlightSupport.bobDeltaY(mob.tickCount) : 0.0d;
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
        double bob = playfulBob ? WigglyDogFlightSupport.bobDeltaY(mob.tickCount) : 0.0d;
        mob.setDeltaMovement(vel[0], vel[1] + bob, vel[2]);
        mob.hasImpulse = true;
        mob.getLookControl().setLookAt(owner, 10.0f, mob.getMaxHeadXRot());
        return true;
    }

    private static void landCompanionNearOwner(Mob companion, Player owner) {
        if (companion.isNoGravity()) {
            companion.setNoGravity(false);
        }
        // Cancel leftover hover velocity. Never short-range teleport — that snaps wander/home-idle.
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

    private static void clearGlow(LivingEntity entity) {
        if (entity != null && entity.hasEffect(MobEffects.GLOWING)) {
            entity.removeEffect(MobEffects.GLOWING);
        }
    }
}
