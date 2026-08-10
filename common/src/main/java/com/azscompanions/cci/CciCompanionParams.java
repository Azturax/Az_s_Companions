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
