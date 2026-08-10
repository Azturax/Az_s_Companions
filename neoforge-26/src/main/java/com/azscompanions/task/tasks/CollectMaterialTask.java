package com.azscompanions.task.tasks;

import com.azscompanions.compat.ContainerAccessApi;
import com.azscompanions.config.CommonConfig;
import com.azscompanions.data.ModTags;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.inventory.CompanionInventory;
import com.azscompanions.task.CompanionTask;
import com.azscompanions.task.TaskPriority;
import com.azscompanions.util.ToolSelectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Copper-golem-like material goal: gather {@code count} of {@code item}, then deposit into a nearby chest.
 * Normal companion goals stay registered; the task queue sets {@code TASK} mode while active.
 */
public final class CollectMaterialTask extends CompanionTask {
    private static final int SCAN_Y = 4;
    private static final int CHEST_RADIUS = 24;
    private static final int REPORT_INTERVAL = 100;

    @Nullable
    private Item targetItem = Items.COBBLESTONE;
    private int targetCount = 64;
    private int deposited;
    @Nullable
    private BlockPos chestPos;
    @Nullable
    private BlockPos breakTarget;
    private int ticks;
    private boolean preferDeposit;

    public CollectMaterialTask() {
        super("collect_material", TaskPriority.NORMAL);
    }

    public CollectMaterialTask of(Item item, int count) {
        this.targetItem = item == null ? Items.COBBLESTONE : item;
        this.targetCount = Math.max(1, Math.min(count, 1_000_000));
        return this;
    }

    public CollectMaterialTask depositAt(@Nullable BlockPos pos) {
        this.chestPos = pos == null ? null : pos.immutable();
        return this;
    }

    public Item targetItem() {
        return targetItem == null ? Items.COBBLESTONE : targetItem;
    }

    public int targetCount() {
        return targetCount;
    }

    public int deposited() {
        return deposited;
    }

    public int totalCollected(CompanionEntity companion) {
        return deposited + countHeld(companion);
    }

    public String progressLabel(CompanionEntity companion) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(targetItem());
        return "collect " + totalCollected(companion) + "/" + targetCount + " " + id;
    }

    @Override
    protected TaskTickResult onTick(CompanionEntity companion, ServerLevel level) {
        ticks++;
        if (!companion.hasPermission("gather")) {
            fail("permission_denied");
            return TaskTickResult.FAILED;
        }
        Item item = targetItem();
        int held = countHeld(companion);
        int total = deposited + held;
        updateProgress(total);

        if (ticks % REPORT_INTERVAL == 0 && companion.getOwner() instanceof ServerPlayer owner) {
            owner.displayClientMessage(Component.literal(
                    companion.getChatDisplayName() + " — " + progressLabel(companion)), true);
        }

        if (total >= targetCount) {
            if (held > 0) {
                TaskTickResult dep = depositHeld(companion, level, item);
                if (countHeld(companion) == 0) {
                    return TaskTickResult.COMPLETED;
                }
                return dep == TaskTickResult.FAILED ? dep : TaskTickResult.RUNNING;
            }
            return TaskTickResult.COMPLETED;
        }

        if (preferDeposit || companion.getCompanionInventory().isFull()
                || held >= Math.min(64, Math.max(1, targetCount - deposited))) {
            preferDeposit = true;
            TaskTickResult depositResult = depositHeld(companion, level, item);
            if (deposited + countHeld(companion) >= targetCount && countHeld(companion) == 0) {
                return TaskTickResult.COMPLETED;
            }
            if (depositResult == TaskTickResult.FAILED && companion.getCompanionInventory().isFull()) {
                return depositResult;
            }
            if (depositResult == TaskTickResult.RUNNING) {
                return TaskTickResult.RUNNING;
            }
            preferDeposit = false;
        }

        ItemEntity ground = findGroundItem(level, companion, item);
        if (ground != null) {
            companion.getNavigation().moveTo(ground, 1.1d);
            if (companion.distanceTo(ground) < 1.6d) {
                ItemStack remaining = companion.getCompanionInventory().insertItemAuto(ground.getItem(), false);
                if (remaining.isEmpty()) {
                    ground.discard();
                } else {
                    ground.setItem(remaining);
                }
            }
            return TaskTickResult.RUNNING;
        }

        if (breakTarget == null || !isUsefulBlock(level, companion, breakTarget, item)) {
            breakTarget = findBlock(level, companion, item);
        }
        if (breakTarget == null) {
            setProgress(Math.max(5, progress()));
            return TaskTickResult.RUNNING;
        }
        if (companion.blockPosition().distManhattan(breakTarget) > 2) {
            companion.getNavigation().moveTo(
                    breakTarget.getX() + 0.5, breakTarget.getY(), breakTarget.getZ() + 0.5, 1.0d);
            return TaskTickResult.RUNNING;
        }
        BlockState state = level.getBlockState(breakTarget);
        ToolSelectionHelper.equipForBreaking(companion, state);
        if (!companion.canBreakBlock(breakTarget)) {
            breakTarget = null;
            return TaskTickResult.RUNNING;
        }
        level.destroyBlock(breakTarget, true, companion);
        breakTarget = null;
        return TaskTickResult.RUNNING;
    }

    private TaskTickResult depositHeld(CompanionEntity companion, ServerLevel level, Item item) {
        if (!companion.hasPermission("containers")) {
            fail("permission_denied");
            return TaskTickResult.FAILED;
        }
        if (chestPos == null || !isChest(level, chestPos)) {
            chestPos = findNearestChest(level, companion);
        }
        if (chestPos == null) {
            fail("no_chest");
            return TaskTickResult.FAILED;
        }
        if (companion.blockPosition().distManhattan(chestPos) > 3) {
            companion.getNavigation().moveTo(
                    chestPos.getX() + 0.5, chestPos.getY(), chestPos.getZ() + 0.5, 1.0d);
            return TaskTickResult.RUNNING;
        }
        if (!ContainerAccessApi.canAccess(level, chestPos, companion)) {
            fail("container_forbidden");
            return TaskTickResult.FAILED;
        }
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, chestPos, null);
        if (handler == null) {
            fail("not_a_container");
            return TaskTickResult.FAILED;
        }
        CompanionInventory inv = companion.getCompanionInventory();
        boolean moved = false;
        for (int i = 0; i < CompanionInventory.BACKPACK_SIZE; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty() || !stack.is(item)) {
                continue;
            }
            int before = stack.getCount();
            ItemStack remaining = ContainerAccessApi.insert(handler, stack, false);
            int movedCount = before - remaining.getCount();
            if (movedCount > 0) {
                deposited += movedCount;
                inv.setStackInSlot(i, remaining);
                moved = true;
            }
        }
        updateProgress(deposited + countHeld(companion));
        return moved || countHeld(companion) == 0 ? TaskTickResult.COMPLETED : TaskTickResult.RUNNING;
    }

    private void updateProgress(int total) {
        setProgress((int) Math.min(100, Math.round(100.0 * total / (double) targetCount)));
    }

    private int countHeld(CompanionEntity companion) {
        Item item = targetItem();
        int n = 0;
        CompanionInventory inv = companion.getCompanionInventory();
        for (int i = 0; i < CompanionInventory.BACKPACK_SIZE; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.is(item)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    @Nullable
    private ItemEntity findGroundItem(ServerLevel level, CompanionEntity companion, Item item) {
        AABB box = companion.getBoundingBox().inflate(12);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box,
                e -> !e.hasPickUpDelay() && e.getItem().is(item));
        if (items.isEmpty()) {
            return null;
        }
        items.sort((a, b) -> Double.compare(companion.distanceToSqr(a), companion.distanceToSqr(b)));
        return items.getFirst();
    }

    @Nullable
    private BlockPos findBlock(ServerLevel level, CompanionEntity companion, Item item) {
        int radius = CommonConfig.DEFAULT_TASK_RADIUS.get();
        int scanned = 0;
        BlockPos origin = companion.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        int bestDist = Integer.MAX_VALUE;
        for (int y = -SCAN_Y; y <= SCAN_Y; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (scanned++ > CommonConfig.MAX_BLOCKS_SCANNED_PER_TICK.get()) {
                        return best;
                    }
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!isUsefulBlock(level, companion, cursor, item)) {
                        continue;
                    }
                    int dist = origin.distManhattan(cursor);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = cursor.immutable();
                    }
                }
            }
        }
        return best;
    }

    private boolean isUsefulBlock(ServerLevel level, CompanionEntity companion, BlockPos pos, Item item) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.is(ModTags.Blocks.BLACKLISTED_BLOCKS)) {
            return false;
        }
        if (!companion.canBreakBlock(pos)) {
            return false;
        }
        if (state.getBlock().asItem() == item) {
            return true;
        }
        ItemStack tool = companion.getCompanionInventory().getMainHand();
        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), companion, tool);
        for (ItemStack drop : drops) {
            if (drop.is(item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isChest(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ModTags.Blocks.ALLOWED_CONTAINERS)) {
            return true;
        }
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null;
    }

    @Nullable
    private static BlockPos findNearestChest(ServerLevel level, CompanionEntity companion) {
        BlockPos origin = companion.blockPosition();
        BlockPos best = null;
        int bestDist = Integer.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = -2; y <= 4; y++) {
            for (int x = -CHEST_RADIUS; x <= CHEST_RADIUS; x++) {
                for (int z = -CHEST_RADIUS; z <= CHEST_RADIUS; z++) {
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!isChest(level, cursor)) {
                        continue;
                    }
                    if (!ContainerAccessApi.canAccess(level, cursor, companion)) {
                        continue;
                    }
                    int dist = origin.distManhattan(cursor);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = cursor.immutable();
                    }
                }
            }
        }
        return best;
    }

    @Override
    protected void writeExtra(CompoundTag tag) {
        tag.putString("Item", BuiltInRegistries.ITEM.getKey(targetItem()).toString());
        tag.putInt("Count", targetCount);
        tag.putInt("Deposited", deposited);
        if (chestPos != null) {
            tag.putLong("Chest", chestPos.asLong());
        }
    }

    @Override
    protected void readExtra(CompoundTag tag) {
        if (tag.contains("Item")) {
            BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(tag.getString("Item")))
                    .ifPresent(i -> targetItem = i);
        }
        if (tag.contains("Count")) {
            targetCount = Math.max(1, tag.getInt("Count"));
        }
        deposited = tag.contains("Deposited") ? tag.getInt("Deposited") : 0;
        if (tag.contains("Chest")) {
            chestPos = BlockPos.of(tag.getLong("Chest"));
        }
    }
}
