package com.azscompanions.entity;

/**
 * Shared home-bed sleep policy for all loaders.
 * Companions sleep only in azscompanions:kon_bed. If the claimed home bed is missing or unusable,
 * the claim is cleared and the nearest free companion bed is searched.
 */
public final class CompanionBedSleepSupport {
    public static final String KON_BED_ID = "azscompanions:kon_bed";
    public static final String KON_BED_PATH = "kon_bed";
    public static final int SEARCH_RADIUS = 48;
    public static final int SEARCH_VERTICAL = 48;

    public record IntPos(int x, int y, int z) {
        public int distManhattan(IntPos other) {
            return Math.abs(x - other.x) + Math.abs(y - other.y) + Math.abs(z - other.z);
        }
    }

    @FunctionalInterface
    public interface CompanionBedProbe {
        boolean isUsableCompanionBed(IntPos pos);
    }

    private CompanionBedSleepSupport() {
    }

    public static boolean isKonBedId(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return false;
        }
        String id = blockId.trim().toLowerCase(java.util.Locale.ROOT);
        return KON_BED_ID.equals(id) || id.endsWith(":" + KON_BED_PATH) || KON_BED_PATH.equals(id);
    }

    public static IntPos resolveSleepBed(IntPos origin, IntPos claimedHome, CompanionBedProbe probe) {
        if (probe == null || origin == null) {
            return null;
        }
        if (claimedHome != null && probe.isUsableCompanionBed(claimedHome)) {
            return claimedHome;
        }
        return findNearestUsableCompanionBed(origin, probe);
    }

    public static boolean isClaimInvalid(IntPos claimedHome, CompanionBedProbe probe) {
        if (claimedHome == null || probe == null) {
            return claimedHome != null;
        }
        return !probe.isUsableCompanionBed(claimedHome);
    }

    public static IntPos findNearestUsableCompanionBed(IntPos origin, CompanionBedProbe probe) {
        if (origin == null || probe == null) {
            return null;
        }
        IntPos best = null;
        int bestDist = Integer.MAX_VALUE;
        for (int dy = -SEARCH_VERTICAL; dy <= SEARCH_VERTICAL; dy++) {
            for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    IntPos cursor = new IntPos(origin.x + dx, origin.y + dy, origin.z + dz);
                    if (!probe.isUsableCompanionBed(cursor)) {
                        continue;
                    }
                    int dist = origin.distManhattan(cursor);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = cursor;
                    }
                }
            }
        }
        return best;
    }
}