package com.azscompanions.event;

import com.azscompanions.ai.CompanionInventoryWatchSupport;
import com.azscompanions.ai.CompanionNotableItemSupport;
import com.azscompanions.ai.CompanionChatEventSupport;
import com.azscompanions.ai.CompanionRecentActionKind;
import com.azscompanions.ai.CompanionRecentActionMemory;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

/**
 * Fabric: darkness + inventory finds / craft-ready each second; damage; disconnect cleanup.
 * Explosions / crafts also arrive via mixins calling {@link #onExplosion} / {@link #onPlayerCrafted}.
 */
public final class FabricCompanionRecentActionEvents {
    public static final double EXPLOSION_RANGE = 24.0d;

    private FabricCompanionRecentActionEvents() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickPlayer(player);
            }
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player && amount > 0.0f) {
                onPlayerHurt(player);
            }
            return true;
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            clearPlayer(handler.getPlayer());
        });
    }

    public static void onExplosion(ServerLevel level, double x, double y, double z) {
        if (level == null || level.isClientSide) {
            return;
        }
        long time = level.getGameTime();
        AABB box = new AABB(x, y, z, x, y, z).inflate(EXPLOSION_RANGE);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            CompanionChatEventSupport.observe(
                    player.getUUID(), time, CompanionRecentActionKind.EXPLOSION,
                    "an explosion nearby (TNT or blast)", null, true);
        }
    }

    public static void onPlayerCrafted(ServerPlayer player, ItemStack crafted) {
        if (player == null || player.level().isClientSide || crafted == null || crafted.isEmpty()) {
            return;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(crafted.getItem());
        if (key == null) {
            return;
        }
        String id = key.toString();
        String pretty = CompanionNotableItemSupport.prettyName(id);
        CompanionChatEventSupport.observe(
                player.getUUID(), player.level().getGameTime(),
                CompanionRecentActionKind.ITEM_CRAFT,
                "player just crafted " + pretty, id, true);
    }

    public static void onPlayerHurt(ServerPlayer player) {
        if (player == null || player.level().isClientSide) {
            return;
        }
        CompanionChatEventSupport.observe(
                    player.getUUID(), player.level().getGameTime(),
                    CompanionRecentActionKind.DAMAGE,
                    "player took damage", null, true);
    }

    public static void tickPlayer(ServerPlayer player) {
        if (player == null || player.level().isClientSide || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        long time = level.getGameTime();
        int block = level.getBrightness(LightLayer.BLOCK, player.blockPosition());
        int sky = level.getBrightness(LightLayer.SKY, player.blockPosition());
        boolean dark = Math.max(block, sky) <= CompanionRecentActionMemory.DARK_LIGHT_THRESHOLD;
        CompanionChatEventSupport.observeDarknessEnter(player.getUUID(), time, dark);

        Map<String, Integer> counts = countTrackedItems(player.getInventory());
        CompanionInventoryWatchSupport.observeCounts(player.getUUID(), time, counts);
    }

    public static Map<String, Integer> countTrackedItems(Inventory inv) {
        Map<String, Integer> counts = new HashMap<>();
        if (inv == null) {
            return counts;
        }
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (key == null) {
                continue;
            }
            String id = key.toString();
            if (!CompanionInventoryWatchSupport.shouldTrackCount(id)) {
                continue;
            }
            counts.merge(CompanionNotableItemSupport.normalizeId(id), stack.getCount(), Integer::sum);
        }
        return counts;
    }

    public static void clearPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CompanionRecentActionMemory.clearPlayer(player.getUUID());
        CompanionInventoryWatchSupport.clearPlayer(player.getUUID());
    }
}
