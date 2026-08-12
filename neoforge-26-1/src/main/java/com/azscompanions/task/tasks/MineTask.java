package com.azscompanions.task.tasks;

import com.azscompanions.config.CommonConfig;
import com.azscompanions.data.ModTags;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.task.CompanionTask;
import com.azscompanions.task.TaskPriority;
import com.azscompanions.util.ToolSelectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

public final class MineTask extends CompanionTask {
    private int radius = 8;
    private BlockPos target;

    public MineTask() {
        super("mine", TaskPriority.NORMAL);
    }

    public MineTask at(BlockPos pos) {
        this.target = pos.immutable();
        return this;
    }

    public MineTask withRadius(int radius) {
        this.radius = radius;
        return this;
    }

    @Override
    protected TaskTickResult onTick(CompanionEntity companion, ServerLevel level) {
        if (!companion.hasPermission("gather")) {
            fail("permission_denied");
            return TaskTickResult.FAILED;
        }
        if (target == null) {
            target = findOre(level, companion);
        }
        if (target == null) {
            return TaskTickResult.COMPLETED;
        }
        if (companion.blockPosition().distManhattan(target) > 2) {
            companion.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0d);
            setProgress(20);
            return TaskTickResult.RUNNING;
        }
        BlockState state = level.getBlockState(target);
        if (!ToolSelectionHelper.equipBestTool(companion, state)) {
            fail("missing_pickaxe");
            return TaskTickResult.FAILED;
        }
        ToolSelectionHelper.preferTorchOffhand(companion, true);
        if (!companion.canBreakBlock(target)) {
            target = null;
            return TaskTickResult.RUNNING;
        }
        level.destroyBlock(target, true, companion);
        setProgress(100);
        target = null;
        return TaskTickResult.COMPLETED;
    }

    private BlockPos findOre(ServerLevel level, CompanionEntity companion) {
        BlockPos origin = companion.blockPosition();
        int scanned = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
            if (scanned++ > CommonConfig.MAX_BLOCKS_SCANNED_PER_TICK.get()) {
                break;
            }
            BlockState state = level.getBlockState(pos);
            if ((state.is(BlockTags.COAL_ORES) || state.is(BlockTags.IRON_ORES) || state.is(BlockTags.COPPER_ORES)
                    || state.is(BlockTags.GOLD_ORES) || state.is(BlockTags.DIAMOND_ORES) || state.is(BlockTags.EMERALD_ORES)
                    || state.is(BlockTags.LAPIS_ORES) || state.is(BlockTags.REDSTONE_ORES)
                    || state.is(ModTags.Blocks.TASK_MATERIALS))
                    && !state.is(ModTags.Blocks.BLACKLISTED_BLOCKS)
                    && companion.canBreakBlock(pos)) {
                return pos.immutable();
            }
        }
        return null;
    }

    @Override
    protected void writeExtra(CompoundTag tag) {
        tag.putInt("Radius", radius);
    }

    @Override
    protected void readExtra(CompoundTag tag) {
        radius = tag.getIntOr("Radius", 0);
    }
}
