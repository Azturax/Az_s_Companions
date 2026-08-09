package com.koncompanions.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public final class FabricFollowOwnerGoal extends Goal {
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
        owner = companion.getOwner();
        return owner != null && !owner.isSleeping() && companion.distanceTo(owner) > 3.0d;
    }

    @Override
    public boolean canContinueToUse() {
        return owner != null
                && companion.getMode() == FabricCompanionMode.FOLLOW
                && !companion.isSleeping()
                && !owner.isSleeping()
                && companion.distanceTo(owner) > 2.0d;
    }

    @Override
    public void tick() {
        if (owner == null) {
            return;
        }
        companion.getLookControl().setLookAt(owner, 10.0f, companion.getMaxHeadXRot());
        if (--recalc <= 0) {
            recalc = 10;
            companion.getNavigation().moveTo(owner, 1.1d);
        }
    }
}
