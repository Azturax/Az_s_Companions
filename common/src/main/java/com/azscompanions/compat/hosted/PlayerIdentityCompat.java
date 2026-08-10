package com.azscompanions.compat.hosted;

import java.util.Locale;
import java.util.UUID;

/**
 * Owner identity helpers for hosted / LAN multiplayer.
 * Primary match is always UUID; optional name fallback covers offline↔online UUID remaps
 * (e.g. Open to LAN + LAN Server Properties hybrid, some tunnel hosts).
 * <p>
 * Never enable name fallback on dedicated servers — see {@link IntegratedMultiplayerCompat}.
 */
public final class PlayerIdentityCompat {
    private PlayerIdentityCompat() {
    }

    /**
     * @param ownerUuid           stored companion owner UUID (may be null)
     * @param ownerName           last-known owner profile name (may be blank)
     * @param playerUuid          current player UUID
     * @param playerName          current player profile name
     * @param allowNameFallback   when true, same profile name counts as owner
     */
    public static boolean isOwner(
            UUID ownerUuid,
            String ownerName,
            UUID playerUuid,
            String playerName,
            boolean allowNameFallback
    ) {
        if (playerUuid == null) {
            return false;
        }
        if (ownerUuid != null && ownerUuid.equals(playerUuid)) {
            return true;
        }
        if (!allowNameFallback) {
            return false;
        }
        return namesMatch(ownerName, playerName);
    }

    public static boolean namesMatch(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        String left = a.trim();
        String right = b.trim();
        if (left.isEmpty() || right.isEmpty()) {
            return false;
        }
        return left.equalsIgnoreCase(right);
    }

    /** Normalize for storage / compare. */
    public static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim();
    }

    public static String debugLabel(UUID uuid, String name) {
        String n = normalizeName(name);
        if (uuid == null && n.isEmpty()) {
            return "(no owner)";
        }
        if (n.isEmpty()) {
            return String.valueOf(uuid);
        }
        if (uuid == null) {
            return n;
        }
        return n + "/" + uuid;
    }

    public static boolean shouldHealOwnerUuid(UUID storedUuid, UUID playerUuid, String storedName, String playerName) {
        if (playerUuid == null || storedUuid == null) {
            return false;
        }
        if (storedUuid.equals(playerUuid)) {
            return false;
        }
        return namesMatch(storedName, playerName);
    }

    /** Case-fold for maps; not used for display. */
    public static String foldKey(String name) {
        return normalizeName(name).toLowerCase(Locale.ROOT);
    }
}
