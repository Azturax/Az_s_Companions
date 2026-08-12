package com.azscompanions.perk;

import com.azscompanions.AzsCompanionsConstants;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

/**
 * Loader-agnostic helpers for the UUID-gated toggleable Wiggly dog perk.
 * <p>
 * Recipients:
 * <ul>
 *   <li>{@link AzsCompanionsConstants#MISTER_WIGGLY_PLAYER_UUID} — defaults <strong>ON</strong></li>
 *   <li>{@link AzsCompanionsConstants#SPECIAL_PERK_PLAYER_UUID} — defaults <strong>OFF</strong> (opt-in)</li>
 * </ul>
 * Separate from the Wolfy companion grant. On NeoForge, Mister Wiggly also has a
 * companion-following sidekick; loaders may suppress the toggle dog while that
 * sidekick is active so at most one Wiggly exists.
 * <p>
 * At most {@link #MAX_OWNED_DOGS} toggle dog may exist per eligible owner.
 */
public final class WigglyDogPerkSupport {
    /** Global fallback when no owner UUID is known: dog stays dismissed. */
    public static final boolean DEFAULT_VISIBLE = false;

    /** Hard cap: one toggle Wiggly per eligible owner (server-wide). */
    public static final int MAX_OWNED_DOGS = 1;

    /**
     * When present on the player (scoreboard tag / persistent flag), the toggle dog is shown.
     */
    public static final String PLAYER_SHOWN_TAG = "azscompanions.wiggly_dog_shown";

    /**
     * Legacy dismiss tag (pre-1.0.2). Presence still forces hidden; cleaned up on toggle.
     * Required for default-ON recipients so empty tags do not re-show the dog.
     */
    public static final String PLAYER_HIDDEN_TAG = "azscompanions.wiggly_dog_hidden";

    /** Entity persistent-data / scoreboard marker for the toggle dog. */
    public static final String ENTITY_TAG = "azscompanions_toggle_wiggly";

    /** Owner UUID stored on the dog entity. */
    public static final String OWNER_TAG = "azscompanions_toggle_wiggly_owner";

    private WigglyDogPerkSupport() {
    }

    /**
     * Toggle dog recipients: Mister Wiggly ({@code 5b0a2d0a-…}) and the flight perk UUID
     * ({@code 4274c47f-…}).
     */
    public static boolean isEligible(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        return AzsCompanionsConstants.MISTER_WIGGLY_PLAYER_UUID.equals(uuid)
                || AzsCompanionsConstants.SPECIAL_PERK_PLAYER_UUID.equals(uuid);
    }

    /**
     * Only Mister Wiggly defaults visible (ON). Flight-perk UUID and everyone else default OFF.
     */
    public static boolean defaultsVisible(UUID uuid) {
        return uuid != null && AzsCompanionsConstants.MISTER_WIGGLY_PLAYER_UUID.equals(uuid);
    }

    public static boolean isToggleDogName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return AzsCompanionsConstants.TOGGLE_WIGGLY_DOG_NAME.equalsIgnoreCase(name.trim());
    }

    /**
     * Scoreboard-tag visibility (Fabric / NeoForge 1.21.1).
     * Legacy {@link #PLAYER_HIDDEN_TAG} forces off; {@link #PLAYER_SHOWN_TAG} forces on;
     * otherwise {@link #defaultsVisible(UUID)}.
     */
    public static boolean isShownFromTags(Collection<String> tags, UUID ownerUuid) {
        if (tags != null && tags.contains(PLAYER_HIDDEN_TAG)) {
            return false;
        }
        if (tags != null && tags.contains(PLAYER_SHOWN_TAG)) {
            return true;
        }
        return defaultsVisible(ownerUuid);
    }

    /**
     * Persistent-data visibility (NeoForge 26.2).
     * Explicit hidden wins; explicit shown uses the flag; else {@link #defaultsVisible(UUID)}.
     */
    public static boolean isShownFromPersistentFlags(
            boolean hasShownKey, boolean shown,
            boolean hasHiddenKey, boolean hidden,
            UUID ownerUuid) {
        if (hasHiddenKey && hidden) {
            return false;
        }
        if (hasShownKey) {
            return shown;
        }
        return defaultsVisible(ownerUuid);
    }

    /**
     * Pick the single dog to keep when enforcing {@link #MAX_OWNED_DOGS}.
     * Caller should pass distance-squared (lower = better); empty → null.
     */
    public static <T> T pickOneToKeep(List<T> dogs, ToDoubleFunction<T> distanceSq) {
        if (dogs == null || dogs.isEmpty()) {
            return null;
        }
        T best = dogs.get(0);
        double bestD = distanceSq.applyAsDouble(best);
        for (int i = 1; i < dogs.size(); i++) {
            T candidate = dogs.get(i);
            double d = distanceSq.applyAsDouble(candidate);
            if (d < bestD) {
                best = candidate;
                bestD = d;
            }
        }
        return best;
    }
}
