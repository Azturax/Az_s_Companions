package com.azscompanions.task.tasks;

import com.azscompanions.compat.ContainerAccessApi;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.task.CompanionTask;
import com.azscompanions.task.TaskPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

public final class DepositTask extends CompanionTask {
    @Nullable
    private BlockPos chestPos;

    public DepositTask() {
        super("deposit", TaskPriority.NORMAL);
    }

    public DepositTask to(BlockPos pos) {
        this.chestPos = pos.immutable();
        return this;
    }

    @Override
    protected TaskTickResult onTick(CompanionEntity companion, ServerLevel level) {
        if (!companion.hasPermission("containers")) {
            fail("permission_denied");
            return TaskTickResult.FAILED;
        }
        if (chestPos == null) {
            fail("no_chest");
            return TaskTickResult.FAILED;
        }
        if (companion.blockPosition().distManhattan(chestPos) > 3) {
            companion.getNavigation().moveTo(chestPos.getX() + 0.5, chestPos.getY(), chestPos.getZ() + 0.5, 1.0d);
            return TaskTickResult.RUNNING;
        }
        if (!ContainerAccessApi.canAccess(level, chestPos, companion)) {
            fail("container_forbidden");
            return TaskTickResult.FAILED;
        }
        IItemHandler handler = ((net.neoforged.neoforge.items.IItemHandler) null) /* capability deferred */;
        if (handler == null) {
            fail("not_a_container");
            return TaskTickResult.FAILED;
        }
        boolean movedAny = false;
        for (int i = 0; i < companion.getCompanionInventory().BACKPACK_SIZE; i++) {
            ItemStack stack = companion.getCompanionInventory().getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack remaining = ContainerAccessApi.insert(handler, stack, false);
            if (remaining.getCount() != stack.getCount()) {
                companion.getCompanionInventory().setStackInSlot(i, remaining);
                movedAny = true;
            }
        }
        setProgress(100);
        return movedAny ? TaskTickResult.COMPLETED : TaskTickResult.COMPLETED;
    }

    @Override
    protected void writeExtra(CompoundTag tag) {
        if (chestPos != null) {
            tag.putLong("Chest", chestPos.asLong());
        }
    }

    @Override
    protected void readExtra(CompoundTag tag) {
        if (tag.contains("Chest")) {
            chestPos = BlockPos.of(tag.getLongOr("Chest", 0L));
        }
    }
}
