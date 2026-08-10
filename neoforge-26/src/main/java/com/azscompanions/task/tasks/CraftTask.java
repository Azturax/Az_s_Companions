package com.azscompanions.task.tasks;

import com.azscompanions.compat.WorkstationHelper;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.task.CompanionTask;
import com.azscompanions.task.TaskPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
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
    private Identifier recipeId;
    @Nullable
    private BlockPos stationPos;

    public CraftTask() {
        super("craft", TaskPriority.NORMAL);
    }

    public CraftTask recipe(Identifier recipeId) {
        this.recipeId = recipeId;
        return this;
    }

    /** Resolve first crafting recipe that outputs {@code itemId} (e.g. minecraft:stick). */
    public CraftTask forResultItem(ServerLevel level, Identifier itemId) {
        if (itemId == null) {
            return this;
        }
        /* Recipe enumeration deferred for NeoForge 26.2. */
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
        // Recipe ResourceKey / assemble APIs moved in NeoForge 26.2 — craft task deferred.
        fail("recipe_api_deferred");
        return TaskTickResult.FAILED;
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
            recipeId = Identifier.tryParse(tag.getStringOr("Recipe", ""));
        }
        if (tag.contains("Station")) {
            stationPos = BlockPos.of(tag.getLongOr("Station", 0L));
        }
    }
}
