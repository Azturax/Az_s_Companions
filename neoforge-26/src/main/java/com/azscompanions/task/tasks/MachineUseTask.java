package com.azscompanions.task.tasks;

import com.azscompanions.api.MachineHandler;
import com.azscompanions.compat.ContainerAccessApi;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.task.CompanionTask;
import com.azscompanions.task.TaskPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * Generic machine interaction via capability/API — fuels or supplies inputs when safe.
 */
public final class MachineUseTask extends CompanionTask {
    @Nullable
    private BlockPos machinePos;

    public MachineUseTask() {
        super("machine", TaskPriority.LOW);
    }

    public MachineUseTask at(BlockPos pos) {
        this.machinePos = pos.immutable();
        return this;
    }

    @Override
    protected TaskTickResult onTick(CompanionEntity companion, ServerLevel level) {
        if (machinePos == null) {
            fail("no_machine");
            return TaskTickResult.FAILED;
        }
        if (!ContainerAccessApi.canAccess(level, machinePos, companion)) {
            fail("machine_forbidden");
            return TaskTickResult.FAILED;
        }
        if (companion.blockPosition().distManhattan(machinePos) > 3) {
            companion.getNavigation().moveTo(machinePos.getX() + 0.5, machinePos.getY(), machinePos.getZ() + 0.5, 1.0d);
            return TaskTickResult.RUNNING;
        }
        for (MachineHandler handler : com.azscompanions.api.CompanionApi.machineHandlers()) {
            if (handler.canHandle(level, machinePos, companion)) {
                MachineHandler.Result result = handler.interact(level, machinePos, companion);
                if (result == MachineHandler.Result.DONE) {
                    setProgress(100);
                    return TaskTickResult.COMPLETED;
                }
                if (result == MachineHandler.Result.FAILED) {
                    fail("machine_handler_failed");
                    return TaskTickResult.FAILED;
                }
                return TaskTickResult.RUNNING;
            }
        }
        var resourceHandler = level.getCapability(Capabilities.Item.BLOCK, machinePos, null);
        if (resourceHandler == null) {
            fail("unsupported_machine");
            return TaskTickResult.FAILED;
        }
        IItemHandler items = IItemHandler.of(resourceHandler);
        // Fallback: move one fuel/input stack into the machine.
        for (int i = 0; i < companion.getCompanionInventory().BACKPACK_SIZE; i++) {
            var stack = companion.getCompanionInventory().getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            var remaining = ContainerAccessApi.insert(items, stack.copyWithCount(1), false);
            if (remaining.isEmpty()) {
                stack.shrink(1);
                setProgress(100);
                return TaskTickResult.COMPLETED;
            }
        }
        fail("no_input_items");
        return TaskTickResult.FAILED;
    }

    @Override
    protected void writeExtra(CompoundTag tag) {
        if (machinePos != null) {
            tag.putLong("Machine", machinePos.asLong());
        }
    }

    @Override
    protected void readExtra(CompoundTag tag) {
        if (tag.contains("Machine")) {
            machinePos = BlockPos.of(tag.getLongOr("Machine", 0L));
        }
    }
}
