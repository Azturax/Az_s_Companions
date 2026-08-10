package com.azscompanions.util;

import com.azscompanions.config.CommonConfig;
import com.azscompanions.data.ModTags;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.inventory.CompanionInventory;
import com.azscompanions.item.CompanionCharmItem;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Auto-equip: best matching tool → main hand; torch → off-hand when dark or while tasking.
 */
public final class ToolSelectionHelper {
    public static final int DARK_LIGHT_THRESHOLD = 7;

    private ToolSelectionHelper() {
    }

    public static boolean equipBestTool(CompanionEntity companion, BlockState state) {
        if (companion == null || state == null || state.isAir()) {
            return false;
        }
        CompanionInventory inv = companion.getCompanionInventory();
        int bestSlot = -1;
        float bestScore = -1f;
        for (int i = 0; i < CompanionInventory.TOTAL_SIZE; i++) {
            if (i == CompanionInventory.OFF_HAND || i == CompanionInventory.HEAD
                    || i == CompanionInventory.CHEST || i == CompanionInventory.LEGS
                    || i == CompanionInventory.FEET || i == CompanionInventory.FOOD) {
                continue;
            }
            ItemStack stack = inv.getStackInSlot(i);
            float score = toolScore(stack, state);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        if (bestSlot < 0 || bestScore <= 0f) {
            return !inv.getMainHand().isEmpty() && toolScore(inv.getMainHand(), state) > 0f;
        }
        if (bestSlot != CompanionInventory.MAIN_HAND) {
            swapSlots(inv, CompanionInventory.MAIN_HAND, bestSlot);
        }
        return true;
    }

    public static boolean preferTorchOffhand(CompanionEntity companion, boolean preferAlways) {
        if (companion == null || companion.level().isClientSide()) {
            return false;
        }
        CompanionInventory inv = companion.getCompanionInventory();
        if (isTorch(inv.getOffHand())) {
            return true;
        }
        Level level = companion.level();
        int blockLight = level.getBrightness(LightLayer.BLOCK, companion.blockPosition());
        int skyLight = level.getBrightness(LightLayer.SKY, companion.blockPosition());
        boolean dark = Math.max(blockLight, skyLight) <= DARK_LIGHT_THRESHOLD;
        if (!preferAlways && !dark) {
            return false;
        }
        int torchSlot = findTorchSlot(inv);
        if (torchSlot < 0 || torchSlot == CompanionInventory.MAIN_HAND) {
            return false;
        }
        swapSlots(inv, CompanionInventory.OFF_HAND, torchSlot);
        return isTorch(inv.getOffHand());
    }

    public static void equipForBreaking(CompanionEntity companion, BlockState state) {
        equipBestTool(companion, state);
        preferTorchOffhand(companion, true);
    }

    public static boolean isTorch(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH)
                || stack.is(Items.REDSTONE_TORCH) || stack.is(Items.LANTERN)
                || stack.is(Items.SOUL_LANTERN);
    }

    private static int findTorchSlot(CompanionInventory inv) {
        for (int i = 0; i < CompanionInventory.BACKPACK_SIZE; i++) {
            if (isTorch(inv.getStackInSlot(i))) {
                return i;
            }
        }
        for (int i = CompanionInventory.HOTBAR_EXTRA_START;
             i < CompanionInventory.HOTBAR_EXTRA_START + CompanionInventory.HOTBAR_EXTRA_SLOTS; i++) {
            if (isTorch(inv.getStackInSlot(i))) {
                return i;
            }
        }
        return -1;
    }

    private static float toolScore(ItemStack stack, BlockState state) {
        if (stack == null || stack.isEmpty() || CompanionCharmItem.isCharm(stack) || isTorch(stack)
                || stack.is(Items.SHIELD) || stack.is(Items.TOTEM_OF_UNDYING)) {
            return -1f;
        }
        if (CommonConfig.AVOID_BREAKING_TOOLS.get() && stack.isDamageableItem()
                && stack.getDamageValue() >= stack.getMaxDamage() - 2) {
            return -1f;
        }
        boolean correct = stack.isCorrectToolForDrops(state);
        float speed = stack.getDestroySpeed(state);
        boolean taggedTool = stack.is(ModTags.Items.COMPANION_TOOLS)
                || stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES) || stack.is(ItemTags.SWORDS);
        if (!correct && speed <= 1.01f && !taggedTool) {
            return -1f;
        }
        float score = speed;
        if (correct) {
            score += 50f;
        }
        if (taggedTool) {
            score += 5f;
        }
        if (stack.isEnchanted()) {
            score += 1.5f;
        }
        if (stack.isDamageableItem()) {
            score += (stack.getMaxDamage() - stack.getDamageValue()) * 0.001f;
        }
        return score;
    }

    private static void swapSlots(CompanionInventory inv, int a, int b) {
        if (a == b) {
            return;
        }
        ItemStack stackA = inv.getStackInSlot(a);
        ItemStack stackB = inv.getStackInSlot(b);
        if (CompanionCharmItem.isCharm(stackA) || CompanionCharmItem.isCharm(stackB)) {
            return;
        }
        inv.setStackInSlot(a, stackB);
        inv.setStackInSlot(b, stackA);
    }
}
