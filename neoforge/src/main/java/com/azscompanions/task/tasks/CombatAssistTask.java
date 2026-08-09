package com.azscompanions.task.tasks;

import com.azscompanions.config.ServerConfig;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.task.CompanionTask;
import com.azscompanions.task.TaskPriority;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class CombatAssistTask extends CompanionTask {
    public CombatAssistTask() {
        super("combat", TaskPriority.HIGH);
    }

    @Override
    protected TaskTickResult onTick(CompanionEntity companion, ServerLevel level) {
        if (!ServerConfig.ALLOW_COMBAT.get() || !companion.hasPermission("combat")) {
            fail("combat_disabled");
            return TaskTickResult.FAILED;
        }
        if (companion.getHealth() / companion.getMaxHealth() <= ServerConfig.LOW_HEALTH_RETREAT_RATIO.get()) {
            Player owner = companion.getOwner();
            if (owner != null) {
                companion.getNavigation().moveTo(owner, 1.3d);
            }
            fail("retreating_low_health");
            return TaskTickResult.FAILED;
        }
        AABB box = companion.getBoundingBox().inflate(16);
        LivingEntity target = level.getNearestEntity(
                Monster.class,
                TargetingConditions.forCombat(),
                companion,
                companion.getX(), companion.getY(), companion.getZ(),
                box
        );
        if (target == null || !companion.canAttackTarget(target)) {
            return TaskTickResult.COMPLETED;
        }
        companion.getNavigation().moveTo(target, 1.25d);
        if (companion.distanceTo(target) < 2.5d) {
            companion.doHurtTarget(target);
        }
        setProgress(60);
        return target.isAlive() ? TaskTickResult.RUNNING : TaskTickResult.COMPLETED;
    }
}
