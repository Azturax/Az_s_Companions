package com.azscompanions.entity;

import com.azscompanions.perk.SpecialPlayerPerks;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Follow owner when commanded and not home-idle near the bed.
 * Home-bed rule: {@link CompanionFollowDistances#HOME_BED_RADIUS}.
 * Distances use per-companion follow radius / personal space.
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
        FabricCompanionMode mode = companion.getMode();
        if (mode == FabricCompanionMode.STAY || mode == FabricCompanionMode.SIT) {
            return false;
        }
        if (mode != FabricCompanionMode.FOLLOW && mode != FabricCompanionMode.WANDER) {
            return false;
        }
        if (mode == FabricCompanionMode.WANDER && !companion.isOwnerFarFromHomeBed()) {
            return false;
        }
        if (companion.isSleeping() || !companion.shouldActivelyFollowOwner()) {
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
                || companion.getMode() == FabricCompanionMode.STAY
                || companion.getMode() == FabricCompanionMode.SIT
                || !companion.shouldActivelyFollowOwner()) {
            return false;
        }
        if (companion.getTarget() != null && companion.getTarget().isAlive()) {
            return false;
        }
        if (SpecialPlayerPerks.isSpecial(owner) && SpecialPlayerPerks.isOwnerActivelyFlying(owner)) {
            return true;
        }
        double dist = companion.distanceTo(owner);
        if (CompanionFollowDistances.tooClose(dist, personalSpace())) {
            return true;
        }
        return dist > followStop();
    }

    @Override
    public void tick() {
        if (owner == null) {
            return;
        }
        if (companion.getTarget() != null && companion.getTarget().isAlive()) {
            return;
        }
        double teleportLeash = Math.max(TELEPORT_DISTANCE, followRadius());
        if (SpecialPlayerPerks.tickCompanionFlightFollow(companion, owner, teleportLeash)) {
            return;
        }
        companion.getLookControl().setLookAt(owner, 10.0f, companion.getMaxHeadXRot());
        if (--recalc <= 0) {
            recalc = 10;
            double dist = companion.distanceTo(owner);
            if (CompanionFollowDistances.tooClose(dist, personalSpace())) {
                pathAwayFromOwner(CompanionFollowDistances.preferredDistance(personalSpace()));
                return;
            }
            boolean mayTeleport = companion.getMode() == FabricCompanionMode.FOLLOW
                    && CompanionFollowDistances.shouldGroundTeleport(dist, followRadius())
                    && !CompanionFollowDistances.tooCloseToTeleport(dist, followRadius())
                    && companion.isOwnerExploring();
            if (mayTeleport) {
                companion.safeTeleportNearOwner(owner);
            } else if (dist > followStop()) {
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
