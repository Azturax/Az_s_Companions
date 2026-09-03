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

    /**
     * Hard cap: one toggle Wiggly per eligible owner (server-wide).
     */
    public static final int MAX_OWNED_DOGS = 1;

    /**
     * Fixed entity scale for the Wiggly dog (toggle perk and companion sidekick).
     * Applied via {@code Attributes.SCALE} where that attribute exists (1.20.5+).
     */
    public static final float DOG_SCALE = 0.7f;

    /**
     * Player {@code tickCount} window after join: do not spawn a replacement dog while
     * the original may still be loading. Keep in lockstep with
     * {@code CompanionSpawnGuardSupport#LOGIN_GRACE_TICKS}.
     */
    public static final int LOGIN_GRACE_TICKS = 80;

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
     * Datapack / summon type path for the Wiggly companion (not Kon, Bits, Dox, or player skins).
     */
    public static boolean isWigglyDefinition(String definitionId) {
        if (definitionId == null || definitionId.isBlank()) {
            return false;
        }
        String token = definitionId.trim();
        int colon = token.indexOf(':');
        String path = colon >= 0 ? token.substring(colon + 1) : token;
        String key = path.toLowerCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
        return "wiggly".equals(key) || "wiggles".equals(key) || "mister_wiggly".equals(key);
    }

    /**
     * Charm Wiggly is wolf-form or a {@code wiggly} definition. Bits (children) never qualify.
     */
    public static boolean isWigglyCompanionType(String definitionId, String formName, boolean childCompanion) {
        if (childCompanion) {
            return false;
        }
        if (isWigglyDefinition(definitionId)) {
            return true;
        }
        return formName != null && "wolf".equalsIgnoreCase(formName.trim());
    }

    /**
     * Companion-following sidekick dog: charm-owned Wiggly only.
     * CCI / {@code /az summon} extras ({@code cciSummoned}) never get a dog, even if type is
     * {@code wiggly} or the owner UUID is Mister Wiggly.
     */
    public static boolean shouldSpawnCompanionSidekick(
            UUID ownerUuid,
            boolean cciSummoned,
            boolean childCompanion,
            String definitionId,
            String formName) {
        if (cciSummoned) {
            return false;
        }
        if (ownerUuid == null || !AzsCompanionsConstants.MISTER_WIGGLY_PLAYER_UUID.equals(ownerUuid)) {
            return false;
        }
        return isWigglyCompanionType(definitionId, formName, childCompanion);
    }

    public static boolean scaleNeedsUpdate(double current) {
        return Math.abs(current - DOG_SCALE) > 0.001d;
    }

    /**
     * True when this wolf should count toward the owner's single Wiggly slot:
     * tagged toggle dog, or nametag {@code Wiggly} owned by the player.
     */
    public static boolean looksLikeOwnedWiggly(
            boolean taggedToggle,
            boolean taggedSidekick,
            String customName,
            UUID ownerUuid,
            UUID wolfOwnerUuid) {
        if (ownerUuid == null || !ownerUuid.equals(wolfOwnerUuid)) {
            return false;
        }
        if (taggedToggle || taggedSidekick) {
            return true;
        }
        return isToggleDogName(customName);
    }

    /**
     * Lower is better. Prefer the sidekick while a companion is summoned, else the toggle dog,
     * then closest.
     */
    public static double keepScore(
            boolean preferSidekick,
            boolean isSidekick,
            boolean isToggle,
            double distSq) {
        double type = 0.0d;
        if (preferSidekick && isSidekick) {
            type = -1.0e15d;
        } else if (!preferSidekick && isToggle) {
            type = -1.0e15d;
        }
        return type + distSq;
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
