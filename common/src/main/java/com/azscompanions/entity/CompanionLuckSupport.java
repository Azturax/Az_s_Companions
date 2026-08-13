package com.azscompanions.entity;

/**
 * Gates whether vanilla luck (attribute + luck/unluck effects) applies to companions.
 * Artifact mods often spam luck modifiers onto nearby entities — default off.
 * <p>
 * Config: {@code world.luckAffectsCompanion} in {@code azscompanions-common.toml} / {@code .json}.
 */
public final class CompanionLuckSupport {
    /** Default false — companions ignore luck effects/modifiers unless hosts opt in. */
    public static final boolean DEFAULT_LUCK_AFFECTS_COMPANION = false;

    private static volatile boolean luckAffectsCompanion = DEFAULT_LUCK_AFFECTS_COMPANION;

    private CompanionLuckSupport() {
    }

    public static boolean luckAffectsCompanion() {
        return luckAffectsCompanion;
    }

    public static void setLuckAffectsCompanion(boolean enabled) {
        luckAffectsCompanion = enabled;
    }

    /** Effect registry path ends with {@code luck} or {@code unluck} (vanilla + common aliases). */
    public static boolean isLuckEffectId(String effectId) {
        if (effectId == null || effectId.isBlank()) {
            return false;
        }
        String id = effectId.trim().toLowerCase(java.util.Locale.ROOT);
        int slash = id.indexOf(':');
        String path = slash >= 0 ? id.substring(slash + 1) : id;
        return path.equals("luck") || path.equals("unluck") || path.equals("bad_luck") || path.equals("badluck");
    }
}
