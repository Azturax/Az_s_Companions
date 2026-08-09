package com.koncompanions.entity.ai;

import com.koncompanions.config.CommonConfig;
import com.koncompanions.entity.CompanionEntity;
import com.koncompanions.entity.CompanionMode;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public final class CompanionFollowGoal extends Goal {
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
        owner = companion.getOwner();
        return owner != null && !owner.isSpectator() && !owner.isSleeping() && companion.distanceTo(owner) > 3.0d;
    }

    @Override
    public boolean canContinueToUse() {
        return owner != null
                && companion.getMode() == CompanionMode.FOLLOW
                && !companion.isSleeping()
                && !owner.isSleeping()
                && companion.distanceTo(owner) > 2.0d;
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
        companion.getLookControl().setLookAt(owner, 10.0f, companion.getMaxHeadXRot());
        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = 10;
            double dist = companion.distanceTo(owner);
            if (dist > CommonConfig.TELEPORT_DISTANCE.get()) {
                companion.safeTeleportNear(owner.blockPosition());
            } else {
                companion.getNavigation().moveTo(owner, 1.1d);
            }
        }
    }
}
