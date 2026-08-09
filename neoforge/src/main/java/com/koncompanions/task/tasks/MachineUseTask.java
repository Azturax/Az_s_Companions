package com.koncompanions.task.tasks;

import com.koncompanions.api.MachineHandler;
import com.koncompanions.compat.ContainerAccessApi;
import com.koncompanions.entity.CompanionEntity;
import com.koncompanions.task.CompanionTask;
import com.koncompanions.task.TaskPriority;
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
        for (MachineHandler handler : com.koncompanions.api.CompanionApi.machineHandlers()) {
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
        IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, machinePos, null);
        if (items == null) {
            fail("unsupported_machine");
            return TaskTickResult.FAILED;
        }
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
            machinePos = BlockPos.of(tag.getLong("Machine"));
        }
    }
}
