package com.azscompanions;

import java.util.UUID;

/**
 * Loader-agnostic constants shared by Fabric and NeoForge modules.
 */
public final class AzsCompanionsConstants {
    public static final String MOD_ID = "azscompanions";
    public static final String MOD_NAME = "Az's Companions";

    /** Declared Minecraft support window. */
    public static final String MIN_MINECRAFT = "1.21.1";
    public static final String MAX_MINECRAFT = "26.2";

    /**
     * Special perk player: survival flight and flying companion.
     * No auto-glowing (glow removed for this UUID). Also eligible for the toggle Wiggly dog
     * (defaults OFF — opt-in via {@code /az wiggly}).
     */
    public static final UUID SPECIAL_PERK_PLAYER_UUID =
            UUID.fromString("4274c47f-d61f-4850-bf29-9e5c185db4ac");

    /**
     * Client cosmetic: cute Kon-style ears on this player's head (UUID-gated render layer).
     */
    public static final UUID KON_EARS_PLAYER_UUID =
            UUID.fromString("42901453-b2b5-4d95-9b7b-e0ed40da504f");

    /**
     * Mister Wiggly: companion-of-companion dog sidekick while his companion is summoned,
     * plus toggle Wiggly dog (defaults ON when no companion sidekick is active).
     */
    public static final UUID MISTER_WIGGLY_PLAYER_UUID =
            UUID.fromString("5b0a2d0a-fd88-49b6-9138-d0103af9a0d5");

    /**
     * Stream-dog display name. Sources refer to Mister Wiggly's pet as the stream dog /
     * “Mr Wiggly bot”; short nametag {@code Wiggly} with a blue collar.
     */
    public static final String WIGGLY_DOG_NAME = "Wiggly";

    /**
     * Pecker: chicken-form companion default for this owner on recruit/summon.
     */
    public static final UUID PECKER_PLAYER_UUID =
            UUID.fromString("966ebb69-a63d-4bb2-ac90-ed39d8c64b80");

    public static final String PECKER_COMPANION_NAME = "Pecker";

    /**
     * Wolfy: one-time brown wolf-form companion grant for this owner on join / perk apply.
     */
    public static final UUID WOLFY_PLAYER_UUID =
            UUID.fromString("7c97e337-2c49-448c-b710-7655487f18df");

    public static final String WOLFY_COMPANION_NAME = "Wolfy";

    /**
     * Toggleable player-following dog (pink collar) for {@link #MISTER_WIGGLY_PLAYER_UUID}
     * (default ON) and {@link #SPECIAL_PERK_PLAYER_UUID} (default OFF).
     * Flies only while the owner is actively flying — same rule as special companions.
     * Hard-capped to one dog per owner.
     */
    public static final String TOGGLE_WIGGLY_DOG_NAME = "Wiggly";

    /**
     * Chat rank prefix for this owner's companion lines (and CCI summons they own).
     * Not a vanilla scoreboard team — applied by {@code CompanionChatFormat}.
     */
    public static final UUID BRAT_PLAYER_UUID =
            UUID.fromString("324ca5e2-c2e1-4b50-be3d-01198293e919");

    public static final String BRAT_CHAT_PREFIX = "BRAT";

    private AzsCompanionsConstants() {
    }

    public static boolean isPeckerOwner(UUID uuid) {
        return uuid != null && PECKER_PLAYER_UUID.equals(uuid);
    }

    public static boolean isWolfyOwner(UUID uuid) {
        return uuid != null && WOLFY_PLAYER_UUID.equals(uuid);
    }

    public static boolean isBratOwner(UUID uuid) {
        return uuid != null && BRAT_PLAYER_UUID.equals(uuid);
    }
}
