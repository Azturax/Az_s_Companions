package com.azscompanions.entity;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pure helpers for flower gifting: cooldown, toss velocity toward the player, and where
 * to show a return offer without overwriting a non-offer task item in hand (fallback when
 * a throw cannot spawn). Loader code accepts {@code #minecraft:flowers} gifts; the return
 * item is chosen by {@link CompanionGiftOfferSupport} (flowers when play context is quiet).
 */
public final class CompanionFlowerGiftSupport {
    /** ~3s between successful gifts (spam guard). */
    public static final int COOLDOWN_TICKS = 60;

    /** Short delay so the tossed gift can arc before pickup (matches potion toss-to-owner). */
    public static final int THROW_PICKUP_DELAY_TICKS = 10;

    /** Horizontal speed of the gift toss toward the player. */
    public static final double THROW_HORIZONTAL = 0.35d;

    /** Mild upward component for a short arc. */
    public static final double THROW_VERTICAL = 0.28d;

    /** Biome base temperature at or below this counts as cold for gift hints. */
    public static final float BIOME_COLD_MAX_TEMP = 0.15f;

    /** Biome base temperature at or above this counts as desert/hot for gift hints. */
    public static final float BIOME_DESERT_MIN_TEMP = 1.5f;

    /**
     * Classic flower / tall-flower / petal items for the random return gift.
     * Intentionally excludes leaves and other broad {@code #minecraft:flowers} members
     * that look odd as a hand-held bouquet.
     */
    public static final List<String> OFFER_FLOWER_IDS = List.of(
            "minecraft:dandelion",
            "minecraft:poppy",
            "minecraft:blue_orchid",
            "minecraft:allium",
            "minecraft:azure_bluet",
            "minecraft:red_tulip",
            "minecraft:orange_tulip",
            "minecraft:white_tulip",
            "minecraft:pink_tulip",
            "minecraft:oxeye_daisy",
            "minecraft:cornflower",
            "minecraft:lily_of_the_valley",
            "minecraft:wither_rose",
            "minecraft:torchflower",
            "minecraft:sunflower",
            "minecraft:lilac",
            "minecraft:rose_bush",
            "minecraft:peony",
            "minecraft:pitcher_plant",
            "minecraft:pink_petals"
    );

    public enum HandPlacement {
        /** Show offer in main hand (empty or replacing a previous offer). */
        MAIN_HAND,
        /** Main busy with a non-offer item; show in offhand if free. */
        OFF_HAND,
        /** Both hands busy with non-offer items — keep pending only. */
        PENDING_ONLY
    }

    private CompanionFlowerGiftSupport() {
    }

    public static boolean canGift(long gameTime, long cooldownUntilGameTime) {
        return gameTime >= cooldownUntilGameTime;
    }

    public static long nextCooldownUntil(long gameTime) {
        return gameTime + COOLDOWN_TICKS;
    }

    /**
     * Prefer main hand for visibility; never displace a non-offer item.
     *
     * @param mainEmpty              main hand empty
     * @param mainHoldsCurrentOffer  main already shows this companion's offered flower
     * @param offEmpty               offhand empty
     * @param offHoldsCurrentOffer   offhand already shows the offered flower
     */
    public static HandPlacement placement(
            boolean mainEmpty,
            boolean mainHoldsCurrentOffer,
            boolean offEmpty,
            boolean offHoldsCurrentOffer) {
        if (mainEmpty || mainHoldsCurrentOffer) {
            return HandPlacement.MAIN_HAND;
        }
        if (offEmpty || offHoldsCurrentOffer) {
            return HandPlacement.OFF_HAND;
        }
        return HandPlacement.PENDING_ONLY;
    }

    /** Uniform pick from {@link #OFFER_FLOWER_IDS}. */
    public static String pickRandomOfferId() {
        List<String> ids = OFFER_FLOWER_IDS;
        return ids.get(ThreadLocalRandom.current().nextInt(ids.size()));
    }

    public static String pickRandomOfferId(int randomIndexBounded) {
        List<String> ids = OFFER_FLOWER_IDS;
        int i = Math.floorMod(randomIndexBounded, ids.size());
        return ids.get(i);
    }

    /**
     * Mild arc velocity from companion toward player (XZ toward target, fixed upward lift).
     * Pure helper — loaders spawn an {@code ItemEntity} with this delta.
     *
     * @return {@code [vx, vy, vz]}
     */
    public static double[] throwVelocity(
            double fromX, double fromY, double fromZ,
            double toX, double toY, double toZ) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        if (horiz < 1.0e-4d) {
            return new double[]{0.0d, THROW_VERTICAL, 0.0d};
        }
        double scale = THROW_HORIZONTAL / horiz;
        double vy = THROW_VERTICAL;
        double dy = toY - fromY;
        if (dy > 0.5d) {
            vy += Math.min(0.15d, dy * 0.05d);
        }
        return new double[]{dx * scale, vy, dz * scale};
    }

    /**
     * Maps loader-resolved biome flags / temperature onto {@link CompanionGiftOfferSupport.Hint}s.
     * Pure helper so loaders stay free of duplicated threshold logic.
     */
    public static void addBiomeHints(
            EnumSet<CompanionGiftOfferSupport.Hint> hints,
            boolean oceanLike,
            boolean forestLike,
            boolean nether,
            float baseTemperature) {
        if (hints == null) {
            return;
        }
        if (oceanLike) {
            hints.add(CompanionGiftOfferSupport.Hint.BIOME_OCEAN);
        }
        if (forestLike) {
            hints.add(CompanionGiftOfferSupport.Hint.BIOME_FOREST);
        }
        if (nether) {
            hints.add(CompanionGiftOfferSupport.Hint.BIOME_NETHER);
        } else if (baseTemperature >= BIOME_DESERT_MIN_TEMP) {
            hints.add(CompanionGiftOfferSupport.Hint.BIOME_DESERT);
        } else if (baseTemperature <= BIOME_COLD_MAX_TEMP) {
            hints.add(CompanionGiftOfferSupport.Hint.BIOME_COLD);
        }
    }
}
