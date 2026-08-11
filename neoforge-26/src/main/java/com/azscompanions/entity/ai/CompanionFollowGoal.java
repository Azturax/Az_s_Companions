package com.azscompanions.entity.ai;

import com.azscompanions.config.CommonConfig;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionFollowDistances;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.entity.CompanionSwimFollow;
import com.azscompanions.perk.SpecialPlayerPerks;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Follow owner when commanded and not home-idle near the bed.
 * <p>
 * Home-bed rule ({@link CompanionFollowDistances#HOME_BED_RADIUS}): while near the home bed and
 * the owner is also within that radius, do not glue-follow. When the owner leaves the bed radius,
 * entity tick teleports here and this goal resumes follow.
 * <p>
 * Distances use per-companion {@link CompanionEntity#getFollowRadius()} /
 * {@link CompanionEntity#getPersonalSpace()}.
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

    private double personalSpace() {
        return companion.getPersonalSpace();
    }

    private double followRadius() {
        return companion.getFollowRadius();
    }

    private double followStop() {
        return CompanionFollowDistances.followStop(personalSpace());
    }

    @Override
    public boolean canUse() {
        CompanionMode mode = companion.getMode();
        if (mode == CompanionMode.STAY || mode == CompanionMode.SIT) {
            return false;
        }
        if (mode != CompanionMode.FOLLOW && mode != CompanionMode.WANDER) {
            return false;
        }
        // Wander never uses this goal for casual stroll — only after home-bed rescue.
        if (mode == CompanionMode.WANDER && !companion.isOwnerFarFromHomeBed()) {
            return false;
        }
        if (companion.isSitting() || companion.isSleeping()) {
            return false;
        }
        if (!companion.shouldActivelyFollowOwner()) {
            return false;
        }
        if (companion.getTarget() != null && companion.getTarget().isAlive()) {
            return false;
        }
        owner = companion.getOwner();
        if (owner == null || owner.isSpectator() || owner.isSleeping()) {
            return false;
        }
        if (CompanionOrbFollow.isOrb(companion) || (SpecialPlayerPerks.isSpecial(owner) && SpecialPlayerPerks.isOwnerActivelyFlying(owner))) {
            return true;
        }
        if (CompanionSwimFollow.shouldKeepFollowing(owner, companion)
                || (CompanionSwimFollow.isOwnerInWater(owner) && CompanionSwimFollow.isCompanionInWater(companion))) {
            return true;
        }
        double dist = companion.distanceTo(owner);
        if (CompanionFollowDistances.tooClose(dist, personalSpace())) {
            return true;
        }
        return CompanionFollowDistances.needsFollow(dist, personalSpace(), followRadius())
                || companion.isOwnerFarFromHomeBed()
                || dist > followStop();
    }

    @Override
    public boolean canContinueToUse() {
        if (owner == null
                || companion.isSleeping()
                || owner.isSleeping()
                || companion.getMode() == CompanionMode.STAY
                || companion.getMode() == CompanionMode.SIT) {
            return false;
        }
        if (!companion.shouldActivelyFollowOwner()) {
            return false;
        }
        if (companion.getTarget() != null && companion.getTarget().isAlive()) {
            return false;
        }
        if (CompanionOrbFollow.isOrb(companion) || (SpecialPlayerPerks.isSpecial(owner) && SpecialPlayerPerks.isOwnerActivelyFlying(owner))) {
            return true;
        }
        if (CompanionSwimFollow.shouldKeepFollowing(owner, companion)
                || (CompanionSwimFollow.isOwnerInWater(owner) && CompanionSwimFollow.isCompanionInWater(companion))) {
            return true;
        }
        double dist = companion.distanceTo(owner);
        if (CompanionFollowDistances.tooClose(dist, personalSpace())) {
            return true;
        }
        return dist > followStop();
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
        double teleportLeash = Math.max(CommonConfig.TELEPORT_DISTANCE.get(), followRadius());
        if (CompanionOrbFollow.tick(companion, owner)) {
            return;
        }
        if (SpecialPlayerPerks.tickCompanionFlightFollow(companion, owner, teleportLeash, personalSpace())) {
            return;
        }
        if (CompanionSwimFollow.tick(companion, owner, personalSpace())) {
            return;
        }
        companion.getLookControl().setLookAt(owner, 10.0f, companion.getMaxHeadXRot());
        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = 10;
            double dist = companion.distanceTo(owner);
            if (CompanionFollowDistances.tooClose(dist, personalSpace())) {
                pathAwayFromOwner(CompanionFollowDistances.preferredDistance(personalSpace()));
                return;
            }
            // Wander: never teleport here — home-bed leash is the only Wander teleport.
            // Follow: only beyond the long ground leash, and never under min teleport floor.
            boolean mayTeleport = companion.getMode() == CompanionMode.FOLLOW
                    && CompanionFollowDistances.shouldGroundTeleport(dist, followRadius())
                    && !CompanionFollowDistances.tooCloseToTeleport(dist, followRadius())
                    && companion.isOwnerExploring();
            if (mayTeleport) {
                companion.safeTeleportNear(owner.blockPosition());
            } else if (dist > followStop() || CompanionSwimFollow.shouldKeepFollowing(owner, companion)) {
                // Owner in water: keep pathing into/toward them even inside the usual stop band.
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
                new Vec3(away.x, 0.0d, away.z).normalize()
                        .scale(CompanionFollowDistances.preferredDistance(personalSpace())));
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
