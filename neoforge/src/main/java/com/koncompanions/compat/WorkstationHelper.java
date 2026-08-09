package com.koncompanions.compat;

import com.koncompanions.data.ModTags;
import com.koncompanions.entity.CompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class WorkstationHelper {
    private WorkstationHelper() {
    }

    public static void bootstrap() {
    }

    public static Optional<BlockPos> findNearestWorkstation(ServerLevel level, BlockPos origin, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -2, -radius), origin.offset(radius, 2, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModTags.Blocks.MACHINE_WORKSTATIONS)
                    || state.is(Blocks.CRAFTING_TABLE)
                    || state.is(Blocks.FURNACE)
                    || state.is(Blocks.BLAST_FURNACE)
                    || state.is(Blocks.SMOKER)
                    || state.is(Blocks.STONECUTTER)
                    || state.is(Blocks.SMITHING_TABLE)) {
                return Optional.of(pos.immutable());
            }
        }
        return Optional.empty();
    }

    /**
     * MVP ingredient consumption: remove one matching stack per ingredient.
     * Full shaped/shapeless matching can be expanded later without changing the task API.
     */
    public static boolean tryConsumeIngredients(CompanionEntity companion, Recipe<?> recipe, ServerLevel level) {
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) {
                continue;
            }
            boolean found = false;
            for (int i = 0; i < companion.getCompanionInventory().getSlots(); i++) {
                ItemStack stack = companion.getCompanionInventory().getStackInSlot(i);
                if (!stack.isEmpty() && ingredient.test(stack)) {
                    stack.shrink(1);
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }
}
