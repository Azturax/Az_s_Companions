package com.azscompanions.entity;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Stops navigation while in SIT or STAY (CCI / commands). */
public final class FabricSitGoal extends Goal {
    private final FabricCompanionEntity companion;

    public FabricSitGoal(FabricCompanionEntity companion) {
        this.companion = companion;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        FabricCompanionMode mode = companion.getMode();
        return mode == FabricCompanionMode.SIT || mode == FabricCompanionMode.STAY;
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
