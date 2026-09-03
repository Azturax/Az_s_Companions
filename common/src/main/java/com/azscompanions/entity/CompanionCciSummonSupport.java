package com.azscompanions.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Loader-agnostic rules for CCI / streamer temporary summons (sub gift, bits, CommandOutcome).
 * Charm-owned companions never receive these flags and never auto-expire.
 */
public final class CompanionCciSummonSupport {
    public static final String NBT_SUMMONED = "CciSummoned";
    public static final String NBT_EXPIRE_AT = "CciExpireAtGameTime";
    public static final String NBT_MAX_HEALTH = "CciMaxHealth";

    /** Default timed-death window when CCI/command omit duration. */
    public static final int DEFAULT_DURATION_SECONDS = 90;
    public static final int MAX_DURATION_SECONDS = 3600;
    public static final float MIN_HEALTH = 1.0f;
    public static final float MAX_HEALTH_VALUE = 1024.0f;

    public static final String SKIP_TOKEN = "-";

    /** Default {@code /az summon} behavior mode (same as charm companions). */
    public static final String DEFAULT_MODE = "follow";

    /**
     * Default {@code /az summon} / CCI type when omitted: player-form with a random Steve or Alex skin.
     * Explicit {@code kon}/{@code bits}/{@code wiggly} still use those appearances.
     */
    public static final String DEFAULT_TYPE = "player";

    private CompanionCciSummonSupport() {
    }

    /**
     * Clamp a summon lifetime. {@code 0} (and negative) means no expiry — testing only.
     * CCI defaults should pass {@link #DEFAULT_DURATION_SECONDS} when the arg is omitted.
     */
    public static int clampDurationSeconds(int seconds) {
        if (seconds <= 0) {
            return 0;
        }
        return Math.min(MAX_DURATION_SECONDS, seconds);
    }

    /**
     * Game-time tick when the CCI companion should die. {@code 0} = never.
     */
    public static long expireAtGameTime(long gameTime, int durationSeconds) {
        int clamped = clampDurationSeconds(durationSeconds);
        if (clamped <= 0) {
            return 0L;
        }
        return gameTime + (long) clamped * 20L;
    }

    public static boolean shouldExpire(boolean cciSummoned, long expireAtGameTime, long currentGameTime) {
        return cciSummoned && expireAtGameTime > 0L && currentGameTime >= expireAtGameTime;
    }

    /** Temporary CCI summons must not be parked into charm / logout persistence. */
    public static boolean shouldParkOnLogout(boolean cciSummoned) {
        return !cciSummoned;
    }

    public static float clampHealth(float health) {
        if (Float.isNaN(health) || health <= 0.0f) {
            return MIN_HEALTH;
        }
        return Math.min(MAX_HEALTH_VALUE, health);
    }

    public static boolean isSkipToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        String v = raw.trim();
        return v.equals(SKIP_TOKEN)
                || v.equalsIgnoreCase("none")
                || v.equalsIgnoreCase("default")
                || v.equalsIgnoreCase("skip")
                || v.equalsIgnoreCase("*");
    }

    /**
     * Canonical companion behavior mode for {@code /az summon}.
     * Idle → wander; attack → guard. Unknown / skip → {@link #DEFAULT_MODE}.
     */
    public static String resolveBehaviorMode(String raw) {
        if (isSkipToken(raw)) {
            return DEFAULT_MODE;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (key) {
            case "follow" -> "follow";
            case "stay" -> "stay";
            case "sit" -> "sit";
            case "idle", "wander" -> "wander";
            case "attack", "guard" -> "guard";
            case "patrol" -> "patrol";
            case "home" -> "home";
            case "task" -> "task";
            default -> DEFAULT_MODE;
        };
    }

    public static boolean looksLikeBehaviorMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        if (isSkipToken(raw)) {
            return true;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (key) {
            case "follow", "stay", "sit", "idle", "wander", "attack", "guard", "patrol", "home", "task" -> true;
            default -> false;
        };
    }

    /**
     * Mode sits after shield. If that token is not a mode (and not {@code -}), treat it as the
     * nametag so {@code ... - Alice} still names the summon.
     *
     * @return {@code [canonicalMode, nameOrEmpty]}
     */
    public static String[] splitModeAndName(String modeOrName, String name) {
        String resolvedName = name == null ? "" : name.trim();
        if (looksLikeBehaviorMode(modeOrName)) {
            return new String[] { resolveBehaviorMode(modeOrName), resolvedName };
        }
        if (!isSkipToken(modeOrName) && resolvedName.isEmpty()) {
            return new String[] { DEFAULT_MODE, modeOrName.trim() };
        }
        return new String[] { DEFAULT_MODE, resolvedName };
    }

    public static boolean displayNameMatches(String displayName, String query) {
        if (query == null || query.isBlank() || displayName == null) {
            return false;
        }
        return displayName.trim().equalsIgnoreCase(query.trim());
    }

    public static String sanitizeDisplayName(String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim();
        if (trimmed.length() > 32) {
            trimmed = trimmed.substring(0, 32);
        }
        return trimmed;
    }

    /**
     * Resolve {@code kon}/{@code bits}/{@code wiggly}/{@code dox} plus form names and datapack ids.
     * Blank / {@code player} defaults to a player-form companion with a random Steve or Alex skin
     * (not Kon’s texture). Wiggly maps to wolf form (not the UUID perk dog). Bits are scaled down
     * but are not charm children.
     */
    public static TypeSpec resolveType(String raw) {
        if (raw == null || raw.isBlank()) {
            return TypeSpec.playerDefault();
        }
        String token = raw.trim();
        int colon = token.indexOf(':');
        String path = colon >= 0 ? token.substring(colon + 1) : token;
        String key = path.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (key) {
            case "kon", "dox" -> TypeSpec.kon();
            case "bit", "bits" -> TypeSpec.bits();
            case "wiggly", "wiggles", "mister_wiggly" -> TypeSpec.wiggly();
            case "player", "human", "person", "skin" -> TypeSpec.playerDefault();
            case "steve" -> TypeSpec.steve();
            case "alex" -> TypeSpec.alex();
            case "wolfy" -> new TypeSpec("kon", "wolf", 1.0f, false, VanillaPlayerPick.NONE);
            default -> {
                CompanionForm form = tryForm(key);
                if (form != null) {
                    yield new TypeSpec("kon", form.serializedName(), 1.0f, false, VanillaPlayerPick.NONE);
                }
                yield new TypeSpec(path, "player", 1.0f, false, VanillaPlayerPick.NONE);
            }
        };
    }

    /**
     * Expand {@code diamond} / {@code iron} into a four-piece set, or pass through a single item id.
     * Numeric protection values are ignored (companions equip items, not raw armor points).
     */
    public static List<String> armorItemIds(String armor) {
        List<String> out = new ArrayList<>();
        if (isSkipToken(armor)) {
            return out;
        }
        String v = armor.trim().toLowerCase(Locale.ROOT);
        if (v.matches("\\d+(\\.\\d+)?")) {
            return out;
        }
        String material = switch (v) {
            case "leather" -> "leather";
            case "chain", "chainmail" -> "chainmail";
            case "iron" -> "iron";
            case "gold", "golden" -> "golden";
            case "diamond" -> "diamond";
            case "netherite" -> "netherite";
            default -> null;
        };
        if (material != null) {
            out.add("minecraft:" + material + "_helmet");
            out.add("minecraft:" + material + "_chestplate");
            out.add("minecraft:" + material + "_leggings");
            out.add("minecraft:" + material + "_boots");
            return out;
        }
        out.add(v.contains(":") ? v : "minecraft:" + v);
        return out;
    }

    public static boolean wantsPlayerSkin(String formName) {
        return formName == null || formName.isBlank()
                || "player".equalsIgnoreCase(formName)
                || "human".equalsIgnoreCase(formName);
    }

    /**
     * Apply a vanilla Steve/Alex skin when the type asked for one and no Mojang username skin landed.
     */
    public static boolean shouldApplyVanillaDefault(TypeSpec spec, boolean appliedUsernameSkin) {
        return spec != null
                && !appliedUsernameSkin
                && spec.vanillaPlayerPick() != VanillaPlayerPick.NONE
                && wantsPlayerSkin(spec.formName());
    }

    public static VanillaPlayerSkin pickVanillaPlayerSkin(VanillaPlayerPick pick) {
        return pickVanillaPlayerSkin(pick, ThreadLocalRandom.current());
    }

    /**
     * Steve = wide arms + {@code textures/entity/player/wide/steve.png}.
     * Alex = slim arms + {@code textures/entity/player/slim/alex.png}.
     * {@link VanillaPlayerPick#RANDOM} is 50/50 per call (not cached for the process).
     */
    public static VanillaPlayerSkin pickVanillaPlayerSkin(VanillaPlayerPick pick, Random random) {
        if (pick == null || pick == VanillaPlayerPick.NONE) {
            return null;
        }
        return switch (pick) {
            case STEVE -> VanillaPlayerSkin.STEVE;
            case ALEX -> VanillaPlayerSkin.ALEX;
            case RANDOM -> random.nextBoolean() ? VanillaPlayerSkin.STEVE : VanillaPlayerSkin.ALEX;
            case NONE -> null;
        };
    }

    private static CompanionForm tryForm(String key) {
        try {
            return CompanionForm.valueOf(key.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public enum VanillaPlayerPick {
        NONE,
        RANDOM,
        STEVE,
        ALEX
    }

    /**
     * Vanilla player texture applied to player-form CCI summons that are not Kon/Bits
     * and did not resolve a Mojang username skin.
     */
    public record VanillaPlayerSkin(String texturePath, boolean slim, String label) {
        public static final VanillaPlayerSkin STEVE = new VanillaPlayerSkin(
                "minecraft:textures/entity/player/wide/steve.png", false, "Steve");
        public static final VanillaPlayerSkin ALEX = new VanillaPlayerSkin(
                "minecraft:textures/entity/player/slim/alex.png", true, "Alex");
    }

    public record TypeSpec(
            String definitionPath,
            String formName,
            float bodyScale,
            boolean bitSized,
            VanillaPlayerPick vanillaPlayerPick) {
        public static TypeSpec kon() {
            return new TypeSpec("kon", "player", 1.0f, false, VanillaPlayerPick.NONE);
        }

        public static TypeSpec bits() {
            return new TypeSpec("kon", "player", CompanionChildLimits.DEFAULT_BODY_SCALE, true, VanillaPlayerPick.NONE);
        }

        public static TypeSpec wiggly() {
            return new TypeSpec("kon", "wolf", 1.0f, false, VanillaPlayerPick.NONE);
        }

        public static TypeSpec playerDefault() {
            return new TypeSpec("kon", "player", 1.0f, false, VanillaPlayerPick.RANDOM);
        }

        public static TypeSpec steve() {
            return new TypeSpec("kon", "player", 1.0f, false, VanillaPlayerPick.STEVE);
        }

        public static TypeSpec alex() {
            return new TypeSpec("kon", "player", 1.0f, false, VanillaPlayerPick.ALEX);
        }

        public String definitionId(String namespace) {
            if (definitionPath.contains(":")) {
                return definitionPath;
            }
            return namespace + ":" + definitionPath;
        }
    }
}
