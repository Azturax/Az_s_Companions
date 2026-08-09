package com.koncompanions.task.tasks;

import com.koncompanions.entity.FabricCompanionEntity;
import com.koncompanions.entity.FabricCompanionMode;
import com.koncompanions.task.FabricCompanionTask;
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
        if (companion.distanceTo(owner) > 4.0d) {
            companion.getNavigation().moveTo(owner, 1.1d);
            return Result.RUNNING;
        }
        return Result.COMPLETED;
    }
}
