package com.azscompanions.ai;

import com.azscompanions.config.ServerConfig;
import com.azscompanions.data.ModTags;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.entity.CompanionPlayMode;
import com.azscompanions.entity.inventory.CompanionInventory;
import com.azscompanions.task.tasks.BuildTask;
import com.azscompanions.task.tasks.CraftTask;
import com.azscompanions.task.tasks.MineTask;
import com.azscompanions.util.ToolSelectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Executes structured LLM actions on a NeoForge companion (mine/craft/build/move/play/inventory).
 */
public final class CompanionAiActionExecutor {
    private CompanionAiActionExecutor() {
    }

    public static List<String> execute(CompanionEntity companion, ServerPlayer owner,
                                       List<CompanionAiAction> actions, CompanionAiSettings settings) {
        return execute(companion, owner, actions, settings, owner, true);
    }

    /**
     * @param approachTarget player to briefly approach for {@code come_here}/{@code run_at_player}
     * @param fullControl when false, only {@link CompanionAiActionNames#isStrangerSafe} actions run
     *                    and approach does not set permanent FOLLOW
     */
    public static List<String> execute(CompanionEntity companion, ServerPlayer owner,
                                       List<CompanionAiAction> actions, CompanionAiSettings settings,
                                       ServerPlayer approachTarget, boolean fullControl) {
        return execute(companion, owner, actions, settings,
                fullControl ? CompanionAiActionTrust.OWNER : CompanionAiActionTrust.STRANGER,
                approachTarget);
    }

    /**
     * @param trust filters which action names run; {@link CompanionAiActionTrust#STRANGER} blocks grief/inventory
     * @param socialTarget for stranger {@code come_here}/{@code run_at_player} approach target (speaker)
     */
    public static List<String> execute(CompanionEntity companion, ServerPlayer owner,
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
        if (true) { // AI Mode removed — LLM world tools disabled
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

    private static String runOne(CompanionEntity companion, ServerPlayer owner, ServerLevel level,
                                 CompanionAiAction action, int reach, boolean mayBuild,
                                 ServerPlayer approachTarget, boolean fullControl) {
        String name = action.name();
        return switch (name) {
            case CompanionAiActionNames.FOLLOW, "follow_owner" -> {
                companion.setMode(CompanionMode.FOLLOW);
                companion.getTaskQueue().clear();
                companion.clearPlayMode();
                yield "follow";
            }
            case CompanionAiActionNames.STOP, CompanionAiActionNames.STAY -> {
                companion.setMode(CompanionMode.STAY);
                companion.getNavigation().stop();
                companion.clearPlayMode();
                yield "stay";
            }
            case CompanionAiActionNames.SIT -> {
                companion.setMode(CompanionMode.SIT);
                companion.getNavigation().stop();
                companion.clearPlayMode();
                yield "sit";
            }
            case CompanionAiActionNames.WANDER -> {
                companion.setMode(CompanionMode.WANDER);
                companion.clearPlayMode();
                yield "wander";
            }
            case CompanionAiActionNames.GOTO -> gotoPos(companion, action, reach);
            case CompanionAiActionNames.COME_HERE, CompanionAiActionNames.RUN_AT_PLAYER -> {
                ServerPlayer approach = approachTarget != null ? approachTarget : owner;
                companion.startPlay(CompanionPlayMode.RUN_AT_PLAYER, 100);
                if (fullControl) {
                    companion.setMode(CompanionMode.FOLLOW);
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

    private static String claimOrUnclaim(CompanionEntity companion, ServerPlayer owner, ServerLevel level,
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

    private static String gotoPos(CompanionEntity companion, CompanionAiAction action, int reach) {
        if (!action.args().containsKey("x") || !action.args().containsKey("z")) {
            return "goto:missing_coords";
        }
        int x = action.argInt("x", companion.getBlockX());
        int y = action.argInt("y", companion.getBlockY());
        int z = action.argInt("z", companion.getBlockZ());
        BlockPos pos = new BlockPos(x, y, z);
        int maxDist = companion.getLeaderUuid() != null
                ? (int) CompanionAiRuntime.get().settings().effectiveChildLeashRadius() + 4
                : 48;
        if (companion.blockPosition().distManhattan(pos) > maxDist) {
            return "goto:too_far";
        }
        companion.setMode(CompanionMode.FOLLOW);
        companion.getNavigation().moveTo(x + 0.5, y, z + 0.5, 1.1d);
        return "goto";
    }

    private static String mine(CompanionEntity companion, ServerLevel level, CompanionAiAction action, int reach) {
        if (!ServerConfig.ALLOW_GRIEFING.get() && !companion.hasPermission("gather")) {
            // still allow if gather permission — griefing gate is in canBreakBlock
        }
        if (action.args().containsKey("x") && action.args().containsKey("y") && action.args().containsKey("z")) {
            BlockPos pos = new BlockPos(action.argInt("x", 0), action.argInt("y", 0), action.argInt("z", 0));
            if (companion.blockPosition().distManhattan(pos) > reach + 2) {
                companion.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0d);
                companion.getTaskQueue().enqueue(new MineTask().at(pos));
                return "mine:queued_path";
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(ModTags.Blocks.BLACKLISTED_BLOCKS)) {
                return "mine:blocked";
            }
            if (!companion.canBreakBlock(pos)) {
                return "mine:denied";
            }
            ToolSelectionHelper.equipBestTool(companion, state);
            if (level.destroyBlock(pos, true, companion)) {
                return "mine:ok";
            }
            companion.getTaskQueue().enqueue(new MineTask().at(pos));
            return "mine:queued";
        }
        companion.getTaskQueue().enqueue(new MineTask().withRadius(action.argInt("radius", 8)));
        return "mine:queued_scan";
    }

    private static String place(CompanionEntity companion, ServerLevel level, CompanionAiAction action, int reach) {
        if (!companion.hasPermission("build")) {
            return "place:no_permission";
        }
        int x = action.argInt("x", companion.getBlockX());
        int y = action.argInt("y", companion.getBlockY());
        int z = action.argInt("z", companion.getBlockZ());
        BlockPos pos = new BlockPos(x, y, z);
        if (companion.blockPosition().distManhattan(pos) > reach + 3) {
            companion.getNavigation().moveTo(x + 0.5, y, z + 0.5, 1.0d);
            companion.getTaskQueue().enqueue(new BuildTask().withPlan(List.of(pos)));
            return "place:queued_path";
        }
        if (!level.getBlockState(pos).canBeReplaced() && !companion.canBreakBlock(pos)) {
            return "place:occupied";
        }
        ItemStack material = findPlaceStack(companion, action.arg("item"));
        if (material.isEmpty() || !(material.getItem() instanceof BlockItem blockItem)) {
            companion.getTaskQueue().enqueue(new BuildTask().withPlan(List.of(pos)));
            return "place:queued_or_missing";
        }
        BlockState placeState = blockItem.getBlock().defaultBlockState();
        if (level.setBlock(pos, placeState, Block.UPDATE_ALL)) {
            material.shrink(1);
            return "place:ok";
        }
        return "place:failed";
    }

    private static ItemStack findPlaceStack(CompanionEntity companion, String itemId) {
        CompanionInventory inv = companion.getCompanionInventory();
        ResourceLocation want = itemId == null || itemId.isBlank() ? null : ResourceLocation.tryParse(itemId);
        // Prefer main hand
        ItemStack main = inv.getMainHand();
        if (!main.isEmpty() && main.getItem() instanceof BlockItem) {
            if (want == null || want.equals(BuiltInRegistries.ITEM.getKey(main.getItem()))) {
                return main;
            }
        }
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
                continue;
            }
            if (want == null || want.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static String craft(CompanionEntity companion, ServerLevel level, CompanionAiAction action) {
        if (!companion.hasPermission("craft")) {
            return "craft:no_permission";
        }
        CraftTask task = new CraftTask();
        String recipe = action.argOr("recipe", action.arg("recipe_id"));
        String item = action.argOr("item", action.argOr("result", action.arg("output")));
        if (recipe != null && !recipe.isBlank()) {
            ResourceLocation id = ResourceLocation.tryParse(recipe);
            if (id != null) {
                task.recipe(id);
            }
        } else if (item != null && !item.isBlank()) {
            ResourceLocation id = ResourceLocation.tryParse(item.contains(":") ? item : "minecraft:" + item);
            task.forResultItem(level, id);
        } else {
            return "craft:missing_item_or_recipe";
        }
        companion.getTaskQueue().enqueue(task);
        return "craft:queued";
    }

    private static String pickup(CompanionEntity companion, ServerLevel level, int reach) {
        AABB box = companion.getBoundingBox().inflate(Math.max(reach, 4));
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box, e -> e.isAlive() && !e.getItem().isEmpty());
        items.sort(Comparator.comparingDouble(e -> e.distanceToSqr(companion)));
        int taken = 0;
        for (ItemEntity entity : items) {
            if (taken >= 8) {
                break;
            }
            ItemStack stack = entity.getItem();
            ItemStack leftover = companion.getCompanionInventory().insertItemAuto(stack, false);
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

    private static String useItem(CompanionEntity companion, ServerLevel level) {
        ItemStack stack = companion.getCompanionInventory().getMainHand();
        if (stack.isEmpty()) {
            return "use_item:empty";
        }
        companion.swing(InteractionHand.MAIN_HAND);
        if (stack.has(net.minecraft.core.component.DataComponents.FOOD)) {
            ItemStack after = stack.finishUsingItem(level, companion);
            companion.getCompanionInventory().setStackInSlot(CompanionInventory.MAIN_HAND, after);
            return "use_item:food";
        }
        // Generic "use" — swing held tool / item (blocks placed via place tool)
        return "use_item:swing";
    }

    private static String equip(CompanionEntity companion, CompanionAiAction action) {
        CompanionInventory inv = companion.getCompanionInventory();
        String slot = action.argOr("slot", "mainhand").toLowerCase();
        int from = action.argInt("from", action.argInt("slot_from", 0));
        if (from < 0 || from >= inv.getSlots()) {
            return "equip:bad_from";
        }
        int to = switch (slot) {
            case "offhand", "off" -> CompanionInventory.OFF_HAND;
            case "head", "helmet" -> CompanionInventory.HEAD;
            case "chest", "chestplate" -> CompanionInventory.CHEST;
            case "legs", "leggings" -> CompanionInventory.LEGS;
            case "feet", "boots" -> CompanionInventory.FEET;
            default -> CompanionInventory.MAIN_HAND;
        };
        ItemStack moving = inv.getStackInSlot(from).copy();
        ItemStack dest = inv.getStackInSlot(to).copy();
        inv.setStackInSlot(to, moving);
        inv.setStackInSlot(from, dest);
        return "equip:" + slot;
    }

    private static String moveItem(CompanionEntity companion, CompanionAiAction action) {
        CompanionInventory inv = companion.getCompanionInventory();
        int from = action.argInt("from", -1);
        int to = action.argInt("to", -1);
        if (from < 0 || to < 0 || from >= inv.getSlots() || to >= inv.getSlots()) {
            return "move_item:bad_slots";
        }
        ItemStack a = inv.getStackInSlot(from).copy();
        ItemStack b = inv.getStackInSlot(to).copy();
        inv.setStackInSlot(to, a);
        inv.setStackInSlot(from, b);
        return "move_item";
    }

    private static String drop(CompanionEntity companion, ServerLevel level, CompanionAiAction action) {
        CompanionInventory inv = companion.getCompanionInventory();
        int slot = action.argInt("slot", CompanionInventory.MAIN_HAND);
        if (slot < 0 || slot >= inv.getSlots()) {
            return "drop:bad_slot";
        }
        ItemStack stack = inv.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return "drop:empty";
        }
        ItemStack dropped = stack.copy();
        inv.setStackInSlot(slot, ItemStack.EMPTY);
        ItemEntity entity = new ItemEntity(level, companion.getX(), companion.getY() + 0.5, companion.getZ(), dropped);
        level.addFreshEntity(entity);
        return "drop:ok";
    }

    private static String selectHotbar(CompanionEntity companion, CompanionAiAction action) {
        int index = action.argInt("index", action.argInt("slot", 0));
        CompanionInventory inv = companion.getCompanionInventory();
        int from = Math.max(0, Math.min(CompanionInventory.BACKPACK_SIZE - 1, index));
        ItemStack selected = inv.getStackInSlot(from).copy();
        ItemStack main = inv.getMainHand().copy();
        inv.setStackInSlot(CompanionInventory.MAIN_HAND, selected);
        inv.setStackInSlot(from, main);
        return "select_hotbar:" + from;
    }
}
