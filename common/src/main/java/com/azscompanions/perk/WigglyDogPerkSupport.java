package com.azscompanions.perk;

import com.azscompanions.AzsCompanionsConstants;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

/**
 * Loader-agnostic helpers for the UUID-gated toggleable Wiggly dog perk
 * ({@link AzsCompanionsConstants#SPECIAL_PERK_PLAYER_UUID}).
 * <p>
 * Separate from the one-shot Wolfy companion grant and from Mister Wiggly's
 * companion-following sidekick.
 * <p>
 * Visibility is <strong>opt-in</strong> ({@link #DEFAULT_VISIBLE} = {@code false}):
 * the dog only appears after the player toggles it on ({@code /az wiggly} or keybind).
 * At most {@link #MAX_OWNED_DOGS} toggle dog may exist per eligible owner.
 */
public final class WigglyDogPerkSupport {
    /** New installs / untoggled players: dog stays dismissed until explicitly shown. */
    public static final boolean DEFAULT_VISIBLE = false;

    /** Hard cap: one toggle Wiggly per eligible owner (server-wide). */
    public static final int MAX_OWNED_DOGS = 1;

    /**
     * When present on the player (scoreboard tag / persistent flag), the toggle dog is shown.
     * Absence = hidden ({@link #DEFAULT_VISIBLE}).
     */
    public static final String PLAYER_SHOWN_TAG = "azscompanions.wiggly_dog_shown";

    /**
     * Legacy dismiss tag (pre-1.0.2). Presence still forces hidden; cleaned up on toggle.
     * Historically absence of this tag meant shown — that default is inverted.
     */
    public static final String PLAYER_HIDDEN_TAG = "azscompanions.wiggly_dog_hidden";

    /** Entity persistent-data / scoreboard marker for the toggle dog. */
    public static final String ENTITY_TAG = "azscompanions_toggle_wiggly";

    /** Owner UUID stored on the dog entity. */
    public static final String OWNER_TAG = "azscompanions_toggle_wiggly_owner";

    private WigglyDogPerkSupport() {
    }

    public static boolean isEligible(UUID uuid) {
        return uuid != null && AzsCompanionsConstants.SPECIAL_PERK_PLAYER_UUID.equals(uuid);
    }

    public static boolean isToggleDogName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return AzsCompanionsConstants.TOGGLE_WIGGLY_DOG_NAME.equalsIgnoreCase(name.trim());
    }

    /**
     * Scoreboard-tag visibility (Fabric / NeoForge 1.21.1).
     * Legacy {@link #PLAYER_HIDDEN_TAG} wins over shown; otherwise requires {@link #PLAYER_SHOWN_TAG}.
     */
    public static boolean isShownFromTags(Collection<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return DEFAULT_VISIBLE;
        }
        if (tags.contains(PLAYER_HIDDEN_TAG)) {
            return false;
        }
        return tags.contains(PLAYER_SHOWN_TAG);
    }

    /**
     * Persistent-data visibility (NeoForge 26.2).
     * Explicit hidden wins; otherwise requires an explicit shown=true flag.
     */
    public static boolean isShownFromPersistentFlags(
            boolean hasShownKey, boolean shown,
            boolean hasHiddenKey, boolean hidden) {
        if (hasHiddenKey && hidden) {
            return false;
        }
        if (hasShownKey) {
            return shown;
        }
        return DEFAULT_VISIBLE;
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
