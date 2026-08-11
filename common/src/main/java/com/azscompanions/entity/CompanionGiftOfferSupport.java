package com.azscompanions.entity;

import com.azscompanions.ai.CompanionRecentAction;
import com.azscompanions.ai.CompanionRecentActionKind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Context-weighted return gifts after the player gives a flower.
 * Pure helpers — loaders gather {@link Hint}s from world/player state and resolve item ids.
 */
public final class CompanionGiftOfferSupport {
    /** Food level at or below this (half-shanks) counts as hungry. */
    public static final int LOW_HUNGER_FOOD_LEVEL = 6;

    /**
     * Play-context signals that bias the offer pool.
     * Stronger / more urgent hints use higher entry weights in {@link #buildWeightedPool}.
     */
    public enum Hint {
        SLEEPING,
        BATHING,
        ADVENTURING,
        DARKNESS,
        EXPLOSION,
        COMBAT,
        DAMAGE,
        CRAFTING,
        ITEM_FIND,
        EATING,
        NIGHT,
        LOW_HUNGER,
        OWNER_IDLE,
        OWNER_EXPLORING,
        HOSTILE,
        BIOME_OCEAN,
        BIOME_DESERT,
        BIOME_COLD,
        BIOME_FOREST,
        BIOME_NETHER
    }

    /** Immutable set of active hints (empty → classic flower pool only). */
    public record Snapshot(Set<Hint> hints) {
        public Snapshot {
            hints = hints == null || hints.isEmpty()
                    ? Set.of()
                    : Set.copyOf(EnumSet.copyOf(hints));
        }

        public static Snapshot empty() {
            return new Snapshot(Set.of());
        }

        public static Snapshot of(Collection<Hint> hints) {
            if (hints == null || hints.isEmpty()) {
                return empty();
            }
            EnumSet<Hint> set = EnumSet.noneOf(Hint.class);
            for (Hint h : hints) {
                if (h != null) {
                    set.add(h);
                }
            }
            return new Snapshot(set);
        }

        public static Snapshot of(Hint... hints) {
            if (hints == null || hints.length == 0) {
                return empty();
            }
            EnumSet<Hint> set = EnumSet.noneOf(Hint.class);
            for (Hint h : hints) {
                if (h != null) {
                    set.add(h);
                }
            }
            return new Snapshot(set);
        }

        public boolean has(Hint hint) {
            return hints.contains(hint);
        }
    }

    private CompanionGiftOfferSupport() {
    }

    /** Uniform-ish pick using {@link ThreadLocalRandom}. */
    public static String pickOfferId(Snapshot snapshot) {
        Map<String, Integer> pool = buildWeightedPool(snapshot);
        int total = pool.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            return CompanionFlowerGiftSupport.pickRandomOfferId();
        }
        return pickFromPool(pool, total, ThreadLocalRandom.current().nextInt(total));
    }

    /**
     * Deterministic pick for tests: {@code rollNonNegative} is reduced modulo total weight.
     */
    public static String pickOfferId(Snapshot snapshot, int rollNonNegative) {
        Map<String, Integer> pool = buildWeightedPool(snapshot);
        int total = pool.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            return CompanionFlowerGiftSupport.pickRandomOfferId(rollNonNegative);
        }
        return pickFromPool(pool, total, Math.floorMod(rollNonNegative, total));
    }

    /**
     * Builds item-id → weight. Always includes the classic flower pool at low weight so
     * peaceful / idle moments still often return flowers.
     */
    public static Map<String, Integer> buildWeightedPool(Snapshot snapshot) {
        Snapshot snap = snapshot == null ? Snapshot.empty() : snapshot;
        Map<String, Integer> weights = new LinkedHashMap<>();

        for (String flower : CompanionFlowerGiftSupport.OFFER_FLOWER_IDS) {
            add(weights, flower, snap.has(Hint.OWNER_IDLE) || snap.hints().isEmpty() ? 4 : 2);
        }

        if (snap.has(Hint.SLEEPING)) {
            add(weights, "minecraft:white_wool", 28);
            add(weights, "minecraft:feather", 22);
            add(weights, "minecraft:cookie", 20);
            add(weights, "minecraft:sweet_berries", 16);
        }
        if (snap.has(Hint.BATHING) || snap.has(Hint.BIOME_OCEAN)) {
            add(weights, "minecraft:sponge", 26);
            add(weights, "minecraft:kelp", 22);
            add(weights, "minecraft:seagrass", 16);
            add(weights, "minecraft:tropical_fish", 14);
            add(weights, "minecraft:prismarine_shard", 12);
        }
        if (snap.has(Hint.ADVENTURING) || snap.has(Hint.OWNER_EXPLORING)) {
            add(weights, "minecraft:torch", 24);
            add(weights, "minecraft:bread", 22);
            add(weights, "minecraft:cooked_chicken", 16);
            add(weights, "minecraft:map", 12);
            add(weights, "minecraft:cookie", 10);
        }
        if (snap.has(Hint.DARKNESS) || snap.has(Hint.NIGHT)) {
            add(weights, "minecraft:torch", 40);
            add(weights, "minecraft:lantern", 28);
            add(weights, "minecraft:glow_berries", 22);
            add(weights, "minecraft:candle", 16);
        }
        if (snap.has(Hint.EXPLOSION)) {
            add(weights, "minecraft:milk_bucket", 30);
            add(weights, "minecraft:cobblestone", 20);
            add(weights, "minecraft:oak_planks", 14);
        }
        if (snap.has(Hint.COMBAT) || snap.has(Hint.DAMAGE)) {
            add(weights, "minecraft:bread", 26);
            add(weights, "minecraft:cooked_beef", 20);
            add(weights, "minecraft:arrow", 18);
            add(weights, "minecraft:shield", 12);
        }
        if (snap.has(Hint.CRAFTING)) {
            add(weights, "minecraft:stick", 24);
            add(weights, "minecraft:coal", 18);
            add(weights, "minecraft:iron_nugget", 14);
        }
        if (snap.has(Hint.ITEM_FIND)) {
            add(weights, "minecraft:cookie", 22);
            add(weights, "minecraft:gold_nugget", 16);
            add(weights, "minecraft:poppy", 14);
        }
        if (snap.has(Hint.LOW_HUNGER) || snap.has(Hint.EATING)) {
            add(weights, "minecraft:bread", 36);
            add(weights, "minecraft:apple", 28);
            add(weights, "minecraft:cooked_chicken", 24);
            add(weights, "minecraft:cookie", 18);
            add(weights, "minecraft:sweet_berries", 14);
        }
        if (snap.has(Hint.HOSTILE)) {
            add(weights, "minecraft:wither_rose", 28);
            add(weights, "minecraft:bone", 20);
            add(weights, "minecraft:rotten_flesh", 14);
        }
        if (snap.has(Hint.BIOME_DESERT)) {
            add(weights, "minecraft:cactus", 22);
            add(weights, "minecraft:dead_bush", 14);
            add(weights, "minecraft:rabbit_hide", 10);
        }
        if (snap.has(Hint.BIOME_COLD)) {
            add(weights, "minecraft:snowball", 22);
            add(weights, "minecraft:sweet_berries", 16);
            add(weights, "minecraft:rabbit_hide", 10);
        }
        if (snap.has(Hint.BIOME_FOREST)) {
            add(weights, "minecraft:apple", 22);
            add(weights, "minecraft:sweet_berries", 16);
            add(weights, "minecraft:stick", 12);
        }
        if (snap.has(Hint.BIOME_NETHER)) {
            add(weights, "minecraft:magma_cream", 22);
            add(weights, "minecraft:glowstone_dust", 16);
            add(weights, "minecraft:netherrack", 10);
        }

        return weights;
    }

    /** Map a recent-action kind onto gift hints (does not consume the buffer). */
    public static void addRecentActionHints(EnumSet<Hint> hints, CompanionRecentActionKind kind) {
        if (hints == null || kind == null) {
            return;
        }
        switch (kind) {
            case DARKNESS -> hints.add(Hint.DARKNESS);
            case EXPLOSION -> hints.add(Hint.EXPLOSION);
            case COMBAT -> hints.add(Hint.COMBAT);
            case DAMAGE -> hints.add(Hint.DAMAGE);
            case ITEM_CRAFT, CRAFT_READY -> hints.add(Hint.CRAFTING);
            case ITEM_FIND -> hints.add(Hint.ITEM_FIND);
            case EATING -> hints.add(Hint.EATING);
            case SLEEPING -> hints.add(Hint.SLEEPING);
            case BLOCK_PLACE, BLOCK_BREAK -> {
                // Ambient dig/place — leave gift pool on stronger signals.
            }
        }
    }

    public static void addRecentActionHints(EnumSet<Hint> hints, Iterable<CompanionRecentAction> actions) {
        if (hints == null || actions == null) {
            return;
        }
        for (CompanionRecentAction action : actions) {
            if (action != null) {
                addRecentActionHints(hints, action.kind());
            }
        }
    }

    /** Human-readable context → example gifts (docs / CHANGELOG). */
    public static List<String> exampleGiftsFor(Hint hint) {
        return switch (Objects.requireNonNull(hint, "hint")) {
            case SLEEPING -> List.of("white_wool", "feather", "cookie", "sweet_berries");
            case BATHING, BIOME_OCEAN -> List.of("sponge", "kelp", "seagrass", "tropical_fish");
            case ADVENTURING, OWNER_EXPLORING -> List.of("torch", "bread", "cooked_chicken", "map");
            case DARKNESS, NIGHT -> List.of("torch", "lantern", "glow_berries", "candle");
            case EXPLOSION -> List.of("milk_bucket", "cobblestone", "oak_planks");
            case COMBAT, DAMAGE -> List.of("bread", "cooked_beef", "arrow", "shield");
            case CRAFTING -> List.of("stick", "coal", "iron_nugget");
            case ITEM_FIND -> List.of("cookie", "gold_nugget", "poppy");
            case LOW_HUNGER, EATING -> List.of("bread", "apple", "cooked_chicken", "cookie");
            case OWNER_IDLE -> List.of("classic flowers");
            case HOSTILE -> List.of("wither_rose", "bone", "rotten_flesh");
            case BIOME_DESERT -> List.of("cactus", "dead_bush", "rabbit_hide");
            case BIOME_COLD -> List.of("snowball", "sweet_berries", "rabbit_hide");
            case BIOME_FOREST -> List.of("apple", "sweet_berries", "stick");
            case BIOME_NETHER -> List.of("magma_cream", "glowstone_dust", "netherrack");
        };
    }

    private static void add(Map<String, Integer> weights, String id, int weight) {
        if (weight <= 0 || id == null || id.isBlank()) {
            return;
        }
        weights.merge(id, weight, Integer::sum);
    }

    private static String pickFromPool(Map<String, Integer> pool, int total, int roll) {
        int cursor = 0;
        String last = CompanionFlowerGiftSupport.OFFER_FLOWER_IDS.get(0);
        for (Map.Entry<String, Integer> e : pool.entrySet()) {
            last = e.getKey();
            cursor += e.getValue();
            if (roll < cursor) {
                return e.getKey();
            }
        }
        return last;
    }

    /** Flatten pool for tests (id repeated by weight is avoided — returns distinct ids). */
    public static List<String> poolItemIds(Snapshot snapshot) {
        return new ArrayList<>(buildWeightedPool(snapshot).keySet());
    }
}
