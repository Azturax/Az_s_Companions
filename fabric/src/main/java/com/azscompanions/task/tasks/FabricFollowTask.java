package com.azscompanions.task.tasks;

import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricCompanionMode;
import com.azscompanions.entity.FabricFollowOwnerGoal;
import com.azscompanions.task.FabricCompanionTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public final class FabricFollowTask extends FabricCompanionTask {
    public FabricFollowTask() {
        super("follow");
    }

    @Override
    public void start(FabricCompanionEntity companion, ServerLevel level) {
        companion.setMode(FabricCompanionMode.FOLLOW);
    }

    @Override
    public Result tick(FabricCompanionEntity companion, ServerLevel level) {
        Player owner = companion.getOwner();
        if (owner == null) {
            return Result.FAILED;
        }
        if (companion.getTarget() != null && companion.getTarget().isAlive()) {
            return Result.RUNNING;
        }
        double dist = companion.distanceTo(owner);
        if (dist > FabricFollowOwnerGoal.TELEPORT_DISTANCE && companion.isOwnerExploring()) {
            companion.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            return Result.RUNNING;
        }
        if (dist > 4.0d && companion.isOwnerExploring()) {
            companion.getNavigation().moveTo(owner, 1.1d);
            return Result.RUNNING;
        }
        return Result.COMPLETED;
    }
}
