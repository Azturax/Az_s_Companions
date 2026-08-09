package com.azscompanions.entity.ai;

import com.azscompanions.config.CommonConfig;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.perk.SpecialPlayerPerks;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Loose ground follow while the owner is exploring. Starts only when far so companions
 * can fight/act nearby. Teleport is a last resort beyond the config leash — never while
 * attacking, and never while the owner is standing around (idle wander instead).
 */
public final class CompanionFollowGoal extends Goal {
    /** Begin pathing back to owner only beyond this distance (blocks). */
    public static final double FOLLOW_START_DISTANCE = 32.0d;
    /** Stop pathing once within this distance (blocks). */
    public static final double FOLLOW_STOP_DISTANCE = 24.0d;

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
        // Stay engaged in combat — do not steal MOVE from MeleeAttackGoal / teleport home.
        if (companion.getTarget() != null && companion.getTarget().isAlive()) {
            return false;
        }
        owner = companion.getOwner();
        if (owner == null || owner.isSpectator() || owner.isSleeping()) {
            return false;
        }
        // While the special owner is flying, keep follow active for tight airborne leash.
        if (SpecialPlayerPerks.isSpecial(owner) && SpecialPlayerPerks.isOwnerActivelyFlying(owner)) {
            return true;
        }
        // Standing around → wander goal; do not hard-follow / teleport.
        if (companion.isOwnerStandingAround()) {
            return false;
        }
        return companion.distanceTo(owner) > FOLLOW_START_DISTANCE;
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
        return companion.distanceTo(owner) > FOLLOW_STOP_DISTANCE;
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
            // Teleport only while exploring and truly beyond the leash — never while idle.
            if (dist > CommonConfig.TELEPORT_DISTANCE.get() && companion.isOwnerExploring()) {
                companion.safeTeleportNear(owner.blockPosition());
            } else {
                companion.getNavigation().moveTo(owner, 1.1d);
            }
        }
    }
}
