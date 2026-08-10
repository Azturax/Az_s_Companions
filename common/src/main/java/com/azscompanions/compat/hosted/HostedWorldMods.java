package com.azscompanions.compat.hosted;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Known “friends join my singleplayer / LAN world over the internet” mods for MC 1.21.1.
 * Soft-detect only — no compile dependency on any of these.
 */
public final class HostedWorldMods {
    /** SparkUniverse Essential (essential.gg / essential.com). */
    public static final String ESSENTIAL = "essential";
    /** e4mc — Open to LAN reverse tunnel. */
    public static final String E4MC = "e4mc";
    /** World Host — host SP without separate server / port forward. */
    public static final String WORLD_HOST = "world-host";
    /** Alternate World Host id seen in some metadata. */
    public static final String WORLD_HOST_ALT = "worldhost";
    /** LAN Server Properties — offline/hybrid auth for LAN hosts. */
    public static final String LAN_SERVER_PROPERTIES = "lanserverproperties";

    /** All ids we soft-check (order = docs priority). */
    public static final List<String> ALL_IDS = List.of(
            ESSENTIAL,
            E4MC,
            WORLD_HOST,
            WORLD_HOST_ALT,
            LAN_SERVER_PROPERTIES
    );

    private HostedWorldMods() {
    }

    /**
     * @param isLoaded loader predicate ({@code ModList.isLoaded} / Fabric {@code isModLoaded})
     * @return present mod ids from {@link #ALL_IDS}
     */
    public static List<String> detectPresent(Predicate<String> isLoaded) {
        List<String> found = new ArrayList<>();
        if (isLoaded == null) {
            return found;
        }
        for (String id : ALL_IDS) {
            try {
                if (isLoaded.test(id)) {
                    found.add(id);
                }
            } catch (Throwable ignored) {
                // Soft-dep: never fail bootstrap because a loader probe threw.
            }
        }
        return found;
    }

    public static String describe(List<String> present) {
        if (present == null || present.isEmpty()) {
            return "none";
        }
        return String.join(", ", present);
    }

    public static boolean includesEssential(List<String> present) {
        if (present == null) {
            return false;
        }
        for (String id : present) {
            if (ESSENTIAL.equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    public static String displayName(String modId) {
        if (modId == null) {
            return "?";
        }
        return switch (modId.toLowerCase(Locale.ROOT)) {
            case ESSENTIAL -> "Essential";
            case E4MC -> "e4mc";
            case WORLD_HOST, WORLD_HOST_ALT -> "World Host";
            case LAN_SERVER_PROPERTIES -> "LAN Server Properties";
            default -> modId;
        };
    }
}
