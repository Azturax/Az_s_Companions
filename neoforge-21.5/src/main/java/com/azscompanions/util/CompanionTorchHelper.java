package com.azscompanions.util;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.inventory.CompanionInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * While tasking: hold a torch off-hand and place one on valid ground when the area is dark.
 */
public final class CompanionTorchHelper {
    public static final int DARK_LIGHT_THRESHOLD = 7;
    private static final int PLACE_COOLDOWN_TICKS = 40;

    private CompanionTorchHelper() {
    }

    public static void tickWhileTasking(CompanionEntity companion, ServerLevel level) {
        if (companion == null || level.isClientSide) {
            return;
        }
        ToolSelectionHelper.preferTorchOffhand(companion, true);
        if (companion.tickCount % PLACE_COOLDOWN_TICKS != 0) {
            return;
        }
        tryPlaceTorch(companion, level);
    }

    public static boolean tryPlaceTorch(CompanionEntity companion, ServerLevel level) {
        BlockPos feet = companion.blockPosition();
        int blockLight = level.getBrightness(LightLayer.BLOCK, feet);
        int skyLight = level.getBrightness(LightLayer.SKY, feet);
        if (Math.max(blockLight, skyLight) > DARK_LIGHT_THRESHOLD) {
            return false;
        }
        CompanionInventory inv = companion.getCompanionInventory();
        int torchSlot = findTorchSlot(inv);
        if (torchSlot < 0) {
            return false;
        }
        BlockPos placeAt = findPlacePos(level, feet);
        if (placeAt == null) {
            return false;
        }
        if (!level.getBlockState(placeAt).canBeReplaced()) {
            return false;
        }
        BlockState torch = Blocks.TORCH.defaultBlockState();
        if (!torch.canSurvive(level, placeAt)) {
            return false;
        }
        if (!level.setBlock(placeAt, torch, 3)) {
            return false;
        }
        ItemStack stack = inv.getStackInSlot(torchSlot);
        stack.shrink(1);
        if (stack.isEmpty()) {
            inv.setStackInSlot(torchSlot, ItemStack.EMPTY);
        }
        return true;
    }

    private static BlockPos findPlacePos(ServerLevel level, BlockPos feet) {
        BlockPos below = feet.below();
        BlockState ground = level.getBlockState(below);
        if (ground.isFaceSturdy(level, below, Direction.UP)
                && level.getBlockState(feet).canBeReplaced()
                && Blocks.TORCH.defaultBlockState().canSurvive(level, feet)) {
            return feet;
        }
        for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            BlockPos side = feet.relative(dir);
            BlockPos sideBelow = side.below();
            if (level.getBlockState(sideBelow).isFaceSturdy(level, sideBelow, Direction.UP)
                    && level.getBlockState(side).canBeReplaced()
                    && Blocks.TORCH.defaultBlockState().canSurvive(level, side)) {
                return side;
            }
        }
        return null;
    }

    private static int findTorchSlot(CompanionInventory inv) {
        for (int i = 0; i < CompanionInventory.TOTAL_SIZE; i++) {
            if (i == CompanionInventory.HEAD || i == CompanionInventory.CHEST
                    || i == CompanionInventory.LEGS || i == CompanionInventory.FEET
                    || i == CompanionInventory.FOOD) {
                continue;
            }
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && (stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH))) {
                return i;
            }
        }
        return -1;
    }
}
