package com.azscompanions.task.tasks;

import com.azscompanions.config.CommonConfig;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.task.CompanionTask;
import com.azscompanions.task.TaskPriority;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public final class FollowOwnerTask extends CompanionTask {
    public FollowOwnerTask() {
        super("follow", TaskPriority.NORMAL);
    }

    @Override
    protected void onStart(CompanionEntity companion, ServerLevel level) {
        companion.setMode(CompanionMode.FOLLOW);
    }

    @Override
    protected TaskTickResult onTick(CompanionEntity companion, ServerLevel level) {
        Player owner = companion.getOwner();
        if (owner == null) {
            return TaskTickResult.FAILED;
        }
        if (companion.getTarget() != null && companion.getTarget().isAlive()) {
            return TaskTickResult.RUNNING;
        }
        double dist = companion.distanceTo(owner);
        if (dist > CommonConfig.TELEPORT_DISTANCE.get()) {
            companion.safeTeleportNear(owner.blockPosition());
        } else if (dist > 4.0d) {
            companion.getNavigation().moveTo(owner, 1.1d);
        }
        setProgress(100);
        // Follow is a persistent mode task — complete once navigation is healthy.
        return dist <= 4.5d ? TaskTickResult.COMPLETED : TaskTickResult.RUNNING;
    }
}
