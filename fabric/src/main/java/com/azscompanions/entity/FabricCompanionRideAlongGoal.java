package com.azscompanions.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * When the owner mounts a rideable, path to / befriend / mount a matching empty vehicle
 * and steer it near the owner. Sync-dismounts when the owner dismounts.
 */
public final class FabricCompanionRideAlongGoal extends Goal {
    private final FabricCompanionEntity companion;
    private Player owner;
    private Entity candidate;
    private int repath;
    private long cooldownUntil;
    private boolean lightApproachDone;

    public FabricCompanionRideAlongGoal(FabricCompanionEntity companion) {
        this.companion = companion;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (companion.isSleeping()) {
            return false;
        }
        FabricCompanionMode mode = companion.getMode();
        if (mode == FabricCompanionMode.STAY || mode == FabricCompanionMode.SIT) {
            return false;
        }
        if (!companion.shouldActivelyFollowOwner()) {
            return false;
        }
        if (companion.getTarget() != null && companion.getTarget().isAlive()) {
            return false;
        }
        owner = companion.getOwner();
        if (owner == null || owner.isSpectator()) {
            return false;
        }
        if (companion.isRideAlongActive() && companion.isPassenger()) {
            return owner.isPassenger();
        }
        if (!CompanionRideAlongSupport.canAttempt(companion.level().getGameTime(), cooldownUntil)) {
            return false;
        }
        return CompanionRideAlongSupport.shouldSeek(
                true,
                CompanionRideAlong.isOwnerRidingSupported(owner),
                companion.isSleeping(),
                false);
    }

    @Override
    public boolean canContinueToUse() {
        if (owner == null || companion.isSleeping()) {
            return false;
        }
        if (companion.getMode() == FabricCompanionMode.STAY || companion.getMode() == FabricCompanionMode.SIT) {
            return false;
        }
        if (companion.getTarget() != null && companion.getTarget().isAlive()) {
            return false;
        }
        if (companion.isRideAlongActive() && companion.isPassenger()) {
            return owner.isPassenger();
        }
        return owner.isPassenger() && CompanionRideAlong.isOwnerRidingSupported(owner);
    }

    @Override
    public void start() {
        repath = 0;
        lightApproachDone = false;
        candidate = null;
    }

    @Override
    public void stop() {
        companion.getNavigation().stop();
        candidate = null;
        owner = null;
    }

    @Override
    public void tick() {
        if (owner == null) {
            return;
        }
        if (CompanionRideAlongSupport.shouldSyncDismount(companion.isRideAlongActive(), owner.isPassenger())) {
            CompanionRideAlong.stopRideAlong(companion);
            companion.setRideAlongActive(false);
            cooldownUntil = CompanionRideAlongSupport.nextFailCooldown(companion.level().getGameTime());
            return;
        }

        if (companion.isPassenger()) {
            Entity vehicle = companion.getVehicle();
            if (vehicle != null) {
                companion.setRideAlongActive(true);
                companion.getNavigation().stop();
                companion.getLookControl().setLookAt(owner, 10.0f, companion.getMaxHeadXRot());
                CompanionRideAlong.steerVehicleTowardOwner(vehicle, owner, companion.getPersonalSpace());
            }
            return;
        }

        CompanionRideAlongSupport.RideKind want = CompanionRideAlong.classify(owner.getVehicle());
        if (want == CompanionRideAlongSupport.RideKind.NONE) {
            return;
        }

        if (candidate == null || !candidate.isAlive() || candidate.getPassengers().size() > 0
                || candidate == owner.getVehicle()) {
            candidate = CompanionRideAlong.findPreferredCandidate(companion, owner, want);
        }

        if (candidate != null) {
            companion.getLookControl().setLookAt(candidate, 10.0f, companion.getMaxHeadXRot());
            if (CompanionRideAlongSupport.withinMountReach(companion.distanceToSqr(candidate))) {
                boolean mounted = CompanionRideAlong.tryBefriendAndMount(companion, owner, candidate);
                if (mounted) {
                    companion.setRideAlongActive(true);
                    companion.getNavigation().stop();
                } else {
                    cooldownUntil = CompanionRideAlongSupport.nextFailCooldown(companion.level().getGameTime());
                    candidate = null;
                }
                return;
            }
            if (--repath <= 0) {
                repath = CompanionRideAlongSupport.APPROACH_REPATH_TICKS;
                companion.getNavigation().moveTo(candidate, 1.15d);
            }
            return;
        }

        // No empty mount: light approach toward nearest matching, then cool down.
        if (!lightApproachDone) {
            Entity approach = CompanionRideAlong.findApproachCandidate(companion, owner, want);
            if (approach != null) {
                companion.getLookControl().setLookAt(approach, 10.0f, companion.getMaxHeadXRot());
                companion.getNavigation().moveTo(approach, 1.0d);
            }
            lightApproachDone = true;
            cooldownUntil = CompanionRideAlongSupport.nextFailCooldown(companion.level().getGameTime());
        }
    }
}
