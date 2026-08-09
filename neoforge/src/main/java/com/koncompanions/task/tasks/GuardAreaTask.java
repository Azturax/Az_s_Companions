package com.koncompanions.task.tasks;

import com.koncompanions.config.ServerConfig;
import com.koncompanions.entity.CompanionEntity;
import com.koncompanions.entity.CompanionMode;
import com.koncompanions.task.CompanionTask;
import com.koncompanions.task.TaskPriority;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

public final class GuardAreaTask extends CompanionTask {
    public GuardAreaTask() {
        super("guard", TaskPriority.HIGH);
    }

    @Override
    protected void onStart(CompanionEntity companion, ServerLevel level) {
        companion.setMode(CompanionMode.GUARD);
        if (companion.getGuardCenter() == null) {
            companion.setGuardCenter(companion.blockPosition(), companion.getGuardRadius());
        }
    }

    @Override
    protected TaskTickResult onTick(CompanionEntity companion, ServerLevel level) {
        if (!ServerConfig.ALLOW_COMBAT.get() || !companion.hasPermission("combat")) {
            return failAndStop("combat_disabled");
        }
        var center = companion.getGuardCenter();
        if (center == null) {
            return failAndStop("no_guard_center");
        }
        double radius = companion.getGuardRadius();
        AABB box = new AABB(center).inflate(radius);
        LivingEntity target = level.getNearestEntity(
                Monster.class,
                TargetingConditions.forCombat(),
                companion,
                companion.getX(), companion.getY(), companion.getZ(),
                box
        );
        if (target != null && companion.canAttackTarget(target)) {
            companion.getNavigation().moveTo(target, 1.2d);
            if (companion.distanceTo(target) < 2.5d) {
                companion.doHurtTarget(target);
            }
            setProgress(50);
            return TaskTickResult.RUNNING;
        }
        if (companion.blockPosition().distSqr(center) > radius * radius) {
            companion.getNavigation().moveTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 1.0d);
        }
        setProgress(100);
        return TaskTickResult.RUNNING;
    }

    private TaskTickResult failAndStop(String reason) {
        fail(reason);
        return TaskTickResult.FAILED;
    }
}
