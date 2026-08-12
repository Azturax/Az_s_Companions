package com.azscompanions.task.tasks;

import com.azscompanions.data.ModTags;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.task.CompanionTask;
import com.azscompanions.task.TaskPriority;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class CollectItemsTask extends CompanionTask {
    public CollectItemsTask() {
        super("collect_items", TaskPriority.LOW);
    }

    @Override
    protected TaskTickResult onTick(CompanionEntity companion, ServerLevel level) {
        if (companion.getCompanionInventory().isFull()) {
            fail("inventory_full");
            return TaskTickResult.FAILED;
        }
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, companion.getBoundingBox().inflate(12),
                item -> !item.hasPickUpDelay() && isAllowed(item.getItem()));
        if (items.isEmpty()) {
            return TaskTickResult.COMPLETED;
        }
        ItemEntity nearest = items.getFirst();
        companion.getNavigation().moveTo(nearest, 1.1d);
        if (companion.distanceTo(nearest) < 1.5d) {
            ItemStack remaining = companion.getCompanionInventory().insertItemAuto(nearest.getItem(), false);
            if (remaining.isEmpty()) {
                nearest.discard();
            } else {
                nearest.setItem(remaining);
            }
            setProgress(100);
            return TaskTickResult.COMPLETED;
        }
        setProgress(40);
        return TaskTickResult.RUNNING;
    }

    private boolean isAllowed(ItemStack stack) {
        if (stack.is(ModTags.Items.ITEM_BLACKLIST)) {
            return false;
        }
        // Empty whitelist tag means allow all non-blacklisted items.
        return true;
    }
}
