package com.azscompanions.util;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.task.CraftRecipeCatalog;
import com.azscompanions.task.tasks.CraftTask;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * When a gather target needs a tool the companion lacks: ask the owner and optionally craft one.
 */
public final class CompanionToolAsk {
    private static final int ASK_COOLDOWN_TICKS = 200;

    private CompanionToolAsk() {
    }

    /**
     * @return true if companion has a usable tool (or bare-hand is enough)
     */
    public static boolean ensureToolOrAsk(CompanionEntity companion, ServerLevel level, BlockState state) {
        if (ToolSelectionHelper.equipBestTool(companion, state)) {
            return true;
        }
        if (!state.requiresCorrectToolForDrops()) {
            return true;
        }
        if (companion.getTaskQueue().getActive() instanceof CraftTask) {
            return false;
        }
        Item preferred = preferredToolItem(state);
        if (preferred != null && tryPreemptCraft(companion, level, preferred)) {
            return false;
        }
        maybeAskOwner(companion, preferred);
        return false;
    }

    @Nullable
    private static Item preferredToolItem(BlockState state) {
        float pick = Items.STONE_PICKAXE.getDefaultInstance().getDestroySpeed(state);
        float axe = Items.STONE_AXE.getDefaultInstance().getDestroySpeed(state);
        float shovel = Items.STONE_SHOVEL.getDefaultInstance().getDestroySpeed(state);
        if (pick >= axe && pick >= shovel) {
            return Items.STONE_PICKAXE;
        }
        if (axe >= shovel) {
            return Items.STONE_AXE;
        }
        return Items.STONE_SHOVEL;
    }

    private static boolean tryPreemptCraft(CompanionEntity companion, ServerLevel level, Item tool) {
        if (!companion.hasPermission("craft")) {
            return false;
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(tool);
        CraftTask craft = new CraftTask();
        String catalogRecipe = CraftRecipeCatalog.firstRecipeForResult(itemId.toString()).orElse(null);
        if (catalogRecipe != null) {
            Identifier rid = Identifier.tryParse(catalogRecipe);
            if (rid != null) {
                craft.recipe(rid);
            } else {
                craft.forResultItem(level, itemId);
            }
        } else {
            craft.forResultItem(level, itemId);
        }
        companion.getTaskQueue().enqueue(craft);
        if (companion.getOwner() instanceof ServerPlayer owner) {
            owner.sendSystemMessage(Component.literal(
                    companion.getChatDisplayName() + " — crafting " + itemId + "…"));
        }
        return true;
    }

    private static void maybeAskOwner(CompanionEntity companion, @Nullable Item tool) {
        if (companion.tickCount % ASK_COOLDOWN_TICKS != 0) {
            return;
        }
        if (!(companion.getOwner() instanceof ServerPlayer owner)) {
            return;
        }
        String name = tool == null ? "a tool"
                : BuiltInRegistries.ITEM.getKey(tool).toString();
        owner.sendSystemMessage(Component.literal(
                companion.getChatDisplayName() + " needs " + name
                        + " — please give one, or put craft ingredients in my inventory."));
    }
}
