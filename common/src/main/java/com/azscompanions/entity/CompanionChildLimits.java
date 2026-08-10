package com.azscompanions.entity;

import com.azscompanions.teamfight.TeamFightDefaults;

/**
 * Caps for CCI / cake child companions under a leader.
 * Default max is {@link #MAX_PER_LEADER} (3); CCI {@code maxChildren=} can raise per parent.
 * Interaction {@code amount=} is not capped here — only per-leader child slots are.
 */
public final class CompanionChildLimits {
    /** Default max children (living + stored) per leader. */
    public static final int MAX_PER_LEADER = 3;
    /**
     * Hard ceiling for a per-companion {@code maxChildren=} override (entity/performance safety).
     * Not an interaction-amount ceiling.
     */
    public static final int HARD_MAX_CHILDREN = 64;
    /** Default body scale when {@code size=} omitted. */
    public static final float DEFAULT_BODY_SCALE = 0.5f;
    /** Default display name when {@code name=} omitted. */
    public static final String DEFAULT_NAME = "Bit";

    private CompanionChildLimits() {
    }

    /** Positive spawn request count — no artificial upper clamp (slots enforce the real limit). */
    public static int clampSpawnCount(int count) {
        return Math.max(1, count);
    }

    /** Clamp a per-leader max children value (1 … {@link #HARD_MAX_CHILDREN}). */
    public static int clampMaxChildren(int maxPerLeader) {
        return Math.max(1, Math.min(HARD_MAX_CHILDREN, maxPerLeader));
    }

    public static int remainingSlots(int currentChildren, int maxPerLeader) {
        int cap = clampMaxChildren(maxPerLeader);
        return Math.max(0, cap - Math.max(0, currentChildren));
    }

    public static int remainingSlots(int currentChildren) {
        return remainingSlots(currentChildren, MAX_PER_LEADER);
    }

    /**
     * Interaction amount → requested child count using the configured price per companion.
     * {@code count = max(1, amount / price)} when amount &gt; 0; otherwise 1.
     * Not capped; caller still clamps to remaining {@code maxChildren} slots.
     */
    public static int spawnCountFromAmount(int amount, int pricePerCompanion) {
        if (amount <= 0) {
            return 1;
        }
        int price = Math.max(1, pricePerCompanion);
        return Math.max(1, amount / price);
    }

    /** Same as {@link #spawnCountFromAmount(int, int)} with {@link TeamFightDefaults#SUPPORT_AMOUNT_PER_COMPANION}. */
    public static int spawnCountFromAmount(int amount) {
        return spawnCountFromAmount(amount, TeamFightDefaults.SUPPORT_AMOUNT_PER_COMPANION);
    }

    /**
     * @deprecated Use {@link #spawnCountFromAmount(int)}; kept for transitional call sites.
     */
    @Deprecated
    public static int spawnCountFromBits(int bits) {
        return spawnCountFromAmount(bits);
    }
}
