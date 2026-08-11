package com.azscompanions.perk;

import com.azscompanions.AzsCompanionsConstants;

import java.util.Locale;
import java.util.UUID;

/**
 * Loader-agnostic helpers for the UUID-gated Wolfy companion perk.
 * <p>
 * Grant is one-shot via {@link #PLAYER_GRANTED_TAG}:
 * Fabric / NeoForge 1.21.1 use the vanilla player scoreboard tag; NeoForge 26.2 stores the
 * same key on {@code Entity#getPersistentData()}.
 * Before marking granted, loaders also treat an existing owned Wolfy (world or charm
 * storage) as already granted so logins never duplicate.
 */
public final class WolfyPerkSupport {
    /** Persists on the player entity (vanilla scoreboard tags). */
    public static final String PLAYER_GRANTED_TAG = "azscompanions.wolfy_granted";

    /** Companion entity NBT flag written on perk spawn. */
    public static final String COMPANION_NBT_FLAG = "WolfyPerk";

    /**
     * Minecraft 1.21+ wolf variant id for a brown coat ({@code chestnut}).
     * {@code rusty} is reddish; chestnut is the brown biome coat.
     */
    public static final String BROWN_WOLF_VARIANT_ID = "minecraft:chestnut";

    private WolfyPerkSupport() {
    }

    public static boolean isWolfyOwner(UUID uuid) {
        return uuid != null && AzsCompanionsConstants.WOLFY_PLAYER_UUID.equals(uuid);
    }

    public static boolean isWolfyName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return AzsCompanionsConstants.WOLFY_COMPANION_NAME.equalsIgnoreCase(name.trim());
    }

    /** True when stored companion NBT looks like the perk Wolfy. */
    public static boolean looksLikeStoredWolfy(String customNameOverride, boolean wolfyPerkFlag) {
        return wolfyPerkFlag || isWolfyName(customNameOverride);
    }

    public static String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
