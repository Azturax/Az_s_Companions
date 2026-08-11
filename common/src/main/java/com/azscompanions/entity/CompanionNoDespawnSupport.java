package com.azscompanions.entity;

/**
 * Owned companions (parents and child Bits) must not naturally despawn when far from players.
 * <p>
 * Loaders apply vanilla {@code Mob#setPersistenceRequired()} and the scoreboard/command tag
 * {@link #ENTITY_TAG} when an owner UUID is set (recruit, Bit spawn, charm summon, logout
 * restore, NBT load). Intentional discard (logout park, charm store, kill) is unaffected.
 */
public final class CompanionNoDespawnSupport {
    /**
     * Vanilla scoreboard / {@code /tag} name so datapacks and admins can target owned
     * companions and so custom despawn logic can skip them.
     */
    public static final String ENTITY_TAG = "azscompanions.nodespawn";

    private CompanionNoDespawnSupport() {
    }
}
