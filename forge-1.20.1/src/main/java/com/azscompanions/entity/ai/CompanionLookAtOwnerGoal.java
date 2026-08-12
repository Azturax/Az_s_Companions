package com.azscompanions.entity.ai;

import com.azscompanions.entity.CompanionEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public final class CompanionLookAtOwnerGoal extends Goal {
    private final CompanionEntity companion;

    public CompanionLookAtOwnerGoal(CompanionEntity companion) {
        this.companion = companion;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        Player owner = companion.getOwner();
        return owner != null && companion.distanceTo(owner) < 10.0d;
    }

    @Override
    public void tick() {
        Player owner = companion.getOwner();
        if (owner != null) {
            companion.getLookControl().setLookAt(owner, 10.0f, companion.getMaxHeadXRot());
        }
    }
}
