package com.azscompanions.teamfight;

/**
 * Maps bit donation amount → starter gear item ids for fight spawns.
 */
public final class BitGearTiers {
    public record GearLoadout(
            int tier,
            int minBits,
            String label,
            String mainhand,
            String helmet,
            String chestplate,
            String leggings,
            String boots
    ) {
    }

    private BitGearTiers() {
    }

    public static GearLoadout forBits(int bits) {
        int b = Math.max(0, bits);
        if (b >= TeamFightDefaults.TIER_BEST_BITS) {
            return new GearLoadout(5, TeamFightDefaults.TIER_BEST_BITS, "netherite best",
                    "minecraft:netherite_sword",
                    "minecraft:netherite_helmet",
                    "minecraft:netherite_chestplate",
                    "minecraft:netherite_leggings",
                    "minecraft:netherite_boots");
        }
        if (b >= TeamFightDefaults.TIER_DIAMOND_BITS) {
            return new GearLoadout(4, TeamFightDefaults.TIER_DIAMOND_BITS, "diamond",
                    "minecraft:diamond_sword",
                    "minecraft:diamond_helmet",
                    "minecraft:iron_chestplate",
                    "minecraft:iron_leggings",
                    "minecraft:diamond_boots");
        }
        if (b >= TeamFightDefaults.TIER_IRON_BITS) {
            return new GearLoadout(3, TeamFightDefaults.TIER_IRON_BITS, "iron",
                    "minecraft:iron_sword",
                    "minecraft:iron_helmet",
                    "minecraft:iron_chestplate",
                    "minecraft:iron_leggings",
                    "minecraft:iron_boots");
        }
        if (b >= TeamFightDefaults.TIER_CHAIN_BITS) {
            return new GearLoadout(2, TeamFightDefaults.TIER_CHAIN_BITS, "chain",
                    "minecraft:stone_sword",
                    "minecraft:chainmail_helmet",
                    "minecraft:chainmail_chestplate",
                    "minecraft:chainmail_leggings",
                    "minecraft:chainmail_boots");
        }
        if (b >= TeamFightDefaults.TIER_LEATHER_BITS) {
            return new GearLoadout(1, TeamFightDefaults.TIER_LEATHER_BITS, "leather",
                    "minecraft:stick",
                    "minecraft:leather_helmet",
                    "minecraft:leather_chestplate",
                    "minecraft:leather_leggings",
                    "minecraft:leather_boots");
        }
        return new GearLoadout(0, 0, "none",
                "", "", "", "", "");
    }

    /** Human-readable tier table for scoreboard / docs. */
    public static String priceTableText() {
        return "100 leather+stick | 250 chain+stone | 500 iron | 750 diamond mix | 1000 netherite";
    }
}
