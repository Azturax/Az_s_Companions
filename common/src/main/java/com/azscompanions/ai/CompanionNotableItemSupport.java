package com.azscompanions.ai;

import com.azscompanions.task.CraftRecipeCatalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Heuristics for notable pickups / crafts and a small set of watched recipes for
 * “last ingredient toward XYZ” chatter. Minecraft-free (item ids as strings).
 */
public final class CompanionNotableItemSupport {
    /** Boring blocks/items — never chatter about these pickups. */
    private static final String[] BORING_SUFFIXES = {
            "dirt", "cobblestone", "cobbled_deepslate", "stone", "netherrack", "gravel",
            "sand", "red_sand", "andesite", "diorite", "granite", "tuff", "deepslate",
            "oak_log", "spruce_log", "birch_log", "jungle_log", "acacia_log", "dark_oak_log",
            "mangrove_log", "cherry_log", "pale_oak_log", "oak_planks", "stick",
            "wheat_seeds", "torch", "soul_torch", "arrow", "bone", "rotten_flesh",
            "string", "feather", "egg", "snowball", "clay_ball"
    };

    private static final String[] WEAPON_TOOL_MARKERS = {
            "_sword", "_axe", "_pickaxe", "_shovel", "_hoe", "_bow", "_crossbow",
            "_trident", "_mace", "_spear"
    };

    private static final String[] ARMOR_MARKERS = {
            "_helmet", "_chestplate", "_leggings", "_boots"
    };

    private static final String[] RARE_MARKERS = {
            "diamond", "netherite", "emerald", "ancient_debris", "nether_star",
            "elytra", "totem", "enchanted_golden_apple", "heart_of_the_sea",
            "echo_shard", "heavy_core", "trial_key", "ominous", "dragon_egg",
            "golden_apple", "ender_pearl", "blaze_rod", "ender_eye", "ghast_tear",
            "amethyst_shard", "copper_ingot", "iron_ingot", "gold_ingot", "raw_iron",
            "raw_gold", "raw_copper", "lapis", "redstone", "quartz", "prismarine"
    };

    /**
     * Watched crafts for “you’ve got the last piece” lines.
     * Keys are result ids; values are ingredient id → count.
     */
    private static final Map<String, Map<String, Integer>> WATCHED_CRAFTS = buildWatchedCrafts();

    private CompanionNotableItemSupport() {
    }

    private static Map<String, Map<String, Integer>> buildWatchedCrafts() {
        Map<String, Map<String, Integer>> m = new LinkedHashMap<>();
        m.put("minecraft:wooden_sword", Map.of("minecraft:oak_planks", 2, "minecraft:stick", 1));
        m.put("minecraft:stone_sword", Map.of("minecraft:cobblestone", 2, "minecraft:stick", 1));
        m.put("minecraft:iron_sword", Map.of("minecraft:iron_ingot", 2, "minecraft:stick", 1));
        m.put("minecraft:golden_sword", Map.of("minecraft:gold_ingot", 2, "minecraft:stick", 1));
        m.put("minecraft:diamond_sword", Map.of("minecraft:diamond", 2, "minecraft:stick", 1));
        m.put("minecraft:netherite_sword", Map.of(
                "minecraft:netherite_ingot", 1, "minecraft:diamond_sword", 1));
        m.put("minecraft:iron_pickaxe", Map.of("minecraft:iron_ingot", 3, "minecraft:stick", 2));
        m.put("minecraft:diamond_pickaxe", Map.of("minecraft:diamond", 3, "minecraft:stick", 2));
        m.put("minecraft:bow", Map.of("minecraft:stick", 3, "minecraft:string", 3));
        m.put("minecraft:shield", Map.of("minecraft:oak_planks", 6, "minecraft:iron_ingot", 1));
        m.put("minecraft:iron_chestplate", Map.of("minecraft:iron_ingot", 8));
        m.put("minecraft:diamond_chestplate", Map.of("minecraft:diamond", 8));
        return Map.copyOf(m);
    }

    public static String normalizeId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return "";
        }
        String id = itemId.trim().toLowerCase(Locale.ROOT);
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        return id;
    }

    public static String prettyName(String itemId) {
        String id = normalizeId(itemId);
        if (id.isEmpty()) {
            return "item";
        }
        int colon = id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1));
            }
        }
        return sb.toString();
    }

    public static boolean isBoring(String itemId) {
        String id = normalizeId(itemId);
        if (id.isEmpty()) {
            return true;
        }
        String path = id.substring(id.indexOf(':') + 1);
        for (String s : BORING_SUFFIXES) {
            if (path.equals(s)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWeaponOrTool(String itemId) {
        String path = pathOf(itemId);
        for (String m : WEAPON_TOOL_MARKERS) {
            if (path.endsWith(m) || path.contains(m)) {
                return true;
            }
        }
        return path.equals("shield") || path.equals("trident") || path.equals("mace");
    }

    public static boolean isSword(String itemId) {
        return pathOf(itemId).endsWith("_sword") || pathOf(itemId).equals("sword");
    }

    public static boolean isArmor(String itemId) {
        String path = pathOf(itemId);
        for (String m : ARMOR_MARKERS) {
            if (path.endsWith(m)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isRareOrValuable(String itemId) {
        String path = pathOf(itemId);
        for (String m : RARE_MARKERS) {
            if (path.contains(m)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Notable for pickup chatter: tools/weapons/armor/rares, or craftable gear,
     * excluding boring filler. First-of-kind is decided by the memory layer.
     */
    public static boolean isNotablePickup(String itemId) {
        String id = normalizeId(itemId);
        if (id.isEmpty() || isBoring(id)) {
            return false;
        }
        return isWeaponOrTool(id) || isArmor(id) || isRareOrValuable(id);
    }

    /** Prefer craft reaction when the gained item is gear with a known recipe. */
    public static boolean looksLikeCraftResult(String itemId) {
        String id = normalizeId(itemId);
        if (id.isEmpty()) {
            return false;
        }
        if (!(isWeaponOrTool(id) || isArmor(id))) {
            return false;
        }
        return !CraftRecipeCatalog.recipesForResult(id).isEmpty()
                || WATCHED_CRAFTS.containsKey(id);
    }

    public static Map<String, Map<String, Integer>> watchedCrafts() {
        return WATCHED_CRAFTS;
    }

    /**
     * If gaining {@code gainedItemId} just completed a watched recipe vs prior inventory counts,
     * return that result id.
     */
    public static Optional<String> craftCompletedByGain(
            String gainedItemId,
            Map<String, Integer> countsBefore,
            Map<String, Integer> countsAfter) {
        if (gainedItemId == null || countsBefore == null || countsAfter == null) {
            return Optional.empty();
        }
        String gained = normalizeId(gainedItemId);
        List<String> hits = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> e : WATCHED_CRAFTS.entrySet()) {
            String result = e.getKey();
            Map<String, Integer> need = e.getValue();
            if (!need.containsKey(gained)) {
                continue; // only when the gained stack is an ingredient
            }
            boolean hadAllBefore = hasAll(countsBefore, need);
            boolean hasAllAfter = hasAll(countsAfter, need);
            if (!hadAllBefore && hasAllAfter) {
                hits.add(result);
            }
        }
        if (hits.isEmpty()) {
            return Optional.empty();
        }
        hits.sort((a, b) -> Integer.compare(craftWatchScore(b), craftWatchScore(a)));
        return Optional.of(hits.getFirst());
    }

    private static int craftWatchScore(String resultId) {
        if (isSword(resultId)) {
            return 3;
        }
        if (isWeaponOrTool(resultId)) {
            return 2;
        }
        if (isArmor(resultId)) {
            return 1;
        }
        return 0;
    }

    private static boolean hasAll(Map<String, Integer> counts, Map<String, Integer> need) {
        for (Map.Entry<String, Integer> e : need.entrySet()) {
            int have = counts.getOrDefault(normalizeId(e.getKey()), 0);
            if (have < e.getValue()) {
                return false;
            }
        }
        return true;
    }

    public static String craftCompliment(String itemId) {
        String name = prettyName(itemId);
        if (isSword(itemId)) {
            return "NICE SWORD!";
        }
        if (pathOf(itemId).endsWith("_pickaxe")) {
            return "Nice pickaxe!";
        }
        if (pathOf(itemId).endsWith("_axe")) {
            return "Nice axe!";
        }
        if (pathOf(itemId).endsWith("_bow") || pathOf(itemId).equals("crossbow")) {
            return "Nice bow!";
        }
        if (isArmor(itemId)) {
            return "Looking sharp in that " + name + "!";
        }
        if (isWeaponOrTool(itemId)) {
            return "Nice " + name + "!";
        }
        return "Ooh, a new " + name + "!";
    }

    private static String pathOf(String itemId) {
        String id = normalizeId(itemId);
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }
}
