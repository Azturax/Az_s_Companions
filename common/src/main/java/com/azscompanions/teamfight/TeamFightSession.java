package com.azscompanions.teamfight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-streamer team-fight runtime (enable flag, scores, bits, leaderboard).
 * CCI-first: all mutations go through CCI actions / commands that call this.
 */
public final class TeamFightSession {
    private static final Map<UUID, TeamFightSession> BY_OWNER = new ConcurrentHashMap<>();

    private final UUID ownerUuid;
    private boolean enabled;
    private boolean hudVisible;
    private String teamLeft = TeamFightDefaults.TEAM_LEFT;
    private String teamRight = TeamFightDefaults.TEAM_RIGHT;
    private int scoreLeft;
    private int scoreRight;
    private int bitsLeft;
    private int bitsRight;
    private int fightSpawns;
    private final Map<String, FighterStat> fighters = new LinkedHashMap<>();
    private final List<String> recentFights = new ArrayList<>();

    public record FighterStat(String name, String team, int kills, int bits, int wins) {
        FighterStat withKill() {
            return new FighterStat(name, team, kills + 1, bits, wins);
        }

        FighterStat withBits(int add) {
            return new FighterStat(name, team, kills, bits + Math.max(0, add), wins);
        }

        FighterStat withWin() {
            return new FighterStat(name, team, kills, bits, wins + 1);
        }
    }

    private TeamFightSession(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public static TeamFightSession of(UUID ownerUuid) {
        return BY_OWNER.computeIfAbsent(ownerUuid, TeamFightSession::new);
    }

    /**
     * Like {@link #of(UUID)} but applies {@code enableByDefault} only when creating a new session.
     */
    public static TeamFightSession of(UUID ownerUuid, boolean enableByDefault) {
        return BY_OWNER.computeIfAbsent(ownerUuid, uuid -> {
            TeamFightSession session = new TeamFightSession(uuid);
            if (enableByDefault) {
                session.setEnabled(true);
            }
            return session;
        });
    }

    public static void clearAll() {
        BY_OWNER.clear();
    }

    public UUID ownerUuid() {
        return ownerUuid;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isHudVisible() {
        return hudVisible;
    }

    public void setEnabled(boolean value) {
        this.enabled = value;
        if (value) {
            this.hudVisible = true;
        } else {
            this.hudVisible = false;
        }
    }

    public void setHudVisible(boolean visible) {
        this.hudVisible = visible && enabled;
    }

    public String teamLeft() {
        return teamLeft;
    }

    public String teamRight() {
        return teamRight;
    }

    public void setTeams(String left, String right) {
        if (left != null && !left.isBlank()) {
            teamLeft = left.trim().toLowerCase(Locale.ROOT);
        }
        if (right != null && !right.isBlank()) {
            teamRight = right.trim().toLowerCase(Locale.ROOT);
        }
    }

    public int scoreLeft() {
        return scoreLeft;
    }

    public int scoreRight() {
        return scoreRight;
    }

    public int bitsLeft() {
        return bitsLeft;
    }

    public int bitsRight() {
        return bitsRight;
    }

    public int fightSpawns() {
        return fightSpawns;
    }

    public void addFightSpawn() {
        fightSpawns++;
    }

    public boolean canSpawnMore(int max) {
        return fightSpawns < Math.max(1, max);
    }

    public void addScore(String team, int points) {
        String t = team == null ? "" : team.trim().toLowerCase(Locale.ROOT);
        if (t.equals(teamLeft)) {
            scoreLeft += points;
        } else if (t.equals(teamRight)) {
            scoreRight += points;
        }
    }

    public void addBits(String team, int bits) {
        String t = team == null ? "" : team.trim().toLowerCase(Locale.ROOT);
        int b = Math.max(0, bits);
        if (t.equals(teamLeft)) {
            bitsLeft += b;
        } else if (t.equals(teamRight)) {
            bitsRight += b;
        }
    }

    public void recordFighter(String name, String team, int bitsContributed) {
        if (name == null || name.isBlank()) {
            return;
        }
        String key = name.trim();
        FighterStat existing = fighters.get(key);
        if (existing == null) {
            fighters.put(key, new FighterStat(key, team == null ? "" : team, 0, Math.max(0, bitsContributed), 0));
        } else {
            fighters.put(key, existing.withBits(bitsContributed));
        }
    }

    public void recordKill(String killerName) {
        recordKill(killerName, null);
    }

    public void recordKill(String killerName, String killerTeamFallback) {
        if (killerName == null || killerName.isBlank()) {
            return;
        }
        String key = killerName.trim();
        FighterStat existing = fighters.get(key);
        if (existing == null) {
            existing = new FighterStat(key, killerTeamFallback == null ? "" : killerTeamFallback, 0, 0, 0);
        }
        fighters.put(key, existing.withKill());
        String team = existing.team() == null || existing.team().isBlank()
                ? (killerTeamFallback == null ? "" : killerTeamFallback)
                : existing.team();
        addScore(team, 1);
    }

    /**
     * Scores a PvP kill between owned fight companions on different teams.
     * @return true if the scoreboard changed
     */
    public boolean tryRecordTeamKill(String killerName, String killerTeam, String victimName, String victimTeam) {
        if (!enabled) {
            return false;
        }
        String kt = killerTeam == null ? "" : killerTeam.trim();
        String vt = victimTeam == null ? "" : victimTeam.trim();
        if (kt.isBlank() || vt.isBlank() || kt.equalsIgnoreCase(vt)) {
            return false;
        }
        recordKill(killerName, kt);
        noteFight((killerName == null ? "?" : killerName) + " defeated " + (victimName == null ? "?" : victimName));
        return true;
    }

    public void noteFight(String summary) {
        if (summary == null || summary.isBlank()) {
            return;
        }
        recentFights.add(0, summary.trim());
        while (recentFights.size() > 8) {
            recentFights.remove(recentFights.size() - 1);
        }
    }

    public List<FighterStat> topByBits(int limit) {
        return fighters.values().stream()
                .sorted(Comparator.comparingInt(FighterStat::bits).reversed())
                .limit(Math.max(1, limit))
                .toList();
    }

    public List<FighterStat> topByKills(int limit) {
        return fighters.values().stream()
                .sorted(Comparator.comparingInt(FighterStat::kills).reversed())
                .limit(Math.max(1, limit))
                .toList();
    }

    public List<String> recentFights() {
        return List.copyOf(recentFights);
    }

    public void resetScores() {
        scoreLeft = 0;
        scoreRight = 0;
        bitsLeft = 0;
        bitsRight = 0;
        fighters.clear();
        recentFights.clear();
        fightSpawns = 0;
    }

    public TeamFightHudSnapshot snapshot() {
        List<String> leftNames = new ArrayList<>();
        List<String> rightNames = new ArrayList<>();
        for (FighterStat f : fighters.values()) {
            if (teamLeft.equalsIgnoreCase(f.team())) {
                leftNames.add(f.name());
            } else if (teamRight.equalsIgnoreCase(f.team())) {
                rightNames.add(f.name());
            }
        }
        return new TeamFightHudSnapshot(
                enabled,
                hudVisible,
                teamLeft,
                teamRight,
                scoreLeft,
                scoreRight,
                bitsLeft,
                bitsRight,
                BitGearTiers.priceTableText(),
                String.join(",", leftNames),
                String.join(",", rightNames),
                formatTop(topByBits(3), "bits"),
                formatTop(topByKills(3), "kills")
        );
    }

    private static String formatTop(List<FighterStat> list, String metric) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            FighterStat f = list.get(i);
            if (i > 0) {
                sb.append(" | ");
            }
            int val = "kills".equals(metric) ? f.kills() : f.bits();
            sb.append(f.name()).append(' ').append(val);
        }
        return sb.toString();
    }
}
