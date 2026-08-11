package com.azscompanions.entity;

/**
 * World-save store for companion appearance + persona (global per save, not per dimension).
 * <p>
 * Loaders keep a map companionUUID → identity NBT on overworld SavedData (Fabric) or the same
 * overworld store / player-adjacent persistence (NeoForge). Updated on customize/persona save,
 * dimension travel, and logout park; reapplied when restoring after portals or charm recovery.
 * Never treats dimension travel as logout.
 */
public final class CompanionIdentityPersistence {
    /** Overworld SavedData name (Fabric + NeoForge shared convention). */
    public static final String DATA_NAME = "azscompanions_companion_identity";

    /** Root list of companion identity entries. */
    public static final String TAG_ENTRIES = "Entries";

    /** Companion entity UUID. */
    public static final String ENTRY_UUID = "Uuid";

    /** Identity / full {@code saveWithoutId} snapshot payload. */
    public static final String ENTRY_DATA = "Data";

    private CompanionIdentityPersistence() {
    }
}
