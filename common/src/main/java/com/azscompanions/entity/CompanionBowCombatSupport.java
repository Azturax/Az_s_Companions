package com.azscompanions.entity;

/**
 * Pure helpers for companion / Bit bow combat. Loaders wire {@code RangedBowAttackGoal}
 * and ammo consumption; this decides when ranged should win over melee.
 * <p>
 * Animal / non-humanoid mob forms never use bows — melee only.
 */
public final class CompanionBowCombatSupport {
    public static final float BOW_ATTACK_RADIUS = 15.0f;
    public static final int BOW_ATTACK_INTERVAL_TICKS = 20;
    public static final double BOW_MOVE_SPEED = 1.0d;

    private CompanionBowCombatSupport() {
    }

    /**
     * Bows only for humanoid / player-like forms (player mesh + zombie/skeleton/husk/stray/enderman).
     * Wolf/cat/fox/chicken/spider/etc. stay melee-only — including animal-shaped Bits.
     */
    public static boolean formCanUseBow(CompanionForm form) {
        return form != null && form.supportsHumanoidArmor();
    }

    /** Serialized form name overload for loaders that pass entity-data strings. */
    public static boolean formCanUseBow(String formSerializedName) {
        if (formSerializedName == null || formSerializedName.isBlank()) {
            return false;
        }
        return formCanUseBow(CompanionForm.byName(formSerializedName));
    }

    /** Item id path/namespace checks for vanilla + common mod bows. */
    public static boolean isBowItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        String id = itemId.toLowerCase();
        if (id.equals("minecraft:bow") || id.equals("minecraft:crossbow")) {
            return true;
        }
        int colon = id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        if (path.equals("bowl") || path.contains("elbow")) {
            return false;
        }
        return path.equals("bow") || path.endsWith("_bow") || path.contains("bow_");
    }

    public static boolean isArrowItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        String id = itemId.toLowerCase();
        if (id.equals("minecraft:arrow")
                || id.equals("minecraft:spectral_arrow")
                || id.equals("minecraft:tipped_arrow")) {
            return true;
        }
        int colon = id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        return path.equals("arrow") || path.endsWith("_arrow") || path.contains("arrow");
    }

    /**
     * Prefer ranged when the form may use bows, holding a bow, and Infinity or arrow ammo is available.
     */
    public static boolean shouldPreferRanged(
            boolean humanoidForm,
            boolean holdingBow,
            boolean hasInfinity,
            boolean hasArrowAmmo) {
        return humanoidForm && holdingBow && (hasInfinity || hasArrowAmmo);
    }

    /** @deprecated use {@link #shouldPreferRanged(boolean, boolean, boolean, boolean)} */
    @Deprecated
    public static boolean shouldPreferRanged(boolean holdingBow, boolean hasInfinity, boolean hasArrowAmmo) {
        return shouldPreferRanged(true, holdingBow, hasInfinity, hasArrowAmmo);
    }
}