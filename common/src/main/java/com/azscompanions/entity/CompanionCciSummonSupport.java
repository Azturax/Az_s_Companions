package com.azscompanions.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
     * Wiggly maps to wolf form (not the UUID perk dog). Bits are scaled down but are not charm children.
     */
    public static TypeSpec resolveType(String raw) {
        if (raw == null || raw.isBlank()) {
            return TypeSpec.kon();
        }
        String token = raw.trim();
        int colon = token.indexOf(':');
        String path = colon >= 0 ? token.substring(colon + 1) : token;
        String key = path.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (key) {
            case "kon", "dox" -> TypeSpec.kon();
            case "bit", "bits" -> TypeSpec.bits();
            case "wiggly", "wiggles", "mister_wiggly" -> TypeSpec.wiggly();
            case "wolfy" -> new TypeSpec("kon", "wolf", 1.0f, false);
            default -> {
                CompanionForm form = tryForm(key);
                if (form != null) {
                    yield new TypeSpec("kon", form.serializedName(), 1.0f, false);
                }
                yield new TypeSpec(path, "player", 1.0f, false);
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

    private static CompanionForm tryForm(String key) {
        try {
            return CompanionForm.valueOf(key.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public record TypeSpec(String definitionPath, String formName, float bodyScale, boolean bitSized) {
        public static TypeSpec kon() {
            return new TypeSpec("kon", "player", 1.0f, false);
        }

        public static TypeSpec bits() {
            return new TypeSpec("kon", "player", CompanionChildLimits.DEFAULT_BODY_SCALE, true);
        }

        public static TypeSpec wiggly() {
            return new TypeSpec("kon", "wolf", 1.0f, false);
        }

        public String definitionId(String namespace) {
            if (definitionPath.contains(":")) {
                return definitionPath;
            }
            return namespace + ":" + definitionPath;
        }
    }
}
