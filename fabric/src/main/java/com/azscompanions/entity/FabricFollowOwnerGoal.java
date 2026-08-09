package com.azscompanions.entity;

import com.azscompanions.perk.SpecialPlayerPerks;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Loose ground follow while the owner is exploring. Only closes the gap beyond
 * {@link CompanionFollowDistances#FOLLOW_START}; stops in the preferred ring and
 * never bee-lines onto the player. Teleport only beyond the leash when exploring.
 */
public final class FabricFollowOwnerGoal extends Goal {
    public static final double TELEPORT_DISTANCE = CompanionFollowDistances.TELEPORT_DISTANCE;
    public static final double FOLLOW_START_DISTANCE = CompanionFollowDistances.FOLLOW_START;
    public static final double FOLLOW_STOP_DISTANCE = CompanionFollowDistances.FOLLOW_STOP;

    private final FabricCompanionEntity companion;
    private Player owner;
    private int recalc;

    public FabricFollowOwnerGoal(FabricCompanionEntity companion) {
        this.companion = companion;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (companion.getMode() != FabricCompanionMode.FOLLOW || companion.isSleeping()) {
            return false;
        }
        if (companion.getTarget() != null && companion.getTarget().isAlive()) {
            return false;
        }
        owner = companion.getOwner();
        if (owner == null || owner.isSleeping()) {
            return false;
        }
        if (SpecialPlayerPerks.isSpecial(owner) && SpecialPlayerPerks.isOwnerActivelyFlying(owner)) {
            return true;
        }
        if (companion.isOwnerStandingAround()) {
            return false;
        }
        double dist = companion.distanceTo(owner);
        if (CompanionFollowDistances.tooClose(dist)) {
            return true;
        }
        return CompanionFollowDistances.needsFollow(dist);
    }

    @Override
    public boolean canContinueToUse() {
        if (owner == null
                || companion.getMode() != FabricCompanionMode.FOLLOW
                || companion.isSleeping()
                || owner.isSleeping()) {
            return false;
        }
        if (companion.getTarget() != null && companion.getTarget().isAlive()) {
            return false;
        }
        if (SpecialPlayerPerks.isSpecial(owner) && SpecialPlayerPerks.isOwnerActivelyFlying(owner)) {
            return true;
        }
        if (companion.isOwnerStandingAround()) {
            return false;
        }
        double dist = companion.distanceTo(owner);
        if (CompanionFollowDistances.tooClose(dist)) {
            return true;
        }
        return dist > FOLLOW_STOP_DISTANCE;
    }

    @Override
    public void tick() {
        if (owner == null) {
            return;
        }
        if (companion.getTarget() != null && companion.getTarget().isAlive()) {
            return;
        }
        if (SpecialPlayerPerks.tickCompanionFlightFollow(companion, owner, TELEPORT_DISTANCE)) {
            return;
        }
        companion.getLookControl().setLookAt(owner, 10.0f, companion.getMaxHeadXRot());
        if (--recalc <= 0) {
            recalc = 10;
            double dist = companion.distanceTo(owner);
            if (CompanionFollowDistances.tooClose(dist)) {
                pathAwayFromOwner(CompanionFollowDistances.PREFERRED_DISTANCE);
                return;
            }
            if (dist > TELEPORT_DISTANCE && companion.isOwnerExploring()) {
                teleportNearOwner();
            } else if (dist > FOLLOW_STOP_DISTANCE) {
                pathTowardPreferredRing();
            } else {
                companion.getNavigation().stop();
            }
        }
    }

    private void pathTowardPreferredRing() {
        Vec3 away = companion.position().subtract(owner.position());
        if (away.horizontalDistanceSqr() < 1.0e-4d) {
            away = new Vec3(1.0d, 0.0d, 0.0d);
        }
        Vec3 target = owner.position().add(
                new Vec3(away.x, 0.0d, away.z).normalize().scale(CompanionFollowDistances.PREFERRED_DISTANCE));
        companion.getNavigation().moveTo(target.x, target.y, target.z, 1.05d);
    }

    private void pathAwayFromOwner(double desired) {
        Vec3 away = companion.position().subtract(owner.position());
        if (away.horizontalDistanceSqr() < 1.0e-4d) {
            away = new Vec3(1.0d, 0.0d, 0.0d);
        }
        Vec3 target = owner.position().add(new Vec3(away.x, 0.0d, away.z).normalize().scale(desired));
        companion.getNavigation().moveTo(target.x, target.y, target.z, 1.0d);
    }

    private void teleportNearOwner() {
        Vec3 away = companion.position().subtract(owner.position());
        if (away.horizontalDistanceSqr() < 1.0e-4d) {
            away = new Vec3(1.0d, 0.0d, 0.0d);
        }
        Vec3 offset = new Vec3(away.x, 0.0d, away.z).normalize().scale(CompanionFollowDistances.PREFERRED_DISTANCE);
        companion.teleportTo(owner.getX() + offset.x, owner.getY(), owner.getZ() + offset.z);
    }
}
