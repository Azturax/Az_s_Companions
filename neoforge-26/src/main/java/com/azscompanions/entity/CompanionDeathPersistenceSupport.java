package com.azscompanions.entity;

/**
 * Death snapshot helper. On NeoForge 26.x, entity NBT uses ValueOutput — charm park is handled
 * via {@code dropEquipment} keep + existing logout/charm paths. Full snapshot port TBD.
 */
public final class CompanionDeathPersistenceSupport {
    private CompanionDeathPersistenceSupport() {
    }

    public static void persistOnDeath(CompanionEntity companion) {
        // Inventory kept by CompanionEntity.dropEquipment override when keepInventoryOnDeath is true.
    }
}