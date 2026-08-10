package com.azscompanions.ai;

import com.azscompanions.entity.CompanionPlayMode;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricCompanionMode;
import com.azscompanions.entity.inventory.FabricCompanionInventory;
import com.azscompanions.task.tasks.FabricBuildTask;
import com.azscompanions.task.tasks.FabricCraftTask;
import com.azscompanions.task.tasks.FabricMineTask;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Executes structured LLM actions on a Fabric companion.
 */
public final class FabricCompanionAiActionExecutor {
    private FabricCompanionAiActionExecutor() {
    }

    public static List<String> execute(FabricCompanionEntity companion, ServerPlayer owner,
                                       List<CompanionAiAction> actions, CompanionAiSettings settings) {
        return execute(companion, owner, actions, settings, owner, true);
    }

    /**
     * @param approachTarget player to briefly approach for {@code come_here}/{@code run_at_player}
     * @param fullControl when false, only {@link CompanionAiActionNames#isStrangerSafe} actions run
     *                    and approach does not set permanent FOLLOW
     */
    public static List<String> execute(FabricCompanionEntity companion, ServerPlayer owner,
                                       List<CompanionAiAction> actions, CompanionAiSettings settings,
                                       ServerPlayer approachTarget, boolean fullControl) {
        return execute(companion, owner, actions, settings,
                fullControl ? CompanionAiActionTrust.OWNER : CompanionAiActionTrust.STRANGER,
                approachTarget);
    }

    public static List<String> execute(FabricCompanionEntity companion, ServerPlayer owner,
                                       List<CompanionAiAction> actions, CompanionAiSettings settings,
                                       CompanionAiActionTrust trust, ServerPlayer socialTarget) {
        List<String> results = new ArrayList<>();
        if (companion == null || companion.isRemoved() || actions == null || actions.isEmpty()) {
            return results;
        }
        if (owner == null || !companion.isOwnedBy(owner)) {
            results.add("not_owner");
            return results;
        }
        if (!settings.enableAiActions()) {
            results.add("actions_disabled");
            return results;
        }
        CompanionAiActionTrust effective = trust == null ? CompanionAiActionTrust.OWNER : trust;
        if (!effective.allowsActions()) {
            results.add("trust_none");
            return results;
        }
        if (!(companion.level() instanceof ServerLevel level)) {
            return results;
        }
        int reach = settings.aiActionReach();
        boolean fullControl = effective.fullControl();
        boolean mayBuild = owner.getAbilities().mayBuild && fullControl;
        for (CompanionAiAction action : effective.filter(actions)) {
            results.add(runOne(companion, owner, level, action, reach, mayBuild, socialTarget, fullControl));
        }
        return results;
    }

    private static String runOne(FabricCompanionEntity companion, ServerPlayer owner, ServerLevel level,
                                 CompanionAiAction action, int reach, boolean mayBuild,
                                 ServerPlayer approachTarget, boolean fullControl) {
        String name = action.name();
        return switch (name) {
            case CompanionAiActionNames.FOLLOW, "follow_owner" -> {
                companion.setMode(FabricCompanionMode.FOLLOW);
                companion.getTaskQueue().clear();
                companion.clearPlayMode();
                yield "follow";
            }
            case CompanionAiActionNames.STOP, CompanionAiActionNames.STAY -> {
                companion.setMode(FabricCompanionMode.STAY);
                companion.getNavigation().stop();
                companion.clearPlayMode();
                yield "stay";
            }
            case CompanionAiActionNames.SIT -> {
                companion.setMode(FabricCompanionMode.SIT);
                companion.getNavigation().stop();
                companion.clearPlayMode();
                yield "sit";
            }
            case CompanionAiActionNames.WANDER -> {
                companion.setMode(FabricCompanionMode.WANDER);
                companion.clearPlayMode();
                yield "wander";
            }
            case CompanionAiActionNames.GOTO -> gotoPos(companion, action);
            case CompanionAiActionNames.COME_HERE, CompanionAiActionNames.RUN_AT_PLAYER -> {
                ServerPlayer approach = approachTarget != null ? approachTarget : owner;
                companion.startPlay(CompanionPlayMode.RUN_AT_PLAYER, 100);
                if (fullControl) {
                    companion.setMode(FabricCompanionMode.FOLLOW);
                }
                if (approach != null) {
                    companion.getNavigation().moveTo(approach, 1.35d);
                    companion.getLookControl().setLookAt(approach, 10.0f, companion.getMaxHeadXRot());
                }
                yield name;
            }
            case CompanionAiActionNames.HIDE -> {
                companion.startPlay(CompanionPlayMode.HIDE, 200);
                yield "hide";
            }
            case CompanionAiActionNames.SEEK, "seeker" -> {
                companion.startPlay(CompanionPlayMode.SEEK, 300);
                yield "seek";
            }
            case CompanionAiActionNames.HIDE_AND_SEEK -> {
                String role = action.argOr("role", "hider");
                if (role.equalsIgnoreCase("seeker") || role.equalsIgnoreCase("seek")) {
                    companion.startPlay(CompanionPlayMode.SEEK, 300);
                    yield "hide_and_seek:seek";
                }
                companion.startPlay(CompanionPlayMode.HIDE, 200);
                yield "hide_and_seek:hide";
            }
            case CompanionAiActionNames.DANCE -> {
                companion.startPlay(CompanionPlayMode.DANCE, 80);
                yield "dance";
            }
            case CompanionAiActionNames.PEEKABOO -> {
                companion.startPlay(CompanionPlayMode.PEEKABOO, 60);
                yield "peekaboo";
            }
            case CompanionAiActionNames.PLAY_STOP -> {
                companion.clearPlayMode();
                yield "play_stop";
            }
            case CompanionAiActionNames.MINE -> {
                if (!mayBuild) {
                    yield "mine:adventure_blocked";
                }
                yield mine(companion, level, action, reach);
            }
            case CompanionAiActionNames.PLACE, CompanionAiActionNames.BUILD -> {
                if (!mayBuild) {
                    yield "place:adventure_blocked";
                }
                yield place(companion, level, action, reach);
            }
            case CompanionAiActionNames.CRAFT -> craft(companion, level, action);
            case CompanionAiActionNames.PICKUP, CompanionAiActionNames.TAKE -> pickup(companion, level, reach);
            case CompanionAiActionNames.USE_ITEM -> useItem(companion, level);
            case CompanionAiActionNames.EQUIP -> equip(companion, action);
            case CompanionAiActionNames.MOVE_ITEM -> moveItem(companion, action);
            case CompanionAiActionNames.DROP -> drop(companion, level, action);
            case CompanionAiActionNames.SELECT_HOTBAR -> selectHotbar(companion, action);
            case CompanionAiActionNames.SAY -> {
                String line = action.argOr("text", action.argOr("message", ""));
                if (!line.isBlank()) {
                    companion.speakLine(line);
                }
                yield "say";
            }
            case CompanionAiActionNames.CLAIM_CHUNK -> {
                if (!fullControl) {
                    yield "claim:owner_only";
                }
                yield claimOrUnclaim(companion, owner, level, action, true);
            }
            case CompanionAiActionNames.UNCLAIM_CHUNK -> {
                if (!fullControl) {
                    yield "unclaim:owner_only";
                }
                yield claimOrUnclaim(companion, owner, level, action, false);
            }
            default -> "unknown:" + name;
        };
    }

    private static String claimOrUnclaim(FabricCompanionEntity companion, ServerPlayer owner, ServerLevel level,
                                         CompanionAiAction action, boolean claim) {
        if (!com.azscompanions.compat.ftb.FtbCompat.aiClaimEnabled()) {
            return (claim ? "claim" : "unclaim") + ":disabled";
        }
        BlockPos foot = companion.blockPosition();
        int cx;
        int cz;
        if (action.arg("chunkx") != null || action.arg("chunk_x") != null) {
            cx = action.argInt("chunkx", action.argInt("chunk_x", foot.getX() >> 4));
            cz = action.argInt("chunkz", action.argInt("chunk_z", foot.getZ() >> 4));
        } else {
            int blockX = action.arg("x") != null ? action.argInt("x", foot.getX()) : foot.getX();
            int blockZ = action.arg("z") != null ? action.argInt("z", foot.getZ()) : foot.getZ();
            cx = blockX >> 4;
            cz = blockZ >> 4;
        }
        String result = claim
                ? com.azscompanions.compat.ftb.FtbCompat.claimChunkAsOwner(owner, level.dimension(), cx, cz)
                : com.azscompanions.compat.ftb.FtbCompat.unclaimChunkAsOwner(owner, level.dimension(), cx, cz);
        return (claim ? "claim" : "unclaim") + ":" + result;
    }

    private static String gotoPos(FabricCompanionEntity companion, CompanionAiAction action) {
        if (!action.args().containsKey("x") || !action.args().containsKey("z")) {
            return "goto:missing_coords";
        }
        int x = action.argInt("x", companion.getBlockX());
        int y = action.argInt("y", companion.getBlockY());
        int z = action.argInt("z", companion.getBlockZ());
        BlockPos pos = new BlockPos(x, y, z);
        if (companion.blockPosition().distManhattan(pos) > 48) {
            return "goto:too_far";
        }
        companion.setMode(FabricCompanionMode.FOLLOW);
        companion.getNavigation().moveTo(x + 0.5, y, z + 0.5, 1.1d);
        return "goto";
    }

    private static String mine(FabricCompanionEntity companion, ServerLevel level, CompanionAiAction action, int reach) {
        if (action.args().containsKey("x") && action.args().containsKey("y") && action.args().containsKey("z")) {
            BlockPos pos = new BlockPos(action.argInt("x", 0), action.argInt("y", 0), action.argInt("z", 0));
            if (companion.blockPosition().distManhattan(pos) > reach + 2) {
                companion.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0d);
                companion.getTaskQueue().enqueue(new FabricMineTask(pos));
                return "mine:queued_path";
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(Blocks.BEDROCK) || state.is(BlockTags.WITHER_IMMUNE)) {
                return "mine:blocked";
            }
            if (!com.azscompanions.util.FabricProtectionHelper.canCompanionModify(level, pos, companion)) {
                return "mine:denied";
            }
            if (level.destroyBlock(pos, true, companion)) {
                return "mine:ok";
            }
            companion.getTaskQueue().enqueue(new FabricMineTask(pos));
            return "mine:queued";
        }
        companion.getTaskQueue().enqueue(new FabricMineTask(action.argInt("radius", 8)));
        return "mine:queued_scan";
    }

    private static String place(FabricCompanionEntity companion, ServerLevel level, CompanionAiAction action, int reach) {
        int x = action.argInt("x", companion.getBlockX());
        int y = action.argInt("y", companion.getBlockY());
        int z = action.argInt("z", companion.getBlockZ());
        BlockPos pos = new BlockPos(x, y, z);
        if (companion.blockPosition().distManhattan(pos) > reach + 3) {
            companion.getNavigation().moveTo(x + 0.5, y, z + 0.5, 1.0d);
            companion.getTaskQueue().enqueue(new FabricBuildTask(List.of(pos), action.arg("item")));
            return "place:queued_path";
        }
        if (!level.getBlockState(pos).canBeReplaced()) {
            return "place:occupied";
        }
        if (!com.azscompanions.util.FabricProtectionHelper.canCompanionModify(level, pos, companion)) {
            return "place:denied";
        }
        ItemStack material = findPlaceStack(companion, action.arg("item"));
        if (material.isEmpty() || !(material.getItem() instanceof BlockItem blockItem)) {
            companion.getTaskQueue().enqueue(new FabricBuildTask(List.of(pos), action.arg("item")));
            return "place:queued_or_missing";
        }
        BlockState placeState = blockItem.getBlock().defaultBlockState();
        if (level.setBlock(pos, placeState, Block.UPDATE_ALL)) {
            material.shrink(1);
            return "place:ok";
        }
        return "place:failed";
    }

    private static ItemStack findPlaceStack(FabricCompanionEntity companion, String itemId) {
        FabricCompanionInventory inv = companion.getCompanionInventory();
        ResourceLocation want = itemId == null || itemId.isBlank() ? null : ResourceLocation.tryParse(itemId);
        ItemStack main = inv.getMainHand();
        if (!main.isEmpty() && main.getItem() instanceof BlockItem) {
            if (want == null || want.equals(BuiltInRegistries.ITEM.getKey(main.getItem()))) {
                return main;
            }
        }
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
                continue;
            }
            if (want == null || want.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static String craft(FabricCompanionEntity companion, ServerLevel level, CompanionAiAction action) {
        String recipe = action.argOr("recipe", action.arg("recipe_id"));
        String item = action.argOr("item", action.argOr("result", action.arg("output")));
        ResourceLocation recipeId = recipe != null && !recipe.isBlank() ? ResourceLocation.tryParse(recipe) : null;
        ResourceLocation itemId = null;
        if (item != null && !item.isBlank()) {
            itemId = ResourceLocation.tryParse(item.contains(":") ? item : "minecraft:" + item);
        }
        if (recipeId == null && itemId == null) {
            return "craft:missing_item_or_recipe";
        }
        companion.getTaskQueue().enqueue(new FabricCraftTask(recipeId, itemId));
        return "craft:queued";
    }

    private static String pickup(FabricCompanionEntity companion, ServerLevel level, int reach) {
        AABB box = companion.getBoundingBox().inflate(Math.max(reach, 4));
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box, e -> e.isAlive() && !e.getItem().isEmpty());
        items.sort(Comparator.comparingDouble(e -> e.distanceToSqr(companion)));
        int taken = 0;
        for (ItemEntity entity : items) {
            if (taken >= 8) {
                break;
            }
            ItemStack stack = entity.getItem();
            ItemStack leftover = companion.getCompanionInventory().insertItemAuto(stack);
            if (leftover.getCount() < stack.getCount()) {
                taken++;
                if (leftover.isEmpty()) {
                    entity.discard();
                } else {
                    entity.setItem(leftover);
                }
            }
            if (companion.getCompanionInventory().isFull()) {
                break;
            }
        }
        return taken > 0 ? "pickup:" + taken : "pickup:none";
    }

    private static String useItem(FabricCompanionEntity companion, ServerLevel level) {
        ItemStack stack = companion.getCompanionInventory().getMainHand();
        if (stack.isEmpty()) {
            return "use_item:empty";
        }
        companion.swing(InteractionHand.MAIN_HAND);
        if (stack.has(net.minecraft.core.component.DataComponents.FOOD)) {
            ItemStack after = stack.finishUsingItem(level, companion);
            companion.getCompanionInventory().setItem(FabricCompanionInventory.MAIN_HAND, after);
            return "use_item:food";
        }
        return "use_item:swing";
    }

    private static String equip(FabricCompanionEntity companion, CompanionAiAction action) {
        FabricCompanionInventory inv = companion.getCompanionInventory();
        String slot = action.argOr("slot", "mainhand").toLowerCase();
        int from = action.argInt("from", action.argInt("slot_from", 0));
        if (from < 0 || from >= inv.getContainerSize()) {
            return "equip:bad_from";
        }
        int to = switch (slot) {
            case "offhand", "off" -> FabricCompanionInventory.OFF_HAND;
            case "head", "helmet" -> FabricCompanionInventory.HEAD;
            case "chest", "chestplate" -> FabricCompanionInventory.CHEST;
            case "legs", "leggings" -> FabricCompanionInventory.LEGS;
            case "feet", "boots" -> FabricCompanionInventory.FEET;
            default -> FabricCompanionInventory.MAIN_HAND;
        };
        ItemStack moving = inv.getItem(from).copy();
        ItemStack dest = inv.getItem(to).copy();
        inv.setItem(to, moving);
        inv.setItem(from, dest);
        return "equip:" + slot;
    }

    private static String moveItem(FabricCompanionEntity companion, CompanionAiAction action) {
        FabricCompanionInventory inv = companion.getCompanionInventory();
        int from = action.argInt("from", -1);
        int to = action.argInt("to", -1);
        if (from < 0 || to < 0 || from >= inv.getContainerSize() || to >= inv.getContainerSize()) {
            return "move_item:bad_slots";
        }
        ItemStack a = inv.getItem(from).copy();
        ItemStack b = inv.getItem(to).copy();
        inv.setItem(to, a);
        inv.setItem(from, b);
        return "move_item";
    }

    private static String drop(FabricCompanionEntity companion, ServerLevel level, CompanionAiAction action) {
        FabricCompanionInventory inv = companion.getCompanionInventory();
        int slot = action.argInt("slot", FabricCompanionInventory.MAIN_HAND);
        if (slot < 0 || slot >= inv.getContainerSize()) {
            return "drop:bad_slot";
        }
        ItemStack stack = inv.getItem(slot);
        if (stack.isEmpty()) {
            return "drop:empty";
        }
        ItemStack dropped = stack.copy();
        inv.setItem(slot, ItemStack.EMPTY);
        level.addFreshEntity(new ItemEntity(level, companion.getX(), companion.getY() + 0.5, companion.getZ(), dropped));
        return "drop:ok";
    }

    private static String selectHotbar(FabricCompanionEntity companion, CompanionAiAction action) {
        int index = action.argInt("index", action.argInt("slot", 0));
        FabricCompanionInventory inv = companion.getCompanionInventory();
        int from = Math.max(0, Math.min(FabricCompanionInventory.BACKPACK_SIZE - 1, index));
        ItemStack selected = inv.getItem(from).copy();
        ItemStack main = inv.getMainHand().copy();
        inv.setItem(FabricCompanionInventory.MAIN_HAND, selected);
        inv.setItem(from, main);
        return "select_hotbar:" + from;
    }
}
