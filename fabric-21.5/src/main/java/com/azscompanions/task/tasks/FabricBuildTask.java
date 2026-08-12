package com.azscompanions.task.tasks;

import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.inventory.FabricCompanionInventory;
import com.azscompanions.task.FabricCompanionTask;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Place blocks from companion inventory at planned positions. */
public final class FabricBuildTask extends FabricCompanionTask {
    private final List<BlockPos> plan = new ArrayList<>();
    private final ResourceLocation preferItem;

    public FabricBuildTask(List<BlockPos> positions, String itemId) {
        super("build");
        if (positions != null) {
            plan.addAll(positions);
        }
        this.preferItem = itemId == null || itemId.isBlank() ? null : ResourceLocation.tryParse(itemId);
    }

    @Override
    public Result tick(FabricCompanionEntity companion, ServerLevel level) {
        if (plan.isEmpty()) {
            return Result.COMPLETED;
        }
        Iterator<BlockPos> it = plan.iterator();
        BlockPos next = it.next();
        if (companion.blockPosition().distManhattan(next) > 3) {
            companion.getNavigation().moveTo(next.getX() + 0.5, next.getY(), next.getZ() + 0.5, 1.0d);
            return Result.RUNNING;
        }
        if (!level.getBlockState(next).canBeReplaced()) {
            it.remove();
            return plan.isEmpty() ? Result.COMPLETED : Result.RUNNING;
        }
        if (!com.azscompanions.util.FabricProtectionHelper.canCompanionModify(level, next, companion)) {
            it.remove();
            return plan.isEmpty() ? Result.COMPLETED : Result.RUNNING;
        }
        ItemStack material = findBlockItem(companion);
        if (material.isEmpty() || !(material.getItem() instanceof BlockItem blockItem)) {
            return Result.FAILED;
        }
        BlockState placeState = blockItem.getBlock().defaultBlockState();
        if (level.setBlock(next, placeState, Block.UPDATE_ALL)) {
            material.shrink(1);
            it.remove();
            return plan.isEmpty() ? Result.COMPLETED : Result.RUNNING;
        }
        return Result.FAILED;
    }

    private ItemStack findBlockItem(FabricCompanionEntity companion) {
        FabricCompanionInventory inv = companion.getCompanionInventory();
        ItemStack main = inv.getMainHand();
        if (!main.isEmpty() && main.getItem() instanceof BlockItem) {
            if (preferItem == null || preferItem.equals(BuiltInRegistries.ITEM.getKey(main.getItem()))) {
                return main;
            }
        }
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
                continue;
            }
            if (preferItem == null || preferItem.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
