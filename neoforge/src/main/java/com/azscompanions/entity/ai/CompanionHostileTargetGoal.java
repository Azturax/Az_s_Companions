package com.azscompanions.entity.ai;

import com.azscompanions.config.ServerConfig;
import com.azscompanions.entity.CompanionEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

/**
 * Hostile attitude and/or team-rival companions aggro valid prey.
 */
public final class CompanionHostileTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {
    private final CompanionEntity companion;

    public CompanionHostileTargetGoal(CompanionEntity companion) {
        super(companion, LivingEntity.class, 10, true, false, companion::isValidHostilePrey);
        this.companion = companion;
    }

    @Override
    public boolean canUse() {
        if (!ServerConfig.ALLOW_COMBAT.get() || !companion.hasPermission("combat")) {
            return false;
        }
        if (!companion.wantsAggressiveTargets()) {
            return false;
        }
        if (companion.isSitting() || companion.isSleeping()) {
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
