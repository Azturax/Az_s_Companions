package com.azscompanions.util;

import com.azscompanions.config.CommonConfig;
import com.azscompanions.data.ModTags;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.inventory.CompanionInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Chooses tools by block suitability, remaining durability, and optional enchantments.
 */
public final class ToolSelectionHelper {
    private ToolSelectionHelper() {
    }

    public static boolean equipBestTool(CompanionEntity companion, BlockState state) {
        CompanionInventory inv = companion.getCompanionInventory();
        int bestSlot = -1;
        float bestScore = -1f;

        for (int i = 0; i < CompanionInventory.TOTAL_SIZE; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty() || !stack.is(ModTags.Items.COMPANION_TOOLS)) {
                // Still allow vanilla tools not yet tagged via isCorrectToolForDrops.
                if (stack.isEmpty() || !stack.isCorrectToolForDrops(state)) {
                    continue;
                }
            } else if (!stack.isCorrectToolForDrops(state) && !stack.is(ModTags.Items.COMPANION_TOOLS)) {
                continue;
            }

            if (CommonConfig.AVOID_BREAKING_TOOLS.get() && stack.isDamageableItem()
                    && stack.getDamageValue() >= stack.getMaxDamage() - 2) {
                continue;
            }

            float score = stack.getDestroySpeed(state);
            if (stack.isEnchanted()) {
                score += 1.5f;
            }
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        if (bestSlot < 0) {
            return false;
        }
        if (bestSlot != CompanionInventory.MAIN_HAND) {
            ItemStack current = inv.getMainHand();
            ItemStack chosen = inv.getStackInSlot(bestSlot);
            inv.setStackInSlot(CompanionInventory.MAIN_HAND, chosen);
            inv.setStackInSlot(bestSlot, current);
        }
        return true;
    }
}
