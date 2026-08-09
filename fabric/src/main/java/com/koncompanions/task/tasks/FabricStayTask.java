package com.koncompanions.task.tasks;

import com.koncompanions.entity.FabricCompanionEntity;
import com.koncompanions.entity.FabricCompanionMode;
import com.koncompanions.task.FabricCompanionTask;
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
