package com.azscompanions.task.tasks;

import com.azscompanions.compat.WorkstationHelper;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.task.CompanionTask;
import com.azscompanions.task.TaskPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Crafts using nearby vanilla workstations when materials are available.
 */
public final class CraftTask extends CompanionTask {
    @Nullable
    private ResourceLocation recipeId;
    @Nullable
    private BlockPos stationPos;

    public CraftTask() {
        super("craft", TaskPriority.NORMAL);
    }

    public CraftTask recipe(ResourceLocation recipeId) {
        this.recipeId = recipeId;
        return this;
    }

    /** Resolve first crafting recipe that outputs {@code itemId} (e.g. minecraft:stick). */
    public CraftTask forResultItem(ServerLevel level, ResourceLocation itemId) {
        if (itemId == null) {
            return this;
        }
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            ItemStack result = holder.value().getResultItem(level.registryAccess());
            if (result.isEmpty()) {
                continue;
            }
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(result.getItem());
            if (itemId.equals(key)) {
                this.recipeId = holder.id();
                return this;
            }
        }
        return this;
    }

    @Override
    protected TaskTickResult onTick(CompanionEntity companion, ServerLevel level) {
        if (!companion.hasPermission("craft")) {
            fail("permission_denied");
            return TaskTickResult.FAILED;
        }
        if (stationPos == null) {
            stationPos = WorkstationHelper.findNearestWorkstation(level, companion.blockPosition(), 16).orElse(null);
        }
        if (stationPos == null) {
            fail("no_workstation");
            return TaskTickResult.FAILED;
        }
        if (companion.blockPosition().distManhattan(stationPos) > 3) {
            companion.getNavigation().moveTo(stationPos.getX() + 0.5, stationPos.getY(), stationPos.getZ() + 0.5, 1.0d);
            return TaskTickResult.RUNNING;
        }
        if (recipeId == null) {
            fail("no_recipe");
            return TaskTickResult.FAILED;
        }
        Optional<RecipeHolder<?>> recipe = level.getRecipeManager().byKey(recipeId);
        if (recipe.isEmpty()) {
            fail("recipe_missing");
            return TaskTickResult.FAILED;
        }
        ItemStack result = recipe.get().value().getResultItem(level.registryAccess()).copy();
        if (result.isEmpty()) {
            fail("empty_result");
            return TaskTickResult.FAILED;
        }
        // MVP: require the player to have staged ingredients in companion inventory; consume one matching tag stack.
        if (!WorkstationHelper.tryConsumeIngredients(companion, recipe.get().value(), level)) {
            fail("missing_ingredients");
            return TaskTickResult.FAILED;
        }
        ItemStack leftover = companion.getCompanionInventory().insertItemAuto(result, false);
        if (!leftover.isEmpty()) {
            fail("inventory_full");
            return TaskTickResult.FAILED;
        }
        setProgress(100);
        return TaskTickResult.COMPLETED;
    }

    @Override
    protected void writeExtra(CompoundTag tag) {
        if (recipeId != null) {
            tag.putString("Recipe", recipeId.toString());
        }
        if (stationPos != null) {
            tag.putLong("Station", stationPos.asLong());
        }
    }

    @Override
    protected void readExtra(CompoundTag tag) {
        if (tag.contains("Recipe")) {
            recipeId = ResourceLocation.tryParse(tag.getString("Recipe"));
        }
        if (tag.contains("Station")) {
            stationPos = BlockPos.of(tag.getLong("Station"));
        }
    }
}
