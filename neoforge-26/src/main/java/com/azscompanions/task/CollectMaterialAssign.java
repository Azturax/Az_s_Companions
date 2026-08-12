package com.azscompanions.task;

import com.azscompanions.deposit.DepositChestSelection;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.task.tasks.CollectMaterialTask;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Shared assign / status helpers for material gather goals (commands + CCI).
 */
public final class CollectMaterialAssign {
    private CollectMaterialAssign() {
    }

    public static int assign(ServerPlayer player, CompanionEntity companion,
                             String itemId, int count, @Nullable String depositMode) {
        if (player == null || companion == null) {
            return 0;
        }
        Item item = resolveItem(itemId);
        if (item == null || item == Items.AIR) {
            player.sendSystemMessage(Component.literal("Unknown item: " + itemId));
            return 0;
        }
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        if (!GatherItemCatalog.isKnown(id)) {
            player.sendSystemMessage(Component.literal("Item not in gather catalog: " + id));
            return 0;
        }
        BlockPos chest = resolveChest(player, depositMode);
        CollectMaterialTask task = new CollectMaterialTask().of(item, count).depositAt(chest);
        companion.getTaskQueue().clear();
        companion.getTaskQueue().enqueue(task);
        String chestLabel;
        if (chest != null) {
            chestLabel = chest.toShortString();
        } else {
            int selected = DepositChestSelection.of(player.getUUID()).size();
            chestLabel = selected > 0
                    ? "nearest of " + selected + " selected chest(s)"
                    : "nearest chest";
        }
        player.sendSystemMessage(Component.literal(
                companion.getChatDisplayName() + " will collect " + count + " × " + id
                        + " → " + chestLabel));
        return 1;
    }

    public static int status(ServerPlayer player, CompanionEntity companion) {
        if (companion.getTaskQueue().getActive() instanceof CollectMaterialTask task) {
            player.sendSystemMessage(Component.literal(
                    companion.getChatDisplayName() + " — " + task.progressLabel(companion)
                            + " (" + task.progress() + "%)"));
            return 1;
        }
        player.sendSystemMessage(Component.literal(
                companion.getChatDisplayName() + " — "
                        + companion.getTaskQueue().describeActive().orElse("no collect_material task")));
        return 1;
    }

    public static int cancel(ServerPlayer player, CompanionEntity companion) {
        companion.getTaskQueue().clear();
        player.sendSystemMessage(Component.literal(
                companion.getChatDisplayName() + " — gather task cancelled"));
        return 1;
    }

    @Nullable
    public static Item resolveItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        String raw = itemId.trim().toLowerCase();
        if (!raw.contains(":")) {
            raw = "minecraft:" + raw;
        }
        Identifier id = Identifier.tryParse(raw);
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    }

    @Nullable
    private static BlockPos resolveChest(ServerPlayer player, @Nullable String depositMode) {
        if (depositMode == null || depositMode.isBlank()
                || depositMode.equalsIgnoreCase("nearest")
                || depositMode.equalsIgnoreCase("chest")
                || depositMode.equalsIgnoreCase("auto")) {
            return null; // task finds nearest
        }
        if (depositMode.equalsIgnoreCase("look") || depositMode.equalsIgnoreCase("lookat")
                || depositMode.equalsIgnoreCase("looking")) {
            return rayTraceBlock(player, 8.0d);
        }
        return null;
    }

    @Nullable
    private static BlockPos rayTraceBlock(ServerPlayer player, double reach) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(reach));
        BlockHitResult hit = player.level().clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getBlockPos();
        }
        return null;
    }
}
