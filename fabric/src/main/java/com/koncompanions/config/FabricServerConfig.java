package com.koncompanions.config;

/**
 * Fabric server limits. Mirrors NeoForge {@code ServerConfig} defaults without a full config file yet.
 * Designed for one girlfriend/companion per player.
 */
public final class FabricServerConfig {
    /** Max owned companions per player. Default 1 (one companion / girlfriend). */
    public static final int MAX_COMPANIONS_PER_PLAYER = 1;

    private FabricServerConfig() {
    }
}
