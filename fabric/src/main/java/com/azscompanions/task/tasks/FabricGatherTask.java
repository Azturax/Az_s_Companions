package com.azscompanions.task.tasks;

import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.task.FabricCompanionTask;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

public final class FabricGatherTask extends FabricCompanionTask {
    private BlockPos target;

    public FabricGatherTask() {
        super("gather");
    }

    @Override
    public Result tick(FabricCompanionEntity companion, ServerLevel level) {
        if (companion.getCompanionInventory().isFull()) {
            return Result.FAILED;
        }
        if (target == null) {
            target = findLog(level, companion.blockPosition());
        }
        if (target == null) {
            return Result.COMPLETED;
        }
        if (companion.blockPosition().distManhattan(target) > 2) {
            companion.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0d);
            return Result.RUNNING;
        }
        level.destroyBlock(target, true, companion);
        target = null;
        companion.speakSuccess();
        return Result.COMPLETED;
    }

    private BlockPos findLog(ServerLevel level, BlockPos origin) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -2, -8), origin.offset(8, 8, 8))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LOGS)) {
                return pos.immutable();
            }
        }
        return null;
    }
}
