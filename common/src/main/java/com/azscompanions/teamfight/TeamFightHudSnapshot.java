package com.azscompanions.teamfight;

/**
 * Compact HUD payload synced S2C. Format uses {@code |} separators (no newlines).
 */
public record TeamFightHudSnapshot(
        boolean enabled,
        boolean hudVisible,
        String teamLeft,
        String teamRight,
        int scoreLeft,
        int scoreRight,
        int bitsLeft,
        int bitsRight,
        String priceTable,
        String membersLeft,
        String membersRight,
        String topBits,
        String topKills
) {
    public static final TeamFightHudSnapshot HIDDEN = new TeamFightHudSnapshot(
            false, false, "red", "blue", 0, 0, 0, 0, "", "", "", "", "");

    public String encode() {
        return String.join("\u001f",
                enabled ? "1" : "0",
                hudVisible ? "1" : "0",
                safe(teamLeft),
                safe(teamRight),
                Integer.toString(scoreLeft),
                Integer.toString(scoreRight),
                Integer.toString(bitsLeft),
                Integer.toString(bitsRight),
                safe(priceTable),
                safe(membersLeft),
                safe(membersRight),
                safe(topBits),
                safe(topKills));
    }

    public static TeamFightHudSnapshot decode(String raw) {
        if (raw == null || raw.isBlank()) {
            return HIDDEN;
        }
        String[] p = raw.split("\u001f", -1);
        if (p.length < 13) {
            return HIDDEN;
        }
        return new TeamFightHudSnapshot(
                "1".equals(p[0]),
                "1".equals(p[1]),
                p[2],
                p[3],
                parseInt(p[4]),
                parseInt(p[5]),
                parseInt(p[6]),
                parseInt(p[7]),
                p[8],
                p[9],
                p[10],
                p[11],
                p[12]
        );
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace('\u001f', ' ');
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
