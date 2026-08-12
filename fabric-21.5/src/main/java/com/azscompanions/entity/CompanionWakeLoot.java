package com.azscompanions.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Chance for a companion to gift valuable loot when waking from a real bed sleep.
 */
public final class CompanionWakeLoot {
    /** ~20% per successful night wake. */
    public static final float CHANCE = 0.20f;
    private static final double OWNER_RANGE = 8.0d;

    private record Entry(Item item, int minCount, int maxCount, int weight) {
    }

    /**
     * Weighted table (total weight 100). Common metals dominate; diamonds / egaps are rare.
     */
    private static final Entry[] ENTRIES = {
            new Entry(Items.IRON_INGOT, 1, 3, 25),
            new Entry(Items.GOLD_INGOT, 1, 2, 18),
            new Entry(Items.LAPIS_LAZULI, 2, 6, 14),
            new Entry(Items.AMETHYST_SHARD, 1, 4, 12),
            new Entry(Items.REDSTONE, 2, 8, 10),
            new Entry(Items.EMERALD, 1, 2, 8),
            new Entry(Items.DIAMOND, 1, 1, 6),
            new Entry(Items.GOLDEN_APPLE, 1, 1, 5),
            new Entry(Items.ENCHANTED_GOLDEN_APPLE, 1, 1, 2),
    };

    private CompanionWakeLoot() {
    }

    /** Call once when the companion actually wakes from sleeping in a bed. */
    public static void tryGiveOnWake(FabricCompanionEntity companion) {
        if (!(companion.level() instanceof ServerLevel level) || level.isClientSide()) {
            return;
        }
        RandomSource random = companion.getRandom();
        if (random.nextFloat() >= CHANCE) {
            return;
        }
        ItemStack stack = roll(random);
        if (stack.isEmpty()) {
            return;
        }

        Player owner = companion.getOwner();
        boolean delivered = false;
        if (owner != null && owner.distanceTo(companion) <= OWNER_RANGE) {
            ItemStack toGive = stack.copy();
            if (owner.getInventory().add(toGive)) {
                delivered = true;
            } else if (!toGive.isEmpty() && toGive.getCount() < stack.getCount()) {
                dropAtFeet(companion, toGive);
                delivered = true;
            }
        }
        if (!delivered) {
            dropAtFeet(companion, stack);
        }

        if (owner instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.azscompanions.wake_loot", companion.getChatDisplayName()),
                    true);
        }
    }

    private static ItemStack roll(RandomSource random) {
        int total = 0;
        for (Entry entry : ENTRIES) {
            total += entry.weight();
        }
        if (total <= 0) {
            return ItemStack.EMPTY;
        }
        int pick = random.nextInt(total);
        for (Entry entry : ENTRIES) {
            pick -= entry.weight();
            if (pick < 0) {
                int count = entry.minCount();
                if (entry.maxCount() > entry.minCount()) {
                    count += random.nextInt(entry.maxCount() - entry.minCount() + 1);
                }
                return new ItemStack(entry.item(), count);
            }
        }
        return ItemStack.EMPTY;
    }

    private static void dropAtFeet(FabricCompanionEntity companion, ItemStack stack) {
        if (stack.isEmpty() || !(companion.level() instanceof ServerLevel level)) {
            return;
        }
        ItemEntity entity = new ItemEntity(level, companion.getX(), companion.getY() + 0.25d, companion.getZ(), stack);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }
}
