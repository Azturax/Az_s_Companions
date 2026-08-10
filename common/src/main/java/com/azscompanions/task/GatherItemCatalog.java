package com.azscompanions.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Dynamic catalog of item ids available as gather targets (refreshed on server start from the item registry).
 */
public final class GatherItemCatalog {
    private static volatile Set<String> IDS = Set.of();

    private GatherItemCatalog() {
    }

    public static void refresh(Collection<String> itemIds) {
        Set<String> next = new LinkedHashSet<>();
        if (itemIds != null) {
            for (String id : itemIds) {
                if (id == null || id.isBlank()) {
                    continue;
                }
                next.add(id.trim().toLowerCase(Locale.ROOT));
            }
        }
        IDS = Collections.unmodifiableSet(next);
    }

    public static boolean isKnown(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        String key = itemId.trim().toLowerCase(Locale.ROOT);
        if (IDS.isEmpty()) {
            return true; // registry not scanned yet — allow; resolve at assign time
        }
        return IDS.contains(key);
    }

    public static List<String> all() {
        return List.copyOf(IDS);
    }

    public static int size() {
        return IDS.size();
    }

    /** Build a mutable list for loaders to fill from {@code BuiltInRegistries.ITEM}. */
    public static List<String> newBuffer() {
        return new ArrayList<>();
    }
}
