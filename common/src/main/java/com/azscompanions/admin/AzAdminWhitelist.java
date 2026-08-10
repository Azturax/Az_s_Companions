package com.azscompanions.admin;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

/**
 * Pure whitelist matching for {@code adminWhitelist} / {@code azAdminUsers}
 * (UUID strings and/or player names, case-insensitive names).
 */
public final class AzAdminWhitelist {
    private AzAdminWhitelist() {
    }

    public static boolean matches(Collection<String> entries, UUID uuid, String playerName) {
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        String uuidStr = uuid == null ? "" : uuid.toString();
        String uuidCompact = uuidStr.replace("-", "");
        String name = playerName == null ? "" : playerName.trim();
        String nameLower = name.toLowerCase(Locale.ROOT);
        for (String raw : entries) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String entry = raw.trim();
            if (!uuidStr.isEmpty() && entry.equalsIgnoreCase(uuidStr)) {
                return true;
            }
            String compact = entry.replace("-", "");
            if (!uuidCompact.isEmpty() && compact.equalsIgnoreCase(uuidCompact)
                    && compact.length() == 32) {
                return true;
            }
            if (!nameLower.isEmpty() && entry.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesAny(Collection<String> primary, Collection<String> alias,
                                     UUID uuid, String playerName) {
        return matches(primary, uuid, playerName) || matches(alias, uuid, playerName);
    }
}
