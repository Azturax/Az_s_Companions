package com.koncompanions.entity.ai;

import com.koncompanions.entity.CompanionEntity;
import com.koncompanions.entity.CompanionMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public final class CompanionGuardGoal extends Goal {
    private final CompanionEntity companion;

    public CompanionGuardGoal(CompanionEntity companion) {
        this.companion = companion;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return companion.getMode() == CompanionMode.GUARD && companion.getGuardCenter() != null;
    }

    @Override
    public void tick() {
        BlockPos center = companion.getGuardCenter();
        if (center == null) {
            return;
        }
        if (companion.blockPosition().distSqr(center) > (double) companion.getGuardRadius() * companion.getGuardRadius()) {
            companion.getNavigation().moveTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 1.0d);
        }
    }
}
