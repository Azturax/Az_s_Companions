package com.azscompanions.task.tasks;

import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.inventory.FabricCompanionInventory;
import com.azscompanions.task.FabricCompanionTask;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Craft at a nearby crafting table / furnace-like workstation using companion inventory ingredients.
 */
public final class FabricCraftTask extends FabricCompanionTask {
    private ResourceLocation recipeId;
    private final ResourceLocation resultItemId;
    private BlockPos stationPos;

    public FabricCraftTask(ResourceLocation recipeId, ResourceLocation resultItemId) {
        super("craft");
        this.recipeId = recipeId;
        this.resultItemId = resultItemId;
    }

    @Override
    public Result tick(FabricCompanionEntity companion, ServerLevel level) {
        if (stationPos == null) {
            stationPos = findWorkstation(level, companion.blockPosition(), 16).orElse(null);
        }
        if (stationPos == null) {
            return Result.FAILED;
        }
        if (companion.blockPosition().distManhattan(stationPos) > 3) {
            companion.getNavigation().moveTo(stationPos.getX() + 0.5, stationPos.getY(), stationPos.getZ() + 0.5, 1.0d);
            return Result.RUNNING;
        }
        if (recipeId == null && resultItemId != null) {
            recipeId = findRecipeForResult(level, resultItemId);
        }
        if (recipeId == null) {
            return Result.FAILED;
        }
        var recipeOpt = level.getRecipeManager().byKey(recipeId);
        if (recipeOpt.isEmpty()) {
            return Result.FAILED;
        }
        Recipe<?> recipe = recipeOpt.get();
        ItemStack result = recipe.getResultItem(level.registryAccess()).copy();
        if (result.isEmpty()) {
            return Result.FAILED;
        }
        if (!consumeIngredients(companion, recipe.getIngredients())) {
            return Result.FAILED;
        }
        ItemStack leftover = companion.getCompanionInventory().insertItemAuto(result);
        if (!leftover.isEmpty()) {
            return Result.FAILED;
        }
        return Result.COMPLETED;
    }

    private static boolean consumeIngredients(FabricCompanionEntity companion, java.util.List<Ingredient> ingredients) {
        FabricCompanionInventory inv = companion.getCompanionInventory();
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) {
                continue;
            }
            boolean found = false;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
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

    private static ResourceLocation findRecipeForResult(ServerLevel level, ResourceLocation itemId) {
        for (Recipe<?> recipe : level.getRecipeManager().getRecipes()) {
            ItemStack result = recipe.getResultItem(level.registryAccess());
            if (result.isEmpty()) {
                continue;
            }
            if (itemId.equals(BuiltInRegistries.ITEM.getKey(result.getItem()))) {
                return recipe.getId();
            }
        }
        return null;
    }

    private static java.util.Optional<BlockPos> findWorkstation(ServerLevel level, BlockPos origin, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -2, -radius), origin.offset(radius, 2, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.CRAFTING_TABLE)
                    || state.is(Blocks.FURNACE)
                    || state.is(Blocks.BLAST_FURNACE)
                    || state.is(Blocks.SMOKER)
                    || state.is(Blocks.STONECUTTER)
                    || state.is(Blocks.SMITHING_TABLE)) {
                return java.util.Optional.of(pos.immutable());
            }
        }
        return java.util.Optional.empty();
    }
}
