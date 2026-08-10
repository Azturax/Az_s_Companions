package com.azscompanions.teamfight;

/**
 * Default streamer team-fight knobs (overridable via server config where wired).
 * Interaction amount → spawn count uses {@link #SUPPORT_AMOUNT_PER_COMPANION} (CCI-defined events, not platform-specific).
 */
public final class TeamFightDefaults {
    public static final String TEAM_LEFT = "red";
    public static final String TEAM_RIGHT = "blue";
    /** Max children under one fight leader (default; per-companion CCI {@code maxChildren=} can raise). */
    public static final int MAX_CHILDREN_PER_LEADER = 3;
    /**
     * Support/interaction amount required per child companion (integer division).
     * Example: {@code amount=500} with price 100 → 5 children.
     */
    public static final int SUPPORT_AMOUNT_PER_COMPANION = 100;

    /** Amount floors for gear quality tiers (inclusive). Same units as CCI {@code amount=}. */
    public static final int TIER_LEATHER_BITS = 100;
    public static final int TIER_CHAIN_BITS = 250;
    public static final int TIER_IRON_BITS = 500;
    public static final int TIER_DIAMOND_BITS = 750;
    public static final int TIER_BEST_BITS = 1000;

    private TeamFightDefaults() {
    }
}
