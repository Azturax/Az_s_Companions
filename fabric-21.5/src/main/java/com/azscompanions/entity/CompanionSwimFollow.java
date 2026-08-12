package com.azscompanions.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * When the owner is swimming / in water, owned companions that are following swim with them
 * instead of floating at the surface or pathing around the shore.
 */
public final class CompanionSwimFollow {
    private CompanionSwimFollow() {
    }

    public static boolean isOwnerInWater(Player owner) {
        return owner != null
                && (owner.isSwimming() || owner.isUnderWater() || owner.isInWater() || owner.isInWater());
    }

    public static boolean isCompanionInWater(Mob companion) {
        return companion.isUnderWater() || companion.isInWater() || companion.isInWater();
    }

    /** Keep follow goals active while the owner is wet and the companion is still ashore. */
    public static boolean shouldKeepFollowing(Player owner, Mob companion) {
        return CompanionSwimFollowSupport.keepGoalWhileOwnerWet(isOwnerInWater(owner), isCompanionInWater(companion));
    }

    /**
     * @return {@code true} if this tick fully handled movement (both wet); otherwise
     *         ground pathfinding should continue (with water-friendly malus).
     */
    public static boolean tick(Mob companion, Player owner, double personalSpace) {
        if (!isOwnerInWater(owner)) {
            if (companion.isSwimming() && !isCompanionInWater(companion)) {
                companion.setSwimming(false);
            }
            return false;
        }
        if (!isCompanionInWater(companion)) {
            return false;
        }

        companion.getNavigation().stop();
        companion.setSwimming(true);

        double preferred = CompanionFollowDistances.preferredDistance(personalSpace);
        double[] target = CompanionSwimFollowSupport.preferredSwimTarget(
                owner.getX(), owner.getY(), owner.getZ(),
                companion.getX(), companion.getZ(),
                preferred);

        double speed = CompanionSwimFollowSupport.speedForDistance(
                companion.position().distanceTo(new Vec3(target[0], target[1], target[2])));
        double[] vel = CompanionSwimFollowSupport.velocityToward(
                companion.getX(), companion.getY(), companion.getZ(),
                target[0], target[1], target[2],
                speed);
        companion.setDeltaMovement(vel[0], vel[1], vel[2]);
        companion.hasImpulse = true;
        companion.getLookControl().setLookAt(owner, 10.0f, companion.getMaxHeadXRot());
        return true;
    }
}
