package com.azscompanions.entity;

import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Melee only when the companion is not actively using a bow/crossbow with ammo.
 */
public final class FabricMeleeAttackGoal extends MeleeAttackGoal {
    private final FabricCompanionEntity companion;

    public FabricMeleeAttackGoal(FabricCompanionEntity companion, double speedModifier, boolean followingTargetEvenIfNotSeen) {
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
