package com.azscompanions.cci;

import com.azscompanions.entity.CompanionAttitude;
import com.azscompanions.entity.CompanionForm;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Loader-agnostic parser for CCI IMC {@code message} payloads.
 * Supports {@code key=value} pairs separated by {@code ;} {@code ,} or whitespace,
 * plus bare tokens (form name, attitude, or team name).
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code form=zombie;attitude=hostile;team=red}</li>
 *   <li>{@code skin=Notch;team=blue}</li>
 *   <li>{@code mainhand=minecraft:diamond_sword}</li>
 *   <li>{@code helmet=minecraft:iron_helmet;offhand=clear}</li>
 *   <li>{@code form=wolf;skin=Notch;name=Fluffy} ({@code companion_modify})</li>
 *   <li>{@code showArmor=false} ({@code companion_modify} — hide armor render)</li>
 *   <li>{@code seconds=10} ({@code companion_turn_evil})</li>
 *   <li>{@code form=chicken;name=Bit;count=3;bits=500;team=red} ({@code companion_spawn_child})</li>
 *   <li>{@code name=Alice;form=zombie;subs=1;team=blue;mainhand=iron_sword} ({@code companion_spawn_leader})</li>
 *   <li>{@code red} (bare team for {@code companion_set_team})</li>
 * </ul>
 */
public final class CciCompanionParams {
    private final Map<String, String> values;

    private CciCompanionParams(Map<String, String> values) {
        this.values = values;
    }

    public static CciCompanionParams parse(String message) {
        Map<String, String> map = new LinkedHashMap<>();
        if (message == null || message.isBlank()) {
            return new CciCompanionParams(map);
        }
        String trimmed = message.trim();
        // Prefer structured key=value pairs.
        String[] parts = trimmed.split("[,;\\n]+");
        boolean anyKv = false;
        for (String part : parts) {
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            int eq = p.indexOf('=');
            if (eq > 0) {
                anyKv = true;
                String key = p.substring(0, eq).trim().toLowerCase(Locale.ROOT);
                String val = p.substring(eq + 1).trim();
                map.put(key, val);
            }
        }
        if (anyKv) {
            return new CciCompanionParams(map);
        }
        // Bare token: treat as form if known, else attitude, else team/name fallbacks.
        String bare = trimmed;
        CompanionForm form = tryForm(bare);
        if (form != null) {
            map.put("form", form.serializedName());
            return new CciCompanionParams(map);
        }
        CompanionAttitude attitude = CompanionAttitude.byName(bare);
        if (attitude.isHostile() || bare.equalsIgnoreCase("passive")) {
            map.put("attitude", attitude.serializedName());
            return new CciCompanionParams(map);
        }
        map.put("team", bare);
        map.put("name", bare);
        map.put("raw", bare);
        return new CciCompanionParams(map);
    }

    public Map<String, String> values() {
        return Collections.unmodifiableMap(values);
    }

    public String get(String key) {
        return values.get(key == null ? null : key.toLowerCase(Locale.ROOT));
    }

    public String getOr(String key, String fallback) {
        String v = get(key);
        return v == null || v.isBlank() ? fallback : v;
    }

    public boolean has(String key) {
        String v = get(key);
        return v != null && !v.isBlank();
    }

    public CompanionForm formOr(CompanionForm fallback) {
        String v = first("form", "mob", "species");
        if (v == null) {
            return fallback;
        }
        // "player" / "skin" / "human" → player form
        if (v.equalsIgnoreCase("skin") || v.equalsIgnoreCase("human") || v.equalsIgnoreCase("person")) {
            return CompanionForm.PLAYER;
        }
        return CompanionForm.byName(v);
    }

    public CompanionAttitude attitudeOr(CompanionAttitude fallback) {
        String v = first("attitude", "stance", "mode");
        return v == null ? fallback : CompanionAttitude.byName(v);
    }

    public String teamOr(String fallback) {
        String v = first("team", "teamid", "squad");
        return v == null || v.isBlank() ? fallback : sanitizeTeam(v);
    }

    public String skinUsername() {
        return first("skin", "player", "username", "ign");
    }

    public String displayName() {
        return first("name", "displayname");
    }

    /**
     * Armor render toggle ({@code showArmor=}/{@code show_armor=}/{@code armor_visible=}).
     * Returns null when the key is absent.
     */
    public Boolean showArmorOrNull() {
        String v = first("showarmor", "show_armor", "armor_visible", "armorvisible");
        if (v == null || v.isBlank()) {
            return null;
        }
        return parseBoolean(v);
    }

    private static Boolean parseBoolean(String raw) {
        String v = raw.trim().toLowerCase(Locale.ROOT);
        if (v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("on") || v.equals("show")) {
            return Boolean.TRUE;
        }
        if (v.equals("false") || v.equals("0") || v.equals("no") || v.equals("off") || v.equals("hide")) {
            return Boolean.FALSE;
        }
        return null;
    }

    /**
     * Duration in seconds for timed actions (e.g. playful evil). Clamped to 5–15.
     * Accepts {@code seconds=} / {@code duration=} or a bare integer message.
     */
    public int durationSecondsOr(int fallback) {
        String v = first("seconds", "duration", "secs", "time");
        if (v == null) {
            v = get("raw");
        }
        if (v == null || v.isBlank()) {
            return clampDurationSeconds(fallback);
        }
        try {
            return clampDurationSeconds(Integer.parseInt(v.trim()));
        } catch (NumberFormatException ex) {
            return clampDurationSeconds(fallback);
        }
    }

    public static int clampDurationSeconds(int seconds) {
        return Math.max(5, Math.min(15, seconds));
    }

    /** Spawn count for child actions ({@code count=}/{@code amount=}/{@code n=}). Clamped 1–8. */
    public int spawnCountOr(int fallback) {
        String v = first("count", "amount", "n", "num");
        if (v == null || v.isBlank()) {
            return com.azscompanions.entity.CompanionChildLimits.clampSpawnCount(fallback);
        }
        try {
            return com.azscompanions.entity.CompanionChildLimits.clampSpawnCount(Integer.parseInt(v.trim()));
        } catch (NumberFormatException ex) {
            return com.azscompanions.entity.CompanionChildLimits.clampSpawnCount(fallback);
        }
    }

    /** Body scale ({@code size=}/{@code scale=}). Clamped 0.5–3.0. */
    public float bodyScaleOr(float fallback) {
        String v = first("size", "scale", "body_scale", "bodyscale");
        if (v == null || v.isBlank()) {
            return clampBodyScale(fallback);
        }
        try {
            return clampBodyScale(Float.parseFloat(v.trim()));
        } catch (NumberFormatException ex) {
            return clampBodyScale(fallback);
        }
    }

    public static float clampBodyScale(float scale) {
        return Math.max(0.5f, Math.min(3.0f, scale));
    }

    /** Bits amount ({@code bits=}/{@code cheer=}/{@code amount=} when used with bits context). */
    public int bitsOr(int fallback) {
        String v = first("bits", "cheer", "cheers", "bit");
        return parseNonNegInt(v, fallback);
    }

    /** Subs amount ({@code subs=}/{@code sub=}/{@code gift=}). */
    public int subsOr(int fallback) {
        String v = first("subs", "sub", "gift", "giftsubs");
        return parseNonNegInt(v, fallback);
    }

    private static int parseNonNegInt(String v, int fallback) {
        if (v == null || v.isBlank()) {
            return Math.max(0, fallback);
        }
        try {
            return Math.max(0, Integer.parseInt(v.trim()));
        } catch (NumberFormatException ex) {
            return Math.max(0, fallback);
        }
    }

    public String equipment(String slotKey) {
        return get(slotKey);
    }

    public String first(String... keys) {
        for (String key : keys) {
            String v = get(key);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    public static String sanitizeTeam(String team) {
        if (team == null) {
            return "";
        }
        String t = team.trim();
        if (t.length() > 32) {
            t = t.substring(0, 32);
        }
        return t;
    }

    public static boolean isClearToken(String value) {
        if (value == null) {
            return false;
        }
        String v = value.trim();
        return v.isEmpty()
                || v.equalsIgnoreCase("clear")
                || v.equalsIgnoreCase("none")
                || v.equalsIgnoreCase("empty")
                || v.equalsIgnoreCase("air")
                || v.equalsIgnoreCase("minecraft:air");
    }

    private static CompanionForm tryForm(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String key = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        try {
            return CompanionForm.valueOf(key);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
