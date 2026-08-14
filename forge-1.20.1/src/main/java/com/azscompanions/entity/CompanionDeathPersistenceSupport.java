package com.azscompanions.entity;

/** Death snapshot helper for Forge 1.20.1 — inventory kept via dropEquipment override. */
public final class CompanionDeathPersistenceSupport {
    private CompanionDeathPersistenceSupport() {
    }

    public static void persistOnDeath(CompanionEntity companion) {
        // Inventory kept by CompanionEntity.dropEquipment when keepInventoryOnDeath is true.
    }
}