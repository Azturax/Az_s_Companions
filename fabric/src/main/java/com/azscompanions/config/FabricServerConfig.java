package com.azscompanions.config;

import com.azscompanions.entity.CompanionFollowDistances;

/**
 * Fabric server limits. Mirrors NeoForge {@code ServerConfig} defaults without a full config file yet.
 * Designed for one girlfriend/companion per player.
 */
public final class FabricServerConfig {
    /** Max owned companions per player. Default 1 (one companion / girlfriend). */
    public static final int MAX_COMPANIONS_PER_PLAYER = 1;
    /** When true, companions defend the owner against living attackers. */
    public static final boolean ALLOW_COMBAT = true;
    /**
     * Home-bed proximity for Follow/Wander auto behavior (blocks).
     * Matches {@link CompanionFollowDistances#HOME_BED_RADIUS}.
     */
    public static final double HOME_BED_RADIUS = CompanionFollowDistances.HOME_BED_RADIUS;
    /** Owner chat/status lines (Hello/Bye). */
    public static final boolean COMPANION_CHAT_MESSAGES = true;
    /** Auto-equip tools/weapons from backpack. Default off. */
    public static final boolean AUTO_EQUIP_TOOLS = false;

    private FabricServerConfig() {
    }
}
