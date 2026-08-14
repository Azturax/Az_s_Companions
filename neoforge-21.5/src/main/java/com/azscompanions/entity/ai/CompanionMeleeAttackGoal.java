package com.azscompanions.entity.ai;

import com.azscompanions.entity.CompanionEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Melee only when the companion is not actively using a bow/crossbow with ammo.
 */
public final class CompanionMeleeAttackGoal extends MeleeAttackGoal {
    private final CompanionEntity companion;

    public CompanionMeleeAttackGoal(CompanionEntity companion, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(companion, speedModifier, followingTargetEvenIfNotSeen);
        this.companion = companion;
    }

    @Override
    public boolean canUse() {
        if (companion.shouldPreferBowCombat()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (companion.shouldPreferBowCombat()) {
            return false;
        }
        return super.canContinueToUse();
    }
}