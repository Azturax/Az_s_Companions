package com.koncompanions.task.tasks;

import com.koncompanions.entity.CompanionEntity;
import com.koncompanions.entity.CompanionMode;
import com.koncompanions.task.CompanionTask;
import com.koncompanions.task.TaskPriority;
import com.koncompanions.voice.DialogueCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class ReturnHomeTask extends CompanionTask {
    public ReturnHomeTask() {
        super("return_home", TaskPriority.HIGH);
    }

    @Override
    protected void onStart(CompanionEntity companion, ServerLevel level) {
        companion.setMode(CompanionMode.HOME);
        companion.speak(DialogueCategory.RETURN_HOME);
    }

    @Override
    protected TaskTickResult onTick(CompanionEntity companion, ServerLevel level) {
        BlockPos home = companion.getHomePos();
        if (home == null) {
            fail("no_home");
            return TaskTickResult.FAILED;
        }
        if (companion.blockPosition().distManhattan(home) <= 2) {
            companion.getNavigation().stop();
            setProgress(100);
            return TaskTickResult.COMPLETED;
        }
        if (companion.blockPosition().distManhattan(home) > 48) {
            companion.safeTeleportNear(home);
        } else {
            companion.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.1d);
        }
        setProgress(50);
        return TaskTickResult.RUNNING;
    }
}
