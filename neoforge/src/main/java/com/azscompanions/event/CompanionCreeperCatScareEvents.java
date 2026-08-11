package com.azscompanions.event;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionMobBehaviorSupport;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Injects vanilla-style cat scare: creepers flee companions in cat form.
 */
public final class CompanionCreeperCatScareEvents {
    private CompanionCreeperCatScareEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Creeper creeper)) {
            return;
        }
        if (alreadyAttached(creeper)) {
            return;
        }
        creeper.goalSelector.addGoal(3, new CompanionCatScareAvoidGoal(creeper));
    }

    private static boolean alreadyAttached(Creeper creeper) {
        for (WrappedGoal wrapped : creeper.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof CompanionCatScareAvoidGoal) {
                return true;
            }
        }
        return false;
    }

    public static final class CompanionCatScareAvoidGoal extends AvoidEntityGoal<CompanionEntity> {
        public CompanionCatScareAvoidGoal(Creeper creeper) {
            super(
                    creeper,
                    CompanionEntity.class,
                    c -> c instanceof CompanionEntity companion
                            && CompanionMobBehaviorSupport.formScaresCreepers(companion.getForm()),
                    CompanionMobBehaviorSupport.CREEPER_SCARE_DISTANCE,
                    CompanionMobBehaviorSupport.CREEPER_WALK_SPEED,
                    CompanionMobBehaviorSupport.CREEPER_SPRINT_SPEED,
                    living -> true);
        }
    }
}
