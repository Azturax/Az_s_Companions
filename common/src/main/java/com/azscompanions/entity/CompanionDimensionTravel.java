package com.azscompanions.entity;

import com.azscompanions.ai.CompanionPersona;

import java.util.Locale;
import java.util.Objects;

/**
 * Shared helpers for owner dimension changes (vanilla Nether/End <em>and</em> any modded dimension).
 * <p>
 * Detection is by dimension registry key / {@code ResourceLocation} identity — no mod-id allowlist.
 * Persona/model are one global identity per world save; dimension travel must never reset them
 * or re-open first-create onboarding.
 */
public final class CompanionDimensionTravel {
    private CompanionDimensionTravel() {
    }

    /**
     * True when the owner moved between two distinct dimension registry keys
     * (Overworld↔Nether, Ad Astra planets, RFTools dims, Twilight Forest, etc.).
     */
    public static boolean isDimensionChange(Object fromKey, Object toKey) {
        if (fromKey == null || toKey == null) {
            return false;
        }
        return !Objects.equals(fromKey, toKey);
    }

    /** Stable string id for logging / soft-compat notes ({@code namespace:path}). */
    public static String dimensionId(Object resourceKeyOrLocation) {
        if (resourceKeyOrLocation == null) {
            return "";
        }
        String raw = resourceKeyOrLocation.toString();
        if (raw == null || raw.isBlank()) {
            return "";
        }
        // ResourceKey#toString is often "ResourceKey[minecraft:dimension / minecraft:overworld]"
        int slash = raw.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < raw.length()) {
            String tail = raw.substring(slash + 1).replace("]", "").trim();
            if (!tail.isEmpty()) {
                return tail.toLowerCase(Locale.ROOT);
            }
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    /** True for the vanilla overworld id only (extension point — not used to gate persistence). */
    public static boolean isVanillaOverworldId(String dimensionId) {
        String id = dimensionId(dimensionId);
        return "minecraft:overworld".equals(id);
    }

    /**
     * NBT keys that define the companion's global appearance + persona identity for a world save.
     * Loaders copy these into {@link CompanionIdentityPersistence} and reapply after portals.
     */
    public static final String[] IDENTITY_NBT_KEYS = {
            "Definition",
            "SkinPath",
            "SkinSleeping",
            "SkinBathing",
            "SkinAdventuring",
            "SlimArms",
            "Gender",
            "BodyScale",
            "Bust",
            "Waist",
            "Hips",
            "Shoulders",
            "BustOffset",
            "CustomNameOverride",
            "CompanionForm",
            CompanionFormVariants.NBT_KEY,
            "ShowNameTag",
            "ShowArmor",
            CompanionPersona.NBT_WHO,
            CompanionPersona.NBT_WHAT,
            CompanionPersona.NBT_HOW,
            CompanionPersona.NBT_SPEECH,
            CompanionPersona.NBT_RELATIONSHIP,
            CompanionPersona.NBT_QUIRKS,
            CompanionPersona.NBT_INITIALIZED,
            "Pronouns",
            "BehaviorStyle",
            "VoiceProfile",
    };

    /** True when identity NBT should count as "persona already set" (skip first-create UI). */
    public static boolean identityMarksPersonaInitialized(
            boolean initializedFlag,
            boolean hasWho,
            boolean hasWhat,
            boolean hasHow,
            boolean hasSpeech,
            boolean hasRelationship,
            boolean hasQuirks
    ) {
        return initializedFlag || hasWho || hasWhat || hasHow || hasSpeech || hasRelationship || hasQuirks;
    }
}
