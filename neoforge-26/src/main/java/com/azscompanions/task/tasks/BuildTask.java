package com.azscompanions.task.tasks;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.task.CompanionTask;
import com.azscompanions.task.TaskPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Places blocks from a simple saved plan (schematic-like list of positions + preferred block item).
 */
public final class BuildTask extends CompanionTask {
    private final List<BlockPos> plan = new ArrayList<>();

    public BuildTask() {
        super("build", TaskPriority.NORMAL);
    }

    public BuildTask withPlan(List<BlockPos> positions) {
        plan.clear();
        plan.addAll(positions);
        return this;
    }

    @Override
    protected TaskTickResult onTick(CompanionEntity companion, ServerLevel level) {
        if (!companion.hasPermission("build")) {
            fail("permission_denied");
            return TaskTickResult.FAILED;
        }
        if (plan.isEmpty()) {
            return TaskTickResult.COMPLETED;
        }
        Iterator<BlockPos> it = plan.iterator();
        BlockPos next = it.next();
        if (companion.blockPosition().distManhattan(next) > 3) {
            companion.getNavigation().moveTo(next.getX() + 0.5, next.getY(), next.getZ() + 0.5, 1.0d);
            return TaskTickResult.RUNNING;
        }
        if (!companion.canBreakBlock(next) && !level.getBlockState(next).canBeReplaced()) {
            it.remove();
            return TaskTickResult.RUNNING;
        }
        ItemStack material = findBlockItem(companion);
        if (material.isEmpty() || !(material.getItem() instanceof BlockItem blockItem)) {
            fail("missing_materials");
            return TaskTickResult.FAILED;
        }
        BlockState placeState = blockItem.getBlock().defaultBlockState();
        if (level.setBlock(next, placeState, Block.UPDATE_ALL)) {
            material.shrink(1);
            it.remove();
            setProgress(Math.max(1, 100 - (plan.size() * 5)));
            return plan.isEmpty() ? TaskTickResult.COMPLETED : TaskTickResult.RUNNING;
        }
        fail("place_failed");
        return TaskTickResult.FAILED;
    }

    private ItemStack findBlockItem(CompanionEntity companion) {
        for (int i = 0; i < companion.getCompanionInventory().getSlots(); i++) {
            ItemStack stack = companion.getCompanionInventory().getStackInSlot(i);
            if (stack.getItem() instanceof BlockItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void writeExtra(CompoundTag tag) {
        ListTag list = new ListTag();
        for (BlockPos pos : plan) {
            CompoundTag p = new CompoundTag();
            p.putLong("Pos", pos.asLong());
            list.add(p);
        }
        tag.put("Plan", list);
    }

    @Override
    protected void readExtra(CompoundTag tag) {
        plan.clear();
        ListTag list = tag.getListOrEmpty("Plan");
        for (int i = 0; i < list.size(); i++) {
            plan.add(BlockPos.of(list.getCompoundOrEmpty(i).getLongOr("Pos", 0L)));
        }
    }
}
