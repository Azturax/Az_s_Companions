package com.azscompanions.platform;

/**
 * Identifies which mod loader is running the current jar.
 */
public enum LoaderPlatform {
    NEOFORGE,
    FABRIC;

    public boolean isNeoForge() {
        return this == NEOFORGE;
    }

    public boolean isFabric() {
        return this == FABRIC;
    }
}
