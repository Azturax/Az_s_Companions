package com.azscompanions.compat;

import com.azscompanions.api.MachineHandler;
import com.azscompanions.entity.CompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

public final class VanillaFurnaceMachineHandler implements MachineHandler {
    @Override
    public boolean canHandle(ServerLevel level, BlockPos pos, CompanionEntity companion) {
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.SMOKER);
    }

    @Override
    public Result interact(ServerLevel level, BlockPos pos, CompanionEntity companion) {
        IItemHandler handler = itemHandlerAt(level, pos);
        if (handler == null) {
            return Result.FAILED;
        }
        // Slot 1 is fuel on AbstractFurnaceBlockEntity item handlers.
        for (int i = 0; i < companion.getCompanionInventory().BACKPACK_SIZE; i++) {
            ItemStack stack = companion.getCompanionInventory().getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL) || stack.is(Items.LAVA_BUCKET) || stack.is(Items.BLAZE_ROD)) {
                ItemStack leftover = handler.insertItem(1, com.azscompanions.util.ItemStackCompat.copyWithCount(stack, 1), false);
                if (leftover.isEmpty()) {
                    stack.shrink(1);
                    return Result.DONE;
                }
            }
        }
        return Result.FAILED;
    }

    private static IItemHandler itemHandlerAt(ServerLevel level, BlockPos pos) {
        var be = level.getBlockEntity(pos);
        if (be == null) {
            return null;
        }
        return be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
    }

}
