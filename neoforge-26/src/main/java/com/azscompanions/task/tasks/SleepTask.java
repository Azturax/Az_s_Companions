package com.azscompanions.task.tasks;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.task.CompanionTask;
import com.azscompanions.task.TaskPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class SleepTask extends CompanionTask {
    public SleepTask() {
        super("sleep", TaskPriority.LOW);
    }

    @Override
    protected TaskTickResult onTick(CompanionEntity companion, ServerLevel level) {
        BlockPos home = companion.getHomePos();
        if (home == null) {
            fail("no_home");
            return TaskTickResult.FAILED;
        }
        if (companion.blockPosition().distManhattan(home) > 2) {
            companion.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0d);
            return TaskTickResult.RUNNING;
        }
        BlockState state = level.getBlockState(home);
        if (state.getBlock() instanceof BedBlock) {
            companion.setMode(CompanionMode.SIT);
            companion.heal(2.0f);
            setProgress(100);
            return TaskTickResult.COMPLETED;
        }
        companion.setMode(CompanionMode.SIT);
        setProgress(100);
        return TaskTickResult.COMPLETED;
    }
}
