package com.azscompanions.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pure helpers for inventory-delta → recent-action recording (loaders supply counts).
 */
public final class CompanionInventoryWatchSupport {
    private static final Map<UUID, Map<String, Integer>> LAST_COUNTS = new LinkedHashMap<>();

    private CompanionInventoryWatchSupport() {
    }

    public static void clearAll() {
        synchronized (LAST_COUNTS) {
            LAST_COUNTS.clear();
        }
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId == null) {
            return;
        }
        synchronized (LAST_COUNTS) {
            LAST_COUNTS.remove(playerId);
        }
    }

    /**
     * Compare previous notable-item counts to {@code currentCounts}; record finds / crafts /
     * craft-ready events. Call about once per second from the server thread.
     *
     * @param currentCounts item id → total count in player inventory (notable + watched ingredients)
     */
    public static void observeCounts(UUID playerId, long gameTime, Map<String, Integer> currentCounts) {
        if (playerId == null || currentCounts == null) {
            return;
        }
        Map<String, Integer> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : currentCounts.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || e.getValue() <= 0) {
                continue;
            }
            normalized.put(CompanionNotableItemSupport.normalizeId(e.getKey()), e.getValue());
        }
        Map<String, Integer> before;
        synchronized (LAST_COUNTS) {
            before = LAST_COUNTS.put(playerId, Map.copyOf(normalized));
        }
        if (before == null) {
            // First snapshot — seed first-of-kind without chatter spam
            for (String id : normalized.keySet()) {
                CompanionRecentActionMemory.markFirstOfKind(playerId, id);
            }
            return;
        }

        List<String> gained = new ArrayList<>();
        for (Map.Entry<String, Integer> e : normalized.entrySet()) {
            int prev = before.getOrDefault(e.getKey(), 0);
            if (e.getValue() > prev) {
                gained.add(e.getKey());
            }
        }
        for (String id : gained) {
            handleGain(playerId, gameTime, id, before, normalized);
        }
    }

    private static void handleGain(
            UUID playerId,
            long gameTime,
            String itemId,
            Map<String, Integer> before,
            Map<String, Integer> after) {
        var craftReady = CompanionNotableItemSupport.craftCompletedByGain(itemId, before, after);
        if (craftReady.isPresent()) {
            String result = craftReady.get();
            String pretty = CompanionNotableItemSupport.prettyName(result);
            CompanionChatEventSupport.observe(
                    playerId, gameTime, CompanionRecentActionKind.CRAFT_READY,
                    "player now has the last materials to craft " + pretty,
                    result, true);
        }

        boolean first = CompanionRecentActionMemory.markFirstOfKind(playerId, itemId);
        // Craft results are recorded by loader craft hooks (ItemCrafted / result slot).
        // Inventory deltas only drive finds + craft-ready (last ingredient).
        if (CompanionNotableItemSupport.isBoring(itemId)) {
            return;
        }
        if (!CompanionNotableItemSupport.isNotablePickup(itemId) && !first) {
            return;
        }
        if (!CompanionNotableItemSupport.isNotablePickup(itemId) && first
                && !CompanionNotableItemSupport.isWeaponOrTool(itemId)
                && !CompanionNotableItemSupport.isArmor(itemId)
                && !CompanionNotableItemSupport.isRareOrValuable(itemId)) {
            return;
        }
        String pretty = CompanionNotableItemSupport.prettyName(itemId);
        String detail = first
                ? "player found their first " + pretty
                : "player found " + pretty;
        CompanionChatEventSupport.observe(
                playerId, gameTime, CompanionRecentActionKind.ITEM_FIND,
                detail, itemId, true);
    }

    /** Item ids loaders should count: notable gear + watched craft ingredients/results. */
    public static boolean shouldTrackCount(String itemId) {
        String id = CompanionNotableItemSupport.normalizeId(itemId);
        if (id.isEmpty()) {
            return false;
        }
        if (CompanionNotableItemSupport.isNotablePickup(id)
                || CompanionNotableItemSupport.looksLikeCraftResult(id)) {
            return true;
        }
        for (Map<String, Integer> need : CompanionNotableItemSupport.watchedCrafts().values()) {
            if (need.containsKey(id)) {
                return true;
            }
        }
        return CompanionNotableItemSupport.watchedCrafts().containsKey(id);
    }
}
