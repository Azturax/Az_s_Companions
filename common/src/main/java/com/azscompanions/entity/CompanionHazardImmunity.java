package com.azscompanions.entity;

import java.util.Set;

/**
 * Environmental damage types all companions ignore (teleport/world hazards).
 * Combat damage still applies unless {@link CompanionInvincibilitySupport} says otherwise.
 *
 * <p>Keys are {@code ResourceLocation#getPath()} values for vanilla {@code DamageTypes}.
 */
public final class CompanionHazardImmunity {
    private static final Set<String> IGNORED_DAMAGE_TYPE_PATHS = Set.of(
            "fall",
            "cactus",
            "sweet_berry_bush",
            "drown",
            "in_wall",
            "campfire",
            "lava",
            "in_fire",
            "on_fire",
            "hot_floor",
            "freeze",
            "cramming",
            "fly_into_wall",
            "falling_block",
            "falling_anvil",
            "falling_stalactite",
            "stalagmite",
            "out_of_world"
    );

    private CompanionHazardImmunity() {
    }

    /** @param damageTypePath path of the damage type id (e.g. {@code "fall"}) */
    public static boolean ignores(String damageTypePath) {
        return damageTypePath != null && IGNORED_DAMAGE_TYPE_PATHS.contains(damageTypePath);
    }

    public static Set<String> ignoredPaths() {
        return IGNORED_DAMAGE_TYPE_PATHS;
    }
}
