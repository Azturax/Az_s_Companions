package com.azscompanions.loot;

/**
 * Shared structure / archaeology loot rates for mod items.
 * NeoForge GLM JSON must mirror these floats.
 * <p>
 * Master switch: config key {@code world.enableLoot} (default {@link #DEFAULT_ENABLE_LOOT}).
 * When false, all mod treasure loot injectors no-op (desert pyramid charm, trail ruins whistle, etc.).
 */
public final class CompanionLootSupport {
    /**
     * Chance to append one Companion Charm to a desert pyramid chest.
     * Was historically 100% (every chest); kept rare so pyramids are not free charms.
     */
    public static final float DESERT_PYRAMID_CHARM_CHANCE = 0.05f;

    /** When a mod treasure pool succeeds, roll this many item stacks (inclusive). */
    public static final int TREASURE_ROLLS_MIN = 1;
    public static final int TREASURE_ROLLS_MAX = 3;

    /**
     * Default for {@code enableLoot} — structure/archaeology loot injections on.
     * NeoForge: {@code config/azscompanions-common.toml} → {@code [world] enableLoot}.
     * Fabric: {@code config/azscompanions-common.json} → {@code world.enableLoot}.
     */
    public static final boolean DEFAULT_ENABLE_LOOT = true;

    private static volatile boolean lootInjectionEnabled = DEFAULT_ENABLE_LOOT;

    private CompanionLootSupport() {
    }

    /** Whether mod treasure loot injectors should append items. */
    public static boolean isLootInjectionEnabled() {
        return lootInjectionEnabled;
    }

    /** Applied by loader common/gameplay config when loaded or reloaded. */
    public static void setLootInjectionEnabled(boolean enabled) {
        lootInjectionEnabled = enabled;
    }
}
