package com.azscompanions.entity;

import net.minecraft.world.InteractionHand;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.BowItem;

import java.util.EnumSet;

/**
 * Bow attack for companions (Fabric vanilla {@code RangedBowAttackGoal} is Monster-bound).
 */
public final class FabricCompanionBowAttackGoal extends Goal {
    private final FabricCompanionEntity companion;
    private final double speedModifier;
    private final int attackIntervalMin;
    private final float attackRadiusSqr;
    private int attackTime = -1;
    private int seeTime;
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;

    public FabricCompanionBowAttackGoal(FabricCompanionEntity companion, double speedModifier, int attackIntervalMin, float attackRadius) {
        this.companion = companion;
        this.speedModifier = speedModifier;
        this.attackIntervalMin = attackIntervalMin;
        this.attackRadiusSqr = attackRadius * attackRadius;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return companion.getTarget() != null && companion.shouldPreferBowCombat();
    }

    @Override
    public boolean canContinueToUse() {
        return (canUse() || !companion.getNavigation().isDone()) && companion.shouldPreferBowCombat();
    }

    @Override
    public void start() {
        super.start();
        companion.setAggressive(true);
    }

    @Override
    public void stop() {
        super.stop();
        companion.setAggressive(false);
        seeTime = 0;
        attackTime = -1;
        companion.stopUsingItem();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = companion.getTarget();
        if (target == null) {
            return;
        }
        double distSqr = companion.distanceToSqr(target.getX(), target.getY(), target.getZ());
        boolean canSee = companion.getSensing().hasLineOfSight(target);
        if (canSee) {
            seeTime++;
        } else {
            seeTime = 0;
        }

        if (distSqr <= attackRadiusSqr && seeTime >= 5) {
            companion.getNavigation().stop();
            strafingTime++;
        } else {
            companion.getNavigation().moveTo(target, speedModifier);
            strafingTime = -1;
        }

        if (strafingTime >= 20) {
            if (companion.getRandom().nextFloat() < 0.3f) {
                strafingClockwise = !strafingClockwise;
            }
            if (companion.getRandom().nextFloat() < 0.3f) {
                strafingBackwards = !strafingBackwards;
            }
            strafingTime = 0;
        }

        if (strafingTime > -1) {
            if (distSqr > attackRadiusSqr * 0.75f) {
                strafingBackwards = false;
            } else if (distSqr < attackRadiusSqr * 0.25f) {
                strafingBackwards = true;
            }
            companion.getMoveControl().strafe(strafingBackwards ? -0.5f : 0.5f, strafingClockwise ? 0.5f : -0.5f);
            companion.lookAt(target, 30.0f, 30.0f);
        } else {
            companion.getLookControl().setLookAt(target, 30.0f, 30.0f);
        }

        if (companion.isUsingItem()) {
            if (!canSee && seeTime < -60) {
                companion.stopUsingItem();
            } else if (canSee) {
                int useTicks = companion.getTicksUsingItem();
                if (useTicks >= 20) {
                    companion.stopUsingItem();
                    ((RangedAttackMob) companion).performRangedAttack(target, BowItem.getPowerForTime(useTicks));
                    attackTime = attackIntervalMin;
                }
            }
        } else if (--attackTime <= 0 && seeTime >= -60) {
            InteractionHand hand = InteractionHand.MAIN_HAND;
            if (!(companion.getMainHandItem().getItem() instanceof BowItem) && companion.getOffhandItem().getItem() instanceof BowItem) {
                hand = InteractionHand.OFF_HAND;
            }
            companion.startUsingItem(hand);
        }
    }
}