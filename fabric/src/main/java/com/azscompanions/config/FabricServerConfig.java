package com.azscompanions.config;

/**
 * Fabric server limits. Mirrors NeoForge {@code ServerConfig} defaults without a full config file yet.
 * Designed for one girlfriend/companion per player.
 */
public final class FabricServerConfig {
    /** Max owned companions per player. Default 1 (one companion / girlfriend). */
    public static final int MAX_COMPANIONS_PER_PLAYER = 1;
    /** When true, companions defend the owner against living attackers. */
    public static final boolean ALLOW_COMBAT = true;

    private FabricServerConfig() {
    }
}
