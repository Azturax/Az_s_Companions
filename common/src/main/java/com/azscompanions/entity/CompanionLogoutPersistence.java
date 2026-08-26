package com.azscompanions.entity;

/**
 * NBT keys for parking owned companions when the owner disconnects and restoring them on join.
 * <p>
 * Loader modules write entity snapshots into the Companion Charm (bound companion) and/or
 * player/offline storage ({@link #PLAYER_LIST_TAG}), then discard living entities. Login
 * restores near the player and clears parking markers so charm recall does not duplicate.
 */
public final class CompanionLogoutPersistence {
    /**
     * ListTag on NeoForge {@code Player#getPersistentData()} / Fabric offline store:
     * compounds with {@link #ENTRY_UUID} + {@link #ENTRY_DATA}.
     */
    public static final String PLAYER_LIST_TAG = "azscompanions.LogoutCompanions";

    /** Companion entity UUID for a parked entry. */
    public static final String ENTRY_UUID = "Uuid";

    /** Full {@code saveWithoutId} companion payload (includes {@link CompanionStoredChildren}). */
    public static final String ENTRY_DATA = "Data";

    /**
     * Charm custom-data flag: stored payload was written by logout parking (auto-restore on join).
     * Manual charm store/recall must leave this unset/false so dismissed companions stay stored.
     */
    public static final String CHARM_LOGOUT_PARKED = "LogoutParked";

    /**
     * Player persistent-data UUID of the owned toggle Wiggly (prevents login tick from
     * summoning a second dog while the original chunk is still loading).
     */
    public static final String WIGGLY_ENTITY_UUID = "azscompanions.WigglyEntityUuid";

    /** Parked toggle-Wiggly snapshot list (same shape as {@link #PLAYER_LIST_TAG}). */
    public static final String WIGGLY_LIST_TAG = "azscompanions.LogoutWiggly";

    /**
     * CCI temporary summons are discarded on logout, never written to charm or
     * {@link #PLAYER_LIST_TAG}. Charm companions always park.
     */
    public static boolean shouldParkOnLogout(boolean cciSummoned) {
        return CompanionCciSummonSupport.shouldParkOnLogout(cciSummoned);
    }

    private CompanionLogoutPersistence() {
    }
}
