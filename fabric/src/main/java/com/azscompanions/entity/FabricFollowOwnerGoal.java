package com.azscompanions.entity;

import com.azscompanions.perk.SpecialPlayerPerks;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Loose ground follow. Starts only when the owner is far, so companions can fight/act
 * nearby without bouncing back at ~10 blocks. Flight perk keeps its own 5-block leash.
 */
public final class FabricFollowOwnerGoal extends Goal {
    /** Teleport-to-owner threshold for ground follow (blocks). */
    public static final double TELEPORT_DISTANCE = 48.0d;
    /** Begin pathing back to owner only beyond this distance (blocks). */
    public static final double FOLLOW_START_DISTANCE = 32.0d;
    /** Stop pathing once within this distance (blocks). */
    public static final double FOLLOW_STOP_DISTANCE = 24.0d;

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
        return companion.distanceTo(owner) > FOLLOW_START_DISTANCE;
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
        return companion.distanceTo(owner) > FOLLOW_STOP_DISTANCE;
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
            if (dist > TELEPORT_DISTANCE) {
                companion.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            } else {
                companion.getNavigation().moveTo(owner, 1.1d);
            }
        }
    }
}
