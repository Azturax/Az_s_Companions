package com.azscompanions.task.tasks;

import com.azscompanions.config.CommonConfig;
import com.azscompanions.data.ModTags;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.task.CompanionTask;
import com.azscompanions.task.TaskPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Harvest mature crops and replant from available seeds.
 */
public final class FarmTask extends CompanionTask {
    private BlockPos target;
    private boolean missingSeeds;

    public FarmTask() {
        super("farm", TaskPriority.NORMAL);
    }

    @Override
    protected TaskTickResult onTick(CompanionEntity companion, ServerLevel level) {
        if (!companion.hasPermission("farm")) {
            fail("permission_denied");
            return TaskTickResult.FAILED;
        }
        if (target == null) {
            target = findMatureCrop(level, companion);
        }
        if (target == null) {
            if (missingSeeds) {
                fail("missing_seeds");
                return TaskTickResult.FAILED;
            }
            return complete("no_crops_ready").status() == com.azscompanions.task.TaskStatus.COMPLETED
                    ? TaskTickResult.COMPLETED : TaskTickResult.COMPLETED;
        }
        if (companion.blockPosition().distManhattan(target) > 2) {
            companion.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0d);
            setProgress(30);
            return TaskTickResult.RUNNING;
        }
        if (!companion.canBreakBlock(target)) {
            target = null;
            return TaskTickResult.RUNNING;
        }

        BlockState state = level.getBlockState(target);
        level.destroyBlock(target, true, companion);
        ItemStack seed = findSeed(companion);
        if (!seed.isEmpty() && seed.getItem() instanceof BlockItem blockItem) {
            BlockState planted = blockItem.getBlock().defaultBlockState();
            if (planted.is(ModTags.Blocks.HARVESTABLE_CROPS) || blockItem.getBlock() instanceof CropBlock) {
                level.setBlock(target, planted, Block.UPDATE_ALL);
                seed.shrink(1);
            }
        } else {
            missingSeeds = true;
        }
        setProgress(100);
        target = null;
        return TaskTickResult.COMPLETED;
    }

    private ItemStack findSeed(CompanionEntity companion) {
        for (int i = 0; i < companion.getCompanionInventory().getSlots(); i++) {
            ItemStack stack = companion.getCompanionInventory().getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(ModTags.Items.REPLANTABLE_SEEDS)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private BlockPos findMatureCrop(ServerLevel level, CompanionEntity companion) {
        int radius = CommonConfig.DEFAULT_TASK_RADIUS.get();
        int scanned = 0;
        BlockPos origin = companion.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-radius, -1, -radius),
                origin.offset(radius, 2, radius))) {
            if (scanned++ > CommonConfig.MAX_BLOCKS_SCANNED_PER_TICK.get()) {
                break;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(ModTags.Blocks.HARVESTABLE_CROPS) && state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)
                    && companion.canBreakBlock(pos)) {
                return pos.immutable();
            }
        }
        return null;
    }
}
