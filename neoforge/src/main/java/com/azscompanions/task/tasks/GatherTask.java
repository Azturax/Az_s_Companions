package com.azscompanions.task.tasks;

import com.azscompanions.config.CommonConfig;
import com.azscompanions.data.ModTags;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.task.CompanionTask;
import com.azscompanions.task.TaskPriority;
import com.azscompanions.util.ToolSelectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public final class GatherTask extends CompanionTask {
    private BlockPos target;

    public GatherTask() {
        super("gather", TaskPriority.NORMAL);
    }

    @Override
    protected TaskTickResult onTick(CompanionEntity companion, ServerLevel level) {
        if (!companion.hasPermission("gather")) {
            fail("permission_denied");
            return TaskTickResult.FAILED;
        }
        if (companion.getCompanionInventory().isFull()) {
            fail("inventory_full");
            return TaskTickResult.FAILED;
        }
        if (target == null || !isValid(level, companion, target)) {
            target = findTarget(level, companion);
        }
        if (target == null) {
            return complete("nothing_nearby").status() == com.azscompanions.task.TaskStatus.COMPLETED
                    ? TaskTickResult.COMPLETED : TaskTickResult.COMPLETED;
        }
        if (companion.blockPosition().distManhattan(target) > 2) {
            companion.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0d);
            setProgress(25);
            return TaskTickResult.RUNNING;
        }
        if (!ToolSelectionHelper.equipBestTool(companion, level.getBlockState(target))) {
            fail("missing_tool");
            return TaskTickResult.FAILED;
        }
        if (!companion.canBreakBlock(target)) {
            target = null;
            return TaskTickResult.RUNNING;
        }
        level.destroyBlock(target, true, companion);
        setProgress(100);
        target = null;
        return TaskTickResult.COMPLETED;
    }

    private boolean isValid(ServerLevel level, CompanionEntity companion, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir()
                && (state.is(ModTags.Blocks.TASK_MATERIALS) || state.is(ModTags.Blocks.GATHERABLE))
                && companion.canBreakBlock(pos)
                && !state.is(ModTags.Blocks.BLACKLISTED_BLOCKS);
    }

    private BlockPos findTarget(ServerLevel level, CompanionEntity companion) {
        int radius = CommonConfig.DEFAULT_TASK_RADIUS.get();
        int scanned = 0;
        BlockPos origin = companion.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = -2; y <= 2; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (scanned++ > CommonConfig.MAX_BLOCKS_SCANNED_PER_TICK.get()) {
                        return null;
                    }
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (isValid(level, companion, cursor)) {
                        return cursor.immutable();
                    }
                }
            }
        }
        return null;
    }
}
