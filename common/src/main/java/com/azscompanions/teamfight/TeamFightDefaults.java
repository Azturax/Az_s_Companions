package com.azscompanions.teamfight;

/**
 * Default streamer team-fight knobs (overridable via NeoForge config where wired).
 */
public final class TeamFightDefaults {
    public static final String TEAM_LEFT = "red";
    public static final String TEAM_RIGHT = "blue";
    /** Subs (or sub-equivalent) required to spawn a team leader. */
    public static final int SUB_COST_LEADER = 1;
    /** Max children under one fight leader. */
    public static final int MAX_CHILDREN_PER_LEADER = 6;
    /** Max fight-spawned companions per streamer (leaders + children). */
    public static final int MAX_FIGHT_SPAWNS_PER_PLAYER = 24;

    /** Bit floors for gear tiers (inclusive). */
    public static final int TIER_LEATHER_BITS = 100;
    public static final int TIER_CHAIN_BITS = 250;
    public static final int TIER_IRON_BITS = 500;
    public static final int TIER_DIAMOND_BITS = 750;
    public static final int TIER_BEST_BITS = 1000;

    private TeamFightDefaults() {
    }
}
