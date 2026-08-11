package com.azscompanions.perk;

import com.azscompanions.AzsCompanionsConstants;

import java.util.UUID;

/**
 * Loader-agnostic helpers for the UUID-gated toggleable Wiggly dog perk
 * ({@link AzsCompanionsConstants#WOLFY_PLAYER_UUID}).
 * <p>
 * Separate from the one-shot Wolfy companion grant and from Mister Wiggly's
 * companion-following sidekick. Visibility persists via {@link #PLAYER_HIDDEN_TAG}.
 */
public final class WigglyDogPerkSupport {
    /** When present on the player, the toggle dog is dismissed. Default = shown. */
    public static final String PLAYER_HIDDEN_TAG = "azscompanions.wiggly_dog_hidden";

    /** Entity persistent-data marker for the toggle dog. */
    public static final String ENTITY_TAG = "azscompanions_toggle_wiggly";

    /** Owner UUID stored on the dog entity. */
    public static final String OWNER_TAG = "azscompanions_toggle_wiggly_owner";

    private WigglyDogPerkSupport() {
    }

    public static boolean isEligible(UUID uuid) {
        return uuid != null && AzsCompanionsConstants.WOLFY_PLAYER_UUID.equals(uuid);
    }

    public static boolean isToggleDogName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return AzsCompanionsConstants.TOGGLE_WIGGLY_DOG_NAME.equalsIgnoreCase(name.trim());
    }
}
