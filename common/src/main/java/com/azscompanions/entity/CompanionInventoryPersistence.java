package com.azscompanions.entity;

/**
 * Policy for companion / Bit item persistence across death, despawn, park, and travel.
 * Loaders must not drop or clear inventory/equipment unless explicitly configured.
 */
public final class CompanionInventoryPersistence {
    /** Default: never drop gear or backpack on death (charm / parent / NBT keep it). */
    public static final boolean DEFAULT_KEEP_INVENTORY_ON_DEATH = true;

    private CompanionInventoryPersistence() {
    }

    /**
     * When true, death must not call vanilla equipment/loot drops that clear hand/armor
     * slots backed by companion inventory. Snapshots should still be written to charm
     * (parents) or parent {@code StoredChildren} (Bits).
     */
    public static boolean shouldKeepInventoryOnDeath(boolean configKeepInventoryOnDeath) {
        return configKeepInventoryOnDeath;
    }

    /** True when this death victim is a child Bit that should park onto a living parent. */
    public static boolean shouldStoreBitOnParent(
            boolean isChildCompanion,
            boolean parentAlive,
            boolean sameLeader) {
        return isChildCompanion && parentAlive && sameLeader;
    }
}
