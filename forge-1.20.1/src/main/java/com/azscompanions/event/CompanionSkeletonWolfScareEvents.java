package com.azscompanions.event;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionMobBehaviorSupport;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

/**
 * Injects wolf/dog scare: skeletons (and variants) flee companions in wolf form.
 */
public final class CompanionSkeletonWolfScareEvents {
    private CompanionSkeletonWolfScareEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof AbstractSkeleton skeleton)) {
            return;
        }
        if (alreadyAttached(skeleton)) {
            return;
        }
        skeleton.goalSelector.addGoal(3, new CompanionWolfScareAvoidGoal(skeleton));
    }

    private static boolean alreadyAttached(AbstractSkeleton skeleton) {
        for (WrappedGoal wrapped : skeleton.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof CompanionWolfScareAvoidGoal) {
                return true;
            }
        }
        return false;
    }

    public static final class CompanionWolfScareAvoidGoal extends AvoidEntityGoal<CompanionEntity> {
        public CompanionWolfScareAvoidGoal(AbstractSkeleton skeleton) {
            super(
                    skeleton,
                    CompanionEntity.class,
                    c -> c instanceof CompanionEntity companion
                            && CompanionMobBehaviorSupport.formScaresSkeletons(companion.getForm()),
                    CompanionMobBehaviorSupport.SKELETON_SCARE_DISTANCE,
                    CompanionMobBehaviorSupport.SKELETON_WALK_SPEED,
                    CompanionMobBehaviorSupport.SKELETON_SPRINT_SPEED,
                    living -> true);
        }
    }
}
