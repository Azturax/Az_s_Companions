package com.koncompanions.task.tasks;

import com.koncompanions.config.CommonConfig;
import com.koncompanions.data.ModTags;
import com.koncompanions.entity.CompanionEntity;
import com.koncompanions.task.CompanionTask;
import com.koncompanions.task.TaskPriority;
import com.koncompanions.util.ToolSelectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

public final class ChopTreesTask extends CompanionTask {
    private BlockPos target;

    public ChopTreesTask() {
        super("chop_trees", TaskPriority.NORMAL);
    }

    @Override
    protected TaskTickResult onTick(CompanionEntity companion, ServerLevel level) {
        if (companion.getCompanionInventory().isFull()) {
            fail("inventory_full");
            return TaskTickResult.FAILED;
        }
        if (target == null) {
            target = findLog(level, companion);
        }
        if (target == null) {
            return TaskTickResult.COMPLETED;
        }
        if (companion.blockPosition().distManhattan(target) > 2) {
            companion.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0d);
            return TaskTickResult.RUNNING;
        }
        BlockState state = level.getBlockState(target);
        if (!ToolSelectionHelper.equipBestTool(companion, state)) {
            fail("missing_axe");
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

    private BlockPos findLog(ServerLevel level, CompanionEntity companion) {
        int radius = CommonConfig.DEFAULT_TASK_RADIUS.get();
        BlockPos origin = companion.blockPosition();
        int scanned = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, 0, -radius), origin.offset(radius, 12, radius))) {
            if (scanned++ > CommonConfig.MAX_BLOCKS_SCANNED_PER_TICK.get()) {
                break;
            }
            BlockState state = level.getBlockState(pos);
            if ((state.is(BlockTags.LOGS) || state.is(ModTags.Blocks.TASK_MATERIALS))
                    && !state.is(ModTags.Blocks.BLACKLISTED_BLOCKS)
                    && companion.canBreakBlock(pos)) {
                return pos.immutable();
            }
        }
        return null;
    }
}
