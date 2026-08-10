package com.azscompanions.cci;

import com.azscompanions.entity.CompanionAttitude;
import com.azscompanions.entity.CompanionFollowDistances;
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
 *   <li>{@code whoAmI=…;whatAmIDoing=…;howWillIBe=…} ({@code companion_persona} / {@code companion_modify})</li>
 *   <li>{@code form=wolf;skin=Notch;name=Fluffy} ({@code companion_modify})</li>
 *   <li>{@code showArmor=false} ({@code companion_modify} — hide armor render)</li>
 *   <li>{@code followRadius=64;personalSpace=3;wanderRadius=12} ({@code companion_modify})</li>
 *   <li>{@code whoAmI=brave wolf;whatAmIDoing=guard;howWillIBe=loyal} (persona on summon/modify)</li>
 *   <li>{@code chunkLoading=false} ({@code companion_modify} — per-companion ticket opt-out)</li>
 *   <li>{@code mode=rush;seconds=8} ({@code companion_play})</li>
 *   <li>{@code seconds=10} ({@code companion_turn_evil})</li>
 *   <li>{@code amount=500;user=Alice;form=chicken;team=red} ({@code companion_interaction} — 500÷price children)</li>
 *   <li>{@code form=chicken;name=Bit;count=3;maxChildren=8;team=red} ({@code companion_spawn_child})</li>
 *   <li>{@code maxChildren=8} / {@code childCap=5} ({@code companion_modify} / summon / spawn_leader)</li>
 *   <li>{@code name=Alice;form=zombie;team=blue;mainhand=iron_sword} ({@code companion_spawn_leader})</li>
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
     * Target username for interaction spawns ({@code user=}/{@code username=}/{@code name=}).
     * Prefer {@code user=} when {@code name=} is reserved for the child display name.
     */
    public String interactionUser() {
        return first("user", "username", "name", "displayname");
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

    /** True/false key helper for CCI flags like {@code ai=true}. */
    public boolean flag(String key, boolean fallback) {
        String v = get(key);
        if (v == null || v.isBlank()) {
            return fallback;
        }
        Boolean parsed = parseBoolean(v);
        return parsed == null ? fallback : parsed;
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

    /** Explicit spawn count ({@code count=}/{@code n=}/{@code num=}). Does not read {@code amount=} (that is support value). */
    public int spawnCountOr(int fallback) {
        String v = first("count", "n", "num");
        if (v == null || v.isBlank()) {
            return com.azscompanions.entity.CompanionChildLimits.clampSpawnCount(fallback);
        }
        try {
            return com.azscompanions.entity.CompanionChildLimits.clampSpawnCount(Integer.parseInt(v.trim()));
        } catch (NumberFormatException ex) {
            return com.azscompanions.entity.CompanionChildLimits.clampSpawnCount(fallback);
        }
    }

    /**
     * Per-leader child cap ({@code maxChildren=}/{@code max_children=}/{@code childCap=}).
     * Returns null when absent. Clamped 1–64 (not an interaction-amount ceiling).
     */
    public Integer maxChildrenOrNull() {
        String v = first("maxchildren", "max_children", "childcap", "child_cap");
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return com.azscompanions.entity.CompanionChildLimits.clampMaxChildren(Integer.parseInt(v.trim()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Requested child spawn count for interaction / spawn_child.
     * Explicit {@code count=}/{@code n=}/{@code num=} wins; else {@code amount=}÷price (see {@link #supportAmountOr(int)}).
     * No artificial upper clamp — remaining leader slots apply at spawn time.
     */
    public int childSpawnRequestOr(int amountFallback, int pricePerCompanion) {
        String v = first("count", "n", "num");
        if (v != null && !v.isBlank()) {
            try {
                return com.azscompanions.entity.CompanionChildLimits.clampSpawnCount(Integer.parseInt(v.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        int amount = supportAmountOr(amountFallback);
        if (amount > 0) {
            return com.azscompanions.entity.CompanionChildLimits.spawnCountFromAmount(amount, pricePerCompanion);
        }
        return 1;
    }

    /** {@link #childSpawnRequestOr(int, int)} with {@link com.azscompanions.teamfight.TeamFightDefaults#SUPPORT_AMOUNT_PER_COMPANION}. */
    public int childSpawnRequestOr(int amountFallback) {
        return childSpawnRequestOr(amountFallback,
                com.azscompanions.teamfight.TeamFightDefaults.SUPPORT_AMOUNT_PER_COMPANION);
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

    /**
     * Follow / teleport leash ({@code followRadius=}/{@code teleportDistance=}).
     * Returns null when absent. Clamped 1–128.
     */
    public Float followRadiusOrNull() {
        String v = first("followradius", "follow_radius", "teleportdistance", "teleport_distance", "teleport");
        return parseFloatOrNull(v, CompanionFollowDistances.FOLLOW_RADIUS_MIN, CompanionFollowDistances.FOLLOW_RADIUS_MAX);
    }

    /**
     * Personal space / stop distance ({@code personalSpace=}/{@code stopDistance=}).
     * Returns null when absent. Clamped 1–12.
     */
    public Float personalSpaceOrNull() {
        String v = first("personalspace", "personal_space", "stopdistance", "stop_distance", "minspace");
        return parseFloatOrNull(v, CompanionFollowDistances.PERSONAL_SPACE_MIN, CompanionFollowDistances.PERSONAL_SPACE_MAX);
    }

    /**
     * Wander free-roam radius ({@code wanderRadius=}). Returns null when absent. Clamped 3–48.
     */
    public Float wanderRadiusOrNull() {
        String v = first("wanderradius", "wander_radius", "wander");
        return parseFloatOrNull(v, CompanionFollowDistances.WANDER_RADIUS_MIN, CompanionFollowDistances.WANDER_RADIUS_MAX);
    }

    /**
     * Per-companion chunk ticket override ({@code chunkLoading=}/{@code forceChunk=}).
     * Returns null when absent. Requires server {@code companionChunkLoading} for tickets to apply.
     */
    public Boolean chunkLoadingOrNull() {
        String v = first("chunkloading", "chunk_loading", "forcechunk", "force_chunk", "forceload");
        if (v == null || v.isBlank()) {
            return null;
        }
        return parseBoolean(v);
    }

    /**
     * Session AI listen mode ({@code chatListenMode=}/{@code chat_listen=}).
     * Returns null when absent.
     */
    public String chatListenModeRawOrNull() {
        return first("chatlistenmode", "chat_listen_mode", "chatlisten", "chat_listen", "listen");
    }

    /**
     * Session {@code enableAiActions=} hint. Returns null when absent.
     */
    public Boolean enableAiActionsOrNull() {
        String v = first("enableaiactions", "enable_ai_actions", "aiactions", "ai_actions");
        if (v == null || v.isBlank()) {
            return null;
        }
        return parseBoolean(v);
    }

    /**
     * Play / claim duration in seconds (wider than evil). Clamped 3–60.
     * Accepts {@code seconds=}/{@code duration=} or bare integer.
     */
    public int playSecondsOr(int fallback) {
        String v = first("seconds", "duration", "secs", "time");
        if (v == null) {
            v = get("raw");
        }
        if (v == null || v.isBlank()) {
            return clampPlaySeconds(fallback);
        }
        try {
            return clampPlaySeconds(Integer.parseInt(v.trim()));
        } catch (NumberFormatException ex) {
            return clampPlaySeconds(fallback);
        }
    }

    public static int clampPlaySeconds(int seconds) {
        return Math.max(3, Math.min(60, seconds));
    }

    /** Play mode token ({@code mode=}/{@code play=}/{@code action=}). */
    public String playModeOr(String fallback) {
        String v = first("mode", "play", "action", "game");
        if (v == null || v.isBlank()) {
            return fallback;
        }
        return v.trim();
    }

    /** Hide-and-seek role ({@code role=hider|seeker}). */
    public String playRoleOr(String fallback) {
        String v = first("role", "as");
        return v == null || v.isBlank() ? fallback : v.trim();
    }

    /**
     * Chunk X for FTB claim ({@code chunkX=} or block {@code x=} >> 4).
     * Uses {@code fallback} when absent.
     */
    public int chunkXOr(int fallback) {
        String cx = first("chunkx", "chunk_x");
        if (cx != null && !cx.isBlank()) {
            try {
                return Integer.parseInt(cx.trim());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        String x = first("x", "blockx", "block_x");
        if (x != null && !x.isBlank()) {
            try {
                return Integer.parseInt(x.trim()) >> 4;
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return fallback;
    }

    /** Chunk Z for FTB claim ({@code chunkZ=} or block {@code z=} >> 4). */
    public int chunkZOr(int fallback) {
        String cz = first("chunkz", "chunk_z");
        if (cz != null && !cz.isBlank()) {
            try {
                return Integer.parseInt(cz.trim());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        String z = first("z", "blockz", "block_z");
        if (z != null && !z.isBlank()) {
            try {
                return Integer.parseInt(z.trim()) >> 4;
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return fallback;
    }

    /** Persona clear flag ({@code clear=true} / {@code op=clear}). */
    public boolean wantsPersonaClear() {
        if (flag("clear", false) || flag("reset", false)) {
            return true;
        }
        String op = first("op", "operation", "action");
        return op != null && (op.equalsIgnoreCase("clear") || op.equalsIgnoreCase("reset"));
    }

    /** Persona get/status flag ({@code op=get} / {@code get=true}). */
    public boolean wantsPersonaGet() {
        if (flag("get", false) || flag("status", false) || flag("show", false)) {
            return true;
        }
        String op = first("op", "operation", "action");
        return op != null && (op.equalsIgnoreCase("get") || op.equalsIgnoreCase("status")
                || op.equalsIgnoreCase("show") || op.equalsIgnoreCase("print"));
    }

    private static Float parseFloatOrNull(String v, float min, float max) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            float parsed = Float.parseFloat(v.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Interaction / support amount ({@code amount=}/{@code support=}/{@code value=}).
     * Optional legacy alias {@code bits=} for older IMC; prefer {@code amount=}.
     * Optional {@code unit=} is informational only (not used for math).
     */
    public int supportAmountOr(int fallback) {
        String v = first("amount", "support", "value", "bits", "bit");
        return parseNonNegInt(v, fallback);
    }

    /** @deprecated Prefer {@link #supportAmountOr(int)}; alias for gear-tier callers. */
    @Deprecated
    public int bitsOr(int fallback) {
        return supportAmountOr(fallback);
    }

    /** Optional informational unit label ({@code unit=bits}, {@code unit=points}, …). */
    public String unitOr(String fallback) {
        String v = first("unit", "currency");
        return v == null || v.isBlank() ? fallback : v.trim();
    }

    /** Optional legacy {@code subs=} / {@code sub=} (no longer gates leader spawn). */
    public int subsOr(int fallback) {
        String v = first("subs", "sub");
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
