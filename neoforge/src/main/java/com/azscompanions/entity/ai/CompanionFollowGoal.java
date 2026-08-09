package com.azscompanions.entity.ai;

import com.azscompanions.config.CommonConfig;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionFollowDistances;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.perk.SpecialPlayerPerks;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Loose ground follow while the owner is exploring. Only closes the gap beyond
 * {@link CompanionFollowDistances#FOLLOW_START}; stops in the preferred ring and
 * never bee-lines onto the player. Teleport is a last resort beyond the leash —
 * never while attacking, and never while the owner is standing around.
 */
public final class CompanionFollowGoal extends Goal {
    public static final double FOLLOW_START_DISTANCE = CompanionFollowDistances.FOLLOW_START;
    public static final double FOLLOW_STOP_DISTANCE = CompanionFollowDistances.FOLLOW_STOP;

    private final CompanionEntity companion;
    private Player owner;
    private int timeToRecalcPath;

    public CompanionFollowGoal(CompanionEntity companion) {
        this.companion = companion;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (companion.getMode() != CompanionMode.FOLLOW || companion.isSitting() || companion.isSleeping()) {
            return false;
        }
        if (companion.getTarget() != null && companion.getTarget().isAlive()) {
            return false;
        }
        owner = companion.getOwner();
        if (owner == null || owner.isSpectator() || owner.isSleeping()) {
            return false;
        }
        if (SpecialPlayerPerks.isSpecial(owner) && SpecialPlayerPerks.isOwnerActivelyFlying(owner)) {
            return true;
        }
        if (companion.isOwnerStandingAround()) {
            return false;
        }
        double dist = companion.distanceTo(owner);
        // Inside personal space: briefly engage to step back.
        if (CompanionFollowDistances.tooClose(dist)) {
            return true;
        }
        return CompanionFollowDistances.needsFollow(dist);
    }

    @Override
    public boolean canContinueToUse() {
        if (owner == null
                || companion.getMode() != CompanionMode.FOLLOW
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
    public void start() {
        timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        owner = null;
        companion.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (owner == null) {
            return;
        }
        if (companion.getTarget() != null && companion.getTarget().isAlive()) {
            return;
        }
        if (SpecialPlayerPerks.tickCompanionFlightFollow(
                companion, owner, CommonConfig.TELEPORT_DISTANCE.get())) {
            return;
        }
        companion.getLookControl().setLookAt(owner, 10.0f, companion.getMaxHeadXRot());
        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = 10;
            double dist = companion.distanceTo(owner);
            if (CompanionFollowDistances.tooClose(dist)) {
                pathAwayFromOwner(CompanionFollowDistances.PREFERRED_DISTANCE);
                return;
            }
            if (dist > CommonConfig.TELEPORT_DISTANCE.get() && companion.isOwnerExploring()) {
                companion.safeTeleportNear(owner.blockPosition());
            } else if (dist > FOLLOW_STOP_DISTANCE) {
                // Path to a point in the preferred ring — never onto the owner's feet.
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
}
