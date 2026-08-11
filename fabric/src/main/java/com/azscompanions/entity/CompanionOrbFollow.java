package com.azscompanions.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Always-air follow for {@link CompanionForm#GLOWING_ORB} using personal space + orb X/Y/Z offsets.
 */
public final class CompanionOrbFollow {
    private CompanionOrbFollow() {
    }

    public static boolean isOrb(Mob companion) {
        if (companion instanceof FabricCompanionEntity c) {
            return c.getForm().isOrb();
        }
        return false;
    }

    /**
     * @return {@code true} if this tick fully handled movement (orb form)
     */
    public static boolean tick(FabricCompanionEntity companion, Player owner) {
        if (companion == null || owner == null || !companion.getForm().isOrb()) {
            return false;
        }

        companion.setNoGravity(true);

        if (companion.getMode() == FabricCompanionMode.STAY
                || companion.getMode() == FabricCompanionMode.SIT) {
            Vec3 motion = companion.getDeltaMovement().scale(0.55d);
            double bob = CompanionOrbSettings.bobDeltaY(
                    companion.tickCount, 0.0f, companion.getOrbFloatAmplitude(), companion.getOrbFloatSpeed());
            companion.setDeltaMovement(motion.x, bob * 0.25d, motion.z);
            companion.hasImpulse = true;
            companion.getNavigation().stop();
            return true;
        }

        companion.getNavigation().stop();

        double personalSpace = companion.getPersonalSpace();
        double teleportLeash = companion.getFollowRadius();
        double dist = companion.distanceTo(owner);
        if (CompanionFlightFollowSupport.shouldFlightSnap(dist, teleportLeash)) {
            double[] target = CompanionOrbFlightSupport.preferredTarget(
                    owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(),
                    companion.getX(), companion.getZ(),
                    personalSpace,
                    companion.getOrbFloatHeight(),
                    companion.getOrbOffsetX(), companion.getOrbOffsetY(), companion.getOrbOffsetZ());
            companion.teleportTo(target[0], target[1], target[2]);
            companion.setDeltaMovement(Vec3.ZERO);
            companion.hasImpulse = true;
            companion.getLookControl().setLookAt(owner, 10.0f, companion.getMaxHeadXRot());
            return true;
        }

        double[] target = CompanionOrbFlightSupport.preferredTarget(
                owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(),
                companion.getX(), companion.getZ(),
                personalSpace,
                companion.getOrbFloatHeight(),
                companion.getOrbOffsetX(), companion.getOrbOffsetY(), companion.getOrbOffsetZ());
        double[] vel = CompanionOrbFlightSupport.velocityWithBob(
                companion.getX(), companion.getY(), companion.getZ(),
                target[0], target[1], target[2],
                companion.tickCount,
                companion.getOrbFloatAmplitude(),
                companion.getOrbFloatSpeed());
        companion.setDeltaMovement(vel[0], vel[1], vel[2]);
        companion.hasImpulse = true;
        companion.getLookControl().setLookAt(owner, 10.0f, companion.getMaxHeadXRot());
        return true;
    }
}
