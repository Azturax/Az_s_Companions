package com.azscompanions.entity;

/**
 * NBT keys for Bit snapshots stored on a parent companion (callable via charm / click).
 * World Bits moved here increase {@code storedChildCount}; calling pops FIFO order.
 */
public final class CompanionStoredChildren {
    public static final String NBT_LIST = "StoredChildren";
    public static final String ENTRY_UUID = "Uuid";
    public static final String ENTRY_DATA = "Data";

    private CompanionStoredChildren() {
    }
}
