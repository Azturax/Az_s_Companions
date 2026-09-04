package com.azscompanions.util;

import com.azscompanions.entity.CompanionEquipmentSupport;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.inventory.FabricCompanionInventory;
import com.azscompanions.item.FabricCompanionCharmItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

/** Fabric: best tool → main hand; torch → off-hand when dark or while tasking. */
public final class FabricToolSelectionHelper {
    public static final int DARK_LIGHT_THRESHOLD = 7;

    private FabricToolSelectionHelper() {
    }

    public static boolean equipBestTool(FabricCompanionEntity companion, BlockState state) {
        if (companion == null || state == null || state.isAir()) {
            return false;
        }
        FabricCompanionInventory inv = companion.getCompanionInventory();
        int bestSlot = -1;
        float bestScore = -1f;
        for (int i = 0; i < FabricCompanionInventory.TOTAL_SIZE; i++) {
            if (i == FabricCompanionInventory.OFF_HAND || i == FabricCompanionInventory.HEAD
                    || i == FabricCompanionInventory.CHEST || i == FabricCompanionInventory.LEGS
                    || i == FabricCompanionInventory.FEET || i == FabricCompanionInventory.FOOD) {
                continue;
            }
            float score = toolScore(inv.getItem(i), state);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        if (bestSlot < 0 || bestScore <= 0f) {
            return !inv.getMainHand().isEmpty() && toolScore(inv.getMainHand(), state) > 0f;
        }
        if (bestSlot != FabricCompanionInventory.MAIN_HAND) {
            swapSlots(inv, FabricCompanionInventory.MAIN_HAND, bestSlot);
        }
        return true;
    }

    public static boolean preferTorchOffhand(FabricCompanionEntity companion, boolean preferAlways) {
        if (companion == null || companion.level().isClientSide) {
            return false;
        }
        FabricCompanionInventory inv = companion.getCompanionInventory();
        if (isTorch(inv.getOffHand())) {
            return true;
        }
        Level level = companion.level();
        boolean dark = Math.max(
                level.getBrightness(LightLayer.BLOCK, companion.blockPosition()),
                level.getBrightness(LightLayer.SKY, companion.blockPosition())) <= DARK_LIGHT_THRESHOLD;
        if (!preferAlways && !dark) {
            return false;
        }
        int torchSlot = findTorchSlot(inv);
        if (torchSlot < 0 || torchSlot == FabricCompanionInventory.MAIN_HAND) {
            return false;
        }
        swapSlots(inv, FabricCompanionInventory.OFF_HAND, torchSlot);
        return isTorch(inv.getOffHand());
    }

    public static void equipForBreaking(FabricCompanionEntity companion, BlockState state) {
        equipBestTool(companion, state);
        preferTorchOffhand(companion, true);
    }

    public static boolean isTorch(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && (stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH)
                || stack.is(Items.REDSTONE_TORCH) || stack.is(Items.LANTERN)
                || stack.is(Items.SOUL_LANTERN));
    }

    private static int findTorchSlot(FabricCompanionInventory inv) {
        for (int i = 0; i < FabricCompanionInventory.BACKPACK_SIZE; i++) {
            if (isTorch(inv.getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    private static float toolScore(ItemStack stack, BlockState state) {
        if (stack == null || stack.isEmpty() || FabricCompanionCharmItem.isCharm(stack) || isTorch(stack)
                || stack.is(Items.SHIELD) || stack.is(Items.TOTEM_OF_UNDYING)) {
            return -1f;
        }
        if (stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 2) {
            return -1f;
        }
        boolean correct = stack.isCorrectToolForDrops(state);
        float speed = stack.getDestroySpeed(state);
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        boolean tagged = stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES) || stack.is(ItemTags.SWORDS)
                || CompanionEquipmentSupport.looksLikeTool(id);
        if (!correct && speed <= 1.01f && !tagged) {
            return -1f;
        }
        float score = speed + (correct ? 50f : 0f) + (tagged ? 5f : 0f);
        if (stack.isEnchanted()) {
            score += 1.5f;
        }
        return score;
    }

    private static void swapSlots(FabricCompanionInventory inv, int a, int b) {
        ItemStack sa = inv.getItem(a);
        ItemStack sb = inv.getItem(b);
        if (FabricCompanionCharmItem.isCharm(sa) || FabricCompanionCharmItem.isCharm(sb)) {
            return;
        }
        inv.setItem(a, sb);
        inv.setItem(b, sa);
    }
}
