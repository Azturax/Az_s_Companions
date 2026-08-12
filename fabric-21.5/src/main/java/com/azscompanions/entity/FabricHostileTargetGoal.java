package com.azscompanions.entity;

import com.azscompanions.config.FabricServerConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

/**
 * Hostile attitude and/or team-rival companions aggro valid prey.
 */
public final class FabricHostileTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {
    private final FabricCompanionEntity companion;

    public FabricHostileTargetGoal(FabricCompanionEntity companion) {
        super(companion, LivingEntity.class, 10, true, false, (target, level) -> companion.isValidHostilePrey(target));
        this.companion = companion;
    }

    @Override
    public boolean canUse() {
        if (!FabricServerConfig.ALLOW_COMBAT) {
            return false;
        }
        if (!companion.wantsAggressiveTargets()) {
            return false;
        }
        if (companion.isSleeping()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (!companion.wantsAggressiveTargets()) {
            return false;
        }
        return super.canContinueToUse();
    }
}
