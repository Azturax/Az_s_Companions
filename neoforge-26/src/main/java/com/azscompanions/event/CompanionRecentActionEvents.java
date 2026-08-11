package com.azscompanions.event;

import com.azscompanions.ai.CompanionInventoryWatchSupport;
import com.azscompanions.ai.CompanionNotableItemSupport;
import com.azscompanions.ai.CompanionRecentActionKind;
import com.azscompanions.ai.CompanionRecentActionMemory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;

/** NeoForge 26.2: explosions, crafts, pickups, darkness, damage → recent-action memory. */
public final class CompanionRecentActionEvents {
    public static final double EXPLOSION_RANGE = 24.0d;

    private CompanionRecentActionEvents() {
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.isClientSide()) {
            return;
        }
        var exp = event.getExplosion();
        double x = exp.center().x;
        double y = exp.center().y;
        double z = exp.center().z;
        long time = level.getGameTime();
        AABB box = new AABB(x, y, z, x, y, z).inflate(EXPLOSION_RANGE);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            CompanionRecentActionMemory.record(
                    player.getUUID(), time, CompanionRecentActionKind.EXPLOSION,
                    "an explosion nearby (TNT or blast)", null, true);
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        onPlayerCrafted(player, event.getCrafting());
    }

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        ItemStack stack = event.getOriginalStack();
        if (stack.isEmpty()) {
            return;
        }
        Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null) {
            return;
        }
        String id = key.toString();
        if (!CompanionNotableItemSupport.isNotablePickup(id)) {
            return;
        }
        boolean first = CompanionRecentActionMemory.markFirstOfKind(player.getUUID(), id);
        String pretty = CompanionNotableItemSupport.prettyName(id);
        CompanionRecentActionMemory.record(
                player.getUUID(), player.level().getGameTime(),
                CompanionRecentActionKind.ITEM_FIND,
                first ? "player found their first " + pretty : "player found " + pretty,
                id, true);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tickPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompanionRecentActionMemory.record(
                    player.getUUID(), player.level().getGameTime(),
                    CompanionRecentActionKind.DAMAGE,
                    "player took damage", null, true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompanionRecentActionMemory.clearPlayer(player.getUUID());
            CompanionInventoryWatchSupport.clearPlayer(player.getUUID());
        }
    }

    public static void onPlayerCrafted(ServerPlayer player, ItemStack crafted) {
        if (player == null || player.level().isClientSide() || crafted == null || crafted.isEmpty()) {
            return;
        }
        Identifier key = BuiltInRegistries.ITEM.getKey(crafted.getItem());
        if (key == null) {
            return;
        }
        String id = key.toString();
        String pretty = CompanionNotableItemSupport.prettyName(id);
        CompanionRecentActionMemory.record(
                player.getUUID(), player.level().getGameTime(),
                CompanionRecentActionKind.ITEM_CRAFT,
                "player just crafted " + pretty, id, true);
    }

    public static void tickPlayer(ServerPlayer player) {
        if (player == null || player.level().isClientSide() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        long time = level.getGameTime();
        int block = level.getBrightness(LightLayer.BLOCK, player.blockPosition());
        int sky = level.getBrightness(LightLayer.SKY, player.blockPosition());
        boolean dark = Math.max(block, sky) <= CompanionRecentActionMemory.DARK_LIGHT_THRESHOLD;
        CompanionRecentActionMemory.recordDarknessEnter(player.getUUID(), time, dark);

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
            Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
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
}
