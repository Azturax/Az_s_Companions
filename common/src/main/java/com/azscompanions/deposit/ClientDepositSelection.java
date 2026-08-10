package com.azscompanions.deposit;

import java.util.List;

/**
 * Client-side deposit selection cache. Highlights render only while {@link #isSelecting()}.
 */
public final class ClientDepositSelection {
    private static volatile DepositSelectionSnapshot snapshot = DepositSelectionSnapshot.OFF;

    private ClientDepositSelection() {
    }

    public static void apply(String encoded) {
        snapshot = DepositSelectionSnapshot.decode(encoded);
    }

    public static void apply(DepositSelectionSnapshot next) {
        snapshot = next == null ? DepositSelectionSnapshot.OFF : next;
    }

    public static DepositSelectionSnapshot get() {
        return snapshot;
    }

    public static boolean isSelecting() {
        return snapshot.selecting();
    }

    /** Highlights are visible only while selection mode is on. */
    public static boolean shouldHighlight() {
        return snapshot.selecting() && !snapshot.chests().isEmpty();
    }

    public static List<DepositChestRef> chests() {
        return snapshot.chests();
    }

    public static void clear() {
        snapshot = DepositSelectionSnapshot.OFF;
    }
}
