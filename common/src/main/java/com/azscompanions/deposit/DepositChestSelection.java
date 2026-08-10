package com.azscompanions.deposit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player deposit chest multi-select used by companion gather {@code deposit=chest}.
 * Selection mode controls client highlights; selected positions persist after mode exits.
 */
public final class DepositChestSelection {
    public static final int MAX_CHESTS = 32;

    private static final ConcurrentHashMap<UUID, DepositChestSelection> BY_OWNER = new ConcurrentHashMap<>();

    private final UUID ownerUuid;
    private boolean selecting;
    private final LinkedHashSet<DepositChestRef> chests = new LinkedHashSet<>();

    private DepositChestSelection(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public static DepositChestSelection of(UUID ownerUuid) {
        return BY_OWNER.computeIfAbsent(ownerUuid, DepositChestSelection::new);
    }

    public static void clearAll() {
        BY_OWNER.clear();
    }

    public UUID ownerUuid() {
        return ownerUuid;
    }

    public boolean isSelecting() {
        return selecting;
    }

    /** Enable selection mode (highlights on). */
    public void enableSelecting() {
        selecting = true;
    }

    /** Exit selection mode; keep stored chests. */
    public void finishKeepingSelection() {
        selecting = false;
    }

    /** Clear stored chests and exit selection mode. */
    public void clearSelection() {
        chests.clear();
        selecting = false;
    }

    public List<DepositChestRef> chests() {
        return List.copyOf(chests);
    }

    public int size() {
        return chests.size();
    }

    public boolean isEmpty() {
        return chests.isEmpty();
    }

    /**
     * Toggle a chest while selecting. Returns {@code true} if now selected, {@code false} if removed
     * or rejected (not selecting / at cap when adding).
     */
    public boolean toggle(String dimension, int x, int y, int z) {
        if (!selecting) {
            return false;
        }
        DepositChestRef ref = new DepositChestRef(dimension, x, y, z);
        if (chests.remove(ref)) {
            return false;
        }
        if (chests.size() >= MAX_CHESTS) {
            return false;
        }
        chests.add(ref);
        return true;
    }

    public boolean contains(String dimension, int x, int y, int z) {
        return chests.contains(new DepositChestRef(dimension, x, y, z));
    }

    public Optional<DepositChestRef> nearestInDimension(String dimension, int x, int y, int z) {
        if (dimension == null || chests.isEmpty()) {
            return Optional.empty();
        }
        return chests.stream()
                .filter(c -> c.dimension().equals(dimension))
                .min(Comparator.comparingInt(c -> c.manhattan(x, y, z)));
    }

    public List<DepositChestRef> inDimension(String dimension) {
        if (dimension == null) {
            return List.of();
        }
        List<DepositChestRef> out = new ArrayList<>();
        for (DepositChestRef c : chests) {
            if (c.dimension().equals(dimension)) {
                out.add(c);
            }
        }
        return out;
    }

    public DepositSelectionSnapshot snapshot() {
        return new DepositSelectionSnapshot(selecting, chests());
    }
}
