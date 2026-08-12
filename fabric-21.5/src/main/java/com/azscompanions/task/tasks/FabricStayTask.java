package com.azscompanions.task.tasks;

import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricCompanionMode;
import com.azscompanions.task.FabricCompanionTask;
import net.minecraft.server.level.ServerLevel;

public final class FabricStayTask extends FabricCompanionTask {
    public FabricStayTask() {
        super("stay");
    }

    @Override
    public void start(FabricCompanionEntity companion, ServerLevel level) {
        companion.setMode(FabricCompanionMode.STAY);
        companion.getNavigation().stop();
    }

    @Override
    public Result tick(FabricCompanionEntity companion, ServerLevel level) {
        companion.getNavigation().stop();
        return Result.COMPLETED;
    }
}
