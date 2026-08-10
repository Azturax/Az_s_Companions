package com.azscompanions.task.tasks;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.task.CompanionTask;
import com.azscompanions.task.TaskPriority;
import net.minecraft.server.level.ServerLevel;

public final class StayTask extends CompanionTask {
    public StayTask() {
        super("stay", TaskPriority.HIGH);
    }

    @Override
    protected void onStart(CompanionEntity companion, ServerLevel level) {
        companion.setMode(CompanionMode.STAY);
        companion.getNavigation().stop();
    }

    @Override
    protected TaskTickResult onTick(CompanionEntity companion, ServerLevel level) {
        companion.getNavigation().stop();
        setProgress(100);
        return TaskTickResult.COMPLETED;
    }
}
