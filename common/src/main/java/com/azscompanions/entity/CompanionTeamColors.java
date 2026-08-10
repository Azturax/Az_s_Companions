package com.azscompanions.entity;

import java.util.Locale;

/**
 * Optional nametag tint for companion {@code teamId} values.
 * Unknown teams stay default white; known colors help stream overlays.
 */
public final class CompanionTeamColors {
    private CompanionTeamColors() {
    }

    /** ARGB color for nametag rendering, or {@code 0xFFFFFF} when unset/unknown. */
    public static int nametagRgb(String teamId) {
        if (teamId == null || teamId.isBlank()) {
            return 0xFFFFFF;
        }
        return switch (teamId.trim().toLowerCase(Locale.ROOT)) {
            case "red", "crimson", "rose" -> 0xFF5555;
            case "blue", "cyan", "azure" -> 0x5555FF;
            case "green", "lime", "emerald" -> 0x55FF55;
            case "yellow", "gold" -> 0xFFFF55;
            case "purple", "violet", "magenta" -> 0xAA55FF;
            case "orange" -> 0xFFAA00;
            case "white", "light" -> 0xFFFFFF;
            case "black", "dark" -> 0x555555;
            case "pink" -> 0xFF55FF;
            case "aqua", "teal" -> 0x55FFFF;
            default -> hashTint(teamId);
        };
    }

    private static int hashTint(String teamId) {
        int h = teamId.toLowerCase(Locale.ROOT).hashCode();
        int r = 0x60 + ((h >>> 16) & 0x9F);
        int g = 0x60 + ((h >>> 8) & 0x9F);
        int b = 0x60 + (h & 0x9F);
        return (r << 16) | (g << 8) | b;
    }

    public static boolean sameTeam(String a, String b) {
        if (a == null || a.isBlank() || b == null || b.isBlank()) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }
}
