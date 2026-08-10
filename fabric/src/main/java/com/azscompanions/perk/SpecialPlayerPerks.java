package com.azscompanions.perk;

import com.azscompanions.AzsCompanionsConstants;
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
    /** Max distance from owner while both are flying before a snap teleport. */
    private static final double FLIGHT_KEEP_RADIUS = 5.0d;
    /** Soft hover offset above the owner's feet while flying. */
    private static final double FLIGHT_HOVER_Y = 0.35d;
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
        }
    }

    public static void applyCompanionPerks(Mob companion, UUID ownerUuid) {
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
     * Special-UUID flying companion follow.
     *
     * @return {@code true} if this method fully handled movement (owner flying);
     *         {@code false} when the owner is grounded so normal ground follow should run.
     */
    public static boolean tickCompanionFlightFollow(Mob companion, Player owner, double teleportDistance) {
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

        double dist = companion.distanceTo(owner);
        if (dist > FLIGHT_KEEP_RADIUS) {
            snapBesideOwner(companion, owner, Math.min(2.0d, FLIGHT_KEEP_RADIUS * 0.4d));
            return true;
        }

        Vec3 target = owner.position().add(0.0d, FLIGHT_HOVER_Y, 0.0d);
        Vec3 delta = target.subtract(companion.position());
        double len = delta.length();
        if (len < 0.45d) {
            Vec3 motion = companion.getDeltaMovement().scale(0.55d);
            double yDelta = target.y - companion.getY();
            if (Math.abs(yDelta) > 0.2d) {
                motion = new Vec3(motion.x, clamp(yDelta * 0.22d, -0.4d, 0.4d), motion.z);
            }
            companion.setDeltaMovement(motion);
            companion.hasImpulse = true;
            companion.getLookControl().setLookAt(owner, 10.0f, companion.getMaxHeadXRot());
            return true;
        }

        double speed = dist > 3.0d ? 0.72d : (dist > 1.5d ? 0.52d : 0.38d);
        companion.setDeltaMovement(delta.scale(1.0d / len).scale(speed));
        companion.hasImpulse = true;
        companion.getLookControl().setLookAt(owner, 10.0f, companion.getMaxHeadXRot());
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
        if (dist >= MIN_SAFE_LAND_TELEPORT && (floatingAbove || dist > FLIGHT_KEEP_RADIUS * 3.0d)) {
            companion.teleportTo(owner.getX() + 0.5d, owner.getY(), owner.getZ() + 0.5d);
            companion.setDeltaMovement(Vec3.ZERO);
            companion.getNavigation().stop();
            companion.hasImpulse = true;
        } else {
            Vec3 motion = companion.getDeltaMovement();
            if (Math.abs(motion.y) > 0.05d && motion.y > 0.0d) {
                companion.setDeltaMovement(motion.x * 0.6d, Math.min(motion.y, 0.0d), motion.z * 0.6d);
                companion.hasImpulse = true;
            }
        }
    }

    private static void snapBesideOwner(Mob companion, Player owner, double sideOffset) {
        if (companion.distanceTo(owner) < 2.0d) {
            return;
        }
        Vec3 away = companion.position().subtract(owner.position());
        if (away.horizontalDistanceSqr() < 1.0e-4d) {
            away = new Vec3(0.5d, 0.0d, 0.5d);
        }
        Vec3 offset = new Vec3(away.x, 0.0d, away.z).normalize().scale(sideOffset);
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

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
