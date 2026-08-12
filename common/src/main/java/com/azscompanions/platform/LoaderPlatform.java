package com.azscompanions.platform;

/**
 * Identifies which mod loader is running the current jar.
 */
public enum LoaderPlatform {
    NEOFORGE,
    FORGE,
    FABRIC;

    public boolean isNeoForge() {
        return this == NEOFORGE;
    }

    /** Forge 1.20.1 line (no NeoForge 20.1). */
    public boolean isForge() {
        return this == FORGE;
    }

    public boolean isFabric() {
        return this == FABRIC;
    }
}
