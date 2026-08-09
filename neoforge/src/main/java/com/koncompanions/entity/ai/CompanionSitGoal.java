package com.koncompanions.entity.ai;

import com.koncompanions.entity.CompanionEntity;
import com.koncompanions.entity.CompanionMode;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public final class CompanionSitGoal extends Goal {
    private final CompanionEntity companion;

    public CompanionSitGoal(CompanionEntity companion) {
        this.companion = companion;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return companion.isSitting() || companion.getMode() == CompanionMode.SIT || companion.getMode() == CompanionMode.STAY;
    }

    @Override
    public void start() {
        companion.getNavigation().stop();
    }

    @Override
    public void tick() {
        companion.getNavigation().stop();
    }
}
