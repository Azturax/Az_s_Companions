package com.azscompanions.util;

import com.azscompanions.AzsCompanions;
import net.minecraftforge.fml.ModList;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/**
 * Runtime helpers for the supported Minecraft / NeoForge window:
 * MC/NeoForge 1.21.1 (Neo 21.1.x) through 26.2 (Neo 26.2.x).
 */
public final class ModVersionCompat {
    public static final String MIN_MINECRAFT = "1.21.1";
    public static final String MAX_MINECRAFT_INCLUSIVE = "26.2";
    public static final String MIN_NEOFORGE = "21.1";
    public static final String MAX_NEOFORGE_INCLUSIVE = "26.2";

    private ModVersionCompat() {
    }

    public static String minecraftVersion() {
        return ModList.get().getModContainerById("minecraft")
                .map(c -> c.getModInfo().getVersion())
                .map(ArtifactVersion::toString)
                .orElse("unknown");
    }

    public static String neoForgeVersion() {
        return ModList.get().getModContainerById("neoforge")
                .map(c -> c.getModInfo().getVersion())
                .map(ArtifactVersion::toString)
                .orElse("unknown");
    }

    public static boolean isWithinSupportedWindow() {
        return isAtLeast(minecraftVersion(), MIN_MINECRAFT)
                && isAtMostMajorMinor(minecraftVersion(), MAX_MINECRAFT_INCLUSIVE)
                && isAtLeast(neoForgeVersion(), MIN_NEOFORGE)
                && isAtMostMajorMinor(neoForgeVersion(), MAX_NEOFORGE_INCLUSIVE);
    }

    public static void logSupportBanner() {
        AzsCompanions.LOGGER.info(
                "Az's Companions compatibility window: Minecraft {}–{}, NeoForge {}–{} | running MC {} / Neo {}",
                MIN_MINECRAFT, MAX_MINECRAFT_INCLUSIVE,
                MIN_NEOFORGE, MAX_NEOFORGE_INCLUSIVE,
                minecraftVersion(), neoForgeVersion());
        if (!isWithinSupportedWindow()) {
            AzsCompanions.LOGGER.warn("Current game versions are outside the declared support window");
        }
    }

    /** True when running on the NeoForge 26.x / Minecraft 26.x line. */
    public static boolean isNeo26Line() {
        ArtifactVersion neo = new DefaultArtifactVersion(neoForgeVersion());
        return neo.getMajorVersion() >= 26;
    }

    /** True when running on the 1.21.1 / NeoForge 21.1 development line. */
    public static boolean isNeo21_1Line() {
        ArtifactVersion neo = new DefaultArtifactVersion(neoForgeVersion());
        return neo.getMajorVersion() == 21 && neo.getMinorVersion() == 1;
    }

    private static boolean isAtLeast(String current, String minimum) {
        return new DefaultArtifactVersion(stripPre(current))
                .compareTo(new DefaultArtifactVersion(minimum)) >= 0;
    }

    private static boolean isAtMostMajorMinor(String current, String maxMajorMinor) {
        ArtifactVersion cur = new DefaultArtifactVersion(stripPre(current));
        ArtifactVersion max = new DefaultArtifactVersion(maxMajorMinor);
        if (cur.getMajorVersion() != max.getMajorVersion()) {
            return cur.getMajorVersion() < max.getMajorVersion();
        }
        return cur.getMinorVersion() <= max.getMinorVersion();
    }

    private static String stripPre(String version) {
        int plus = version.indexOf('+');
        if (plus >= 0) {
            version = version.substring(0, plus);
        }
        // Keep numeric core before -beta/-rc for coarse comparisons.
        int dash = version.indexOf('-');
        return dash >= 0 ? version.substring(0, dash) : version;
    }
}
