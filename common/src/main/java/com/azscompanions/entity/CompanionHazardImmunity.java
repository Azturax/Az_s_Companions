package com.azscompanions.entity;

import java.util.Set;

/**
 * Environmental damage types companions ignore. Combat damage (mobs, players, arrows,
 * explosions from intentional fights, etc.) still applies — companions are not invincible.
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
            "campfire"
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
