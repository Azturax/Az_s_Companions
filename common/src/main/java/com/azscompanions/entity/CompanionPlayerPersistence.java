package com.azscompanions.entity;

import com.azscompanions.AzsCompanionsConstants;
import com.azscompanions.ai.CompanionPersona;
import com.azscompanions.perk.WolfyPerkSupport;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Per-player companion settings + inventory persistence (world SavedData, not client).
 * <p>
 * Companions are discarded on charm dismiss, logout park, and some recoveries, then
 * recreated. Entity NBT / charm CustomData is not enough: charm payloads can be dropped,
 * {@code recruit()} creates a fresh empty entity, and restore paths historically forced
 * Follow mode. This schema is keyed by <strong>player UUID + companion identity</strong>
 * (Kon vs Wolfy vs Pecker vs each Bit) so data survives entity recreation.
 */
public final class CompanionPlayerPersistence {
    /** Overworld SavedData name (all loaders). */
    public static final String DATA_NAME = "azscompanions_player_companion_data";

    public static final String TAG_PLAYERS = "Players";
    public static final String TAG_PLAYER_UUID = "Player";
    public static final String TAG_COMPANIONS = "Companions";
    public static final String TAG_COMPANION_KEY = "Key";
    public static final String TAG_ENTITY_UUID = "EntityUuid";
    public static final String TAG_DATA = "Data";

    public static final String KEY_KON = "azscompanions:kon";
    public static final String KEY_WOLFY = "azscompanions:wolfy";
    public static final String KEY_PECKER = "azscompanions:pecker";
    public static final String KEY_BIT_PREFIX = "bit:";

    /**
     * NBT keys stored per player companion (settings UI + inventory + parked Bits).
     * Intentionally excludes Pos/Health/UUID/Owner and runtime Tasks.
     */
    public static final String[] PAYLOAD_NBT_KEYS = buildPayloadNbtKeys();

    private CompanionPlayerPersistence() {
    }

    /**
     * Stable store key: Bits are per-entity; named perk companions are distinct from Kon
     * even when they share the Kon definition id.
     */
    public static String companionKey(
            boolean childCompanion,
            UUID entityUuid,
            String definitionId,
            String displayName,
            boolean wolfyPerkFlag) {
        if (childCompanion && entityUuid != null) {
            return KEY_BIT_PREFIX + entityUuid;
        }
        if (wolfyPerkFlag || WolfyPerkSupport.isWolfyName(displayName)) {
            return KEY_WOLFY;
        }
        String name = displayName == null ? "" : displayName.trim();
        if (AzsCompanionsConstants.PECKER_COMPANION_NAME.equalsIgnoreCase(name)) {
            return KEY_PECKER;
        }
        if (definitionId == null || definitionId.isBlank()) {
            return KEY_KON;
        }
        return definitionId;
    }

    public static boolean isBitKey(String key) {
        return key != null && key.startsWith(KEY_BIT_PREFIX);
    }

    /**
     * Temporary teamfight leaders must not overwrite the player's Kon inventory/settings.
     * Child Bits still persist under {@code bit:&lt;uuid&gt;}.
     * CCI stream summons never persist as the player's saved companion.
     */
    public static boolean shouldPersist(boolean clientSide, boolean fightSpawn, boolean childCompanion) {
        return shouldPersist(clientSide, fightSpawn, childCompanion, false);
    }

    public static boolean shouldPersist(
            boolean clientSide, boolean fightSpawn, boolean childCompanion, boolean cciSummoned) {
        if (clientSide || cciSummoned) {
            return false;
        }
        return !fightSpawn || childCompanion;
    }

    /** True when a stored snapshot already has a follow/stay/sit/wander mode. */
    public static boolean snapshotHasMode(boolean containsModeKey) {
        return containsModeKey;
    }

    /**
     * When re-applying a payload, skip empty inventory so a new empty store entry cannot
     * wipe items that were just loaded from charm / entity NBT.
     */
    public static boolean shouldApplyInventory(boolean payloadInventoryEmpty, boolean livingInventoryEmpty) {
        if (payloadInventoryEmpty) {
            return livingInventoryEmpty;
        }
        return true;
    }

    private static String[] buildPayloadNbtKeys() {
        Set<String> keys = new LinkedHashSet<>();
        for (String key : CompanionDimensionTravel.IDENTITY_NBT_KEYS) {
            keys.add(key);
        }
        keys.add("Mode");
        keys.add("Inventory");
        keys.add(CompanionStoredChildren.NBT_LIST);
        keys.add("MaxChildren");
        keys.add("FollowRadius");
        keys.add("PersonalSpace");
        keys.add("WanderRadius");
        keys.add("Attitude");
        keys.add("TeamId");
        keys.add("HomePos");
        keys.add("HomeBedPos");
        keys.add("GuardCenter");
        keys.add("GuardRadius");
        keys.add("ChunkLoading");
        keys.add("KonBedGranted");
        keys.add("Trusted");
        keys.add("TrustedCount");
        keys.add("Permissions");
        keys.add("TeleportEnabled");
        keys.add(WolfyPerkSupport.COMPANION_NBT_FLAG);
        keys.add("Definition");
        keys.add(CompanionPersona.NBT_WHO);
        List<String> list = new ArrayList<>(keys);
        return list.toArray(new String[0]);
    }
}
