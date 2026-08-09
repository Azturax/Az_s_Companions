package com.koncompanions.task.tasks;

import com.koncompanions.entity.CompanionEntity;
import com.koncompanions.entity.CompanionMode;
import com.koncompanions.task.CompanionTask;
import com.koncompanions.task.TaskPriority;
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
