package com.azscompanions.teamfight;

/**
 * Client-side HUD cache updated by S2C packets.
 */
public final class ClientTeamFightHud {
    private static volatile TeamFightHudSnapshot snapshot = TeamFightHudSnapshot.HIDDEN;

    private ClientTeamFightHud() {
    }

    public static void apply(String encoded) {
        snapshot = TeamFightHudSnapshot.decode(encoded);
    }

    public static void apply(TeamFightHudSnapshot next) {
        snapshot = next == null ? TeamFightHudSnapshot.HIDDEN : next;
    }

    public static TeamFightHudSnapshot get() {
        return snapshot;
    }

    public static boolean shouldRender() {
        TeamFightHudSnapshot s = snapshot;
        return s.enabled() && s.hudVisible();
    }
}
