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
     * Special perk player: survival flight, glowing, and flying companion while online.
     */
    public static final UUID SPECIAL_PERK_PLAYER_UUID =
            UUID.fromString("4274c47f-d61f-4850-bf29-9e5c185db4ac");

    /**
     * Special nametag player: always-visible display name {@code meow} (and companion if owned).
     */
    public static final UUID MEOW_NAMETAG_PLAYER_UUID =
            UUID.fromString("42901453-b2b5-4d95-9b7b-e0ed40da504f");

    public static final String MEOW_NAMETAG = "meow";

    /**
     * Mister Wiggly: companion-of-companion dog sidekick while his companion is summoned.
     */
    public static final UUID MISTER_WIGGLY_PLAYER_UUID =
            UUID.fromString("5b0a2d0a-fd88-49b6-9138-d0103af9a0d5");

    /**
     * Stream-dog display name. Sources refer to Mister Wiggly's pet as the stream dog /
     * “Mr Wiggly bot”; short nametag {@code Wiggly} with a blue collar.
     */
    public static final String WIGGLY_DOG_NAME = "Wiggly";

    private AzsCompanionsConstants() {
    }
}
