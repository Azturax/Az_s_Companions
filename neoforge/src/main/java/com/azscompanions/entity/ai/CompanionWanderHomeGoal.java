package com.azscompanions.entity.ai;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public final class CompanionWanderHomeGoal extends Goal {
    private final CompanionEntity companion;

    public CompanionWanderHomeGoal(CompanionEntity companion) {
        this.companion = companion;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return companion.getMode() == CompanionMode.HOME && companion.getHomePos() != null;
    }

    @Override
    public void tick() {
        BlockPos home = companion.getHomePos();
        if (home != null && companion.blockPosition().distManhattan(home) > 2) {
            companion.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0d);
        }
    }
}
