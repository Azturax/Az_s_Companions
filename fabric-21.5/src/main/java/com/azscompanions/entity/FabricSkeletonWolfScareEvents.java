package com.azscompanions.entity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;

/**
 * Injects wolf/dog scare: skeletons (and variants) flee companions in {@link CompanionForm#WOLF}.
 */
public final class FabricSkeletonWolfScareEvents {
    private FabricSkeletonWolfScareEvents() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> tryAttach(entity));
    }

    static void tryAttach(Entity entity) {
        if (!(entity instanceof AbstractSkeleton skeleton) || skeleton.level().isClientSide) {
            return;
        }
        if (alreadyAttached(skeleton)) {
            return;
        }
        skeleton.goalSelector.addGoal(3, new FabricCompanionWolfScareAvoidGoal(skeleton));
    }

    private static boolean alreadyAttached(AbstractSkeleton skeleton) {
        for (WrappedGoal wrapped : skeleton.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof FabricCompanionWolfScareAvoidGoal) {
                return true;
            }
        }
        return false;
    }

    /** Marker subclass so chunk reloads do not stack duplicate avoid goals. */
    public static final class FabricCompanionWolfScareAvoidGoal extends AvoidEntityGoal<FabricCompanionEntity> {
        public FabricCompanionWolfScareAvoidGoal(AbstractSkeleton skeleton) {
            super(
                    skeleton,
                    FabricCompanionEntity.class,
                    c -> c instanceof FabricCompanionEntity companion
                            && CompanionMobBehaviorSupport.formScaresSkeletons(companion.getForm()),
                    CompanionMobBehaviorSupport.SKELETON_SCARE_DISTANCE,
                    CompanionMobBehaviorSupport.SKELETON_WALK_SPEED,
                    CompanionMobBehaviorSupport.SKELETON_SPRINT_SPEED,
                    living -> true);
        }
    }
}
