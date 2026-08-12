package com.azscompanions.task.tasks;

import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.task.FabricCompanionTask;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Mine a specific block or scan nearby ores/stone. */
public final class FabricMineTask extends FabricCompanionTask {
    private final BlockPos fixedTarget;
    private final int radius;
    private BlockPos target;

    public FabricMineTask(BlockPos target) {
        super("mine");
        this.fixedTarget = target.immutable();
        this.radius = 8;
        this.target = this.fixedTarget;
    }

    public FabricMineTask(int radius) {
        super("mine");
        this.fixedTarget = null;
        this.radius = Math.max(2, Math.min(16, radius));
    }

    @Override
    public Result tick(FabricCompanionEntity companion, ServerLevel level) {
        if (target == null) {
            target = findTarget(level, companion);
        }
        if (target == null) {
            return Result.COMPLETED;
        }
        if (companion.blockPosition().distManhattan(target) > 2) {
            companion.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0d);
            return Result.RUNNING;
        }
        BlockState state = level.getBlockState(target);
        if (state.isAir() || state.is(Blocks.BEDROCK) || state.is(BlockTags.WITHER_IMMUNE)) {
            target = fixedTarget != null ? null : null;
            return fixedTarget != null ? Result.FAILED : Result.RUNNING;
        }
        if (!com.azscompanions.util.FabricProtectionHelper.canCompanionModify(level, target, companion)) {
            return Result.FAILED;
        }
        if (level.destroyBlock(target, true, companion)) {
            return Result.COMPLETED;
        }
        return Result.FAILED;
    }

    private BlockPos findTarget(ServerLevel level, FabricCompanionEntity companion) {
        BlockPos origin = companion.blockPosition();
        BlockPos best = null;
        int bestDist = Integer.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-radius, -2, -radius), origin.offset(radius, 2, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(Blocks.BEDROCK)) {
                continue;
            }
            if (!(state.is(BlockTags.BASE_STONE_OVERWORLD)
                    || state.is(BlockTags.COAL_ORES)
                    || state.is(BlockTags.IRON_ORES)
                    || state.is(BlockTags.COPPER_ORES)
                    || state.is(BlockTags.GOLD_ORES)
                    || state.is(BlockTags.DIAMOND_ORES)
                    || state.is(BlockTags.EMERALD_ORES)
                    || state.is(BlockTags.LAPIS_ORES)
                    || state.is(BlockTags.REDSTONE_ORES)
                    || state.is(BlockTags.LOGS))) {
                continue;
            }
            int d = origin.distManhattan(pos);
            if (d < bestDist) {
                bestDist = d;
                best = pos.immutable();
            }
        }
        return best;
    }
}
