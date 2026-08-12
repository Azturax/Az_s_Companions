package com.azscompanions.entity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Creeper;

/**
 * Injects vanilla-style cat scare: creepers flee companions in {@link CompanionForm#CAT}.
 */
public final class FabricCreeperCatScareEvents {
    private FabricCreeperCatScareEvents() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> tryAttach(entity));
    }

    static void tryAttach(Entity entity) {
        if (!(entity instanceof Creeper creeper) || creeper.level().isClientSide) {
            return;
        }
        if (alreadyAttached(creeper)) {
            return;
        }
        creeper.goalSelector.addGoal(3, new FabricCompanionCatScareAvoidGoal(creeper));
    }

    private static boolean alreadyAttached(Creeper creeper) {
        for (WrappedGoal wrapped : creeper.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof FabricCompanionCatScareAvoidGoal) {
                return true;
            }
        }
        return false;
    }

    /** Marker subclass so chunk reloads do not stack duplicate avoid goals. */
    public static final class FabricCompanionCatScareAvoidGoal extends AvoidEntityGoal<FabricCompanionEntity> {
        public FabricCompanionCatScareAvoidGoal(Creeper creeper) {
            super(
                    creeper,
                    FabricCompanionEntity.class,
                    c -> c instanceof FabricCompanionEntity companion
                            && CompanionMobBehaviorSupport.formScaresCreepers(companion.getForm()),
                    CompanionMobBehaviorSupport.CREEPER_SCARE_DISTANCE,
                    CompanionMobBehaviorSupport.CREEPER_WALK_SPEED,
                    CompanionMobBehaviorSupport.CREEPER_SPRINT_SPEED,
                    living -> true);
        }
    }
}
