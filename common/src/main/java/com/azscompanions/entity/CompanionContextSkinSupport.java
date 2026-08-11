package com.azscompanions.entity;

import java.util.Locale;

/**
 * Player-form activity / context outfit skins (sleeping, bathing, adventuring).
 *
 * <p>Resolution priority when the companion is in <strong>player form</strong>:
 * <ol>
 *   <li>Active context custom skin (if set and non-blank)</li>
 *   <li>Normal custom applied skin ({@code SkinPath})</li>
 *   <li>Base / default skin (caller applies Kon / owner fallback)</li>
 * </ol>
 *
 * <p>Mob / non-player forms never use context skins — keep form rendering.
 */
public final class CompanionContextSkinSupport {
    public static final int MAX_PATH_LENGTH = 512;

    public enum Context {
        SLEEPING("sleeping"),
        BATHING("bathing"),
        ADVENTURING("adventuring");

        private final String id;

        Context(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static Context byId(String id) {
            if (id == null || id.isBlank()) {
                return null;
            }
            String key = id.trim().toLowerCase(Locale.ROOT);
            for (Context c : values()) {
                if (c.id.equals(key)) {
                    return c;
                }
            }
            return null;
        }
    }

    private CompanionContextSkinSupport() {
    }

    /**
     * Which activity context is active. Sleeping wins over bathing; bathing over adventuring.
     * Returns {@code null} when not player form or no activity context applies.
     */
    public static Context activeContext(
            boolean playerForm,
            boolean sleeping,
            boolean bathing,
            boolean adventuring
    ) {
        if (!playerForm) {
            return null;
        }
        if (sleeping) {
            return Context.SLEEPING;
        }
        if (bathing) {
            return Context.BATHING;
        }
        if (adventuring) {
            return Context.ADVENTURING;
        }
        return null;
    }

    /** True when the companion should count as bathing (in water, not asleep). */
    public static boolean isBathing(boolean sleeping, boolean inWaterOrBubble) {
        return !sleeping && inWaterOrBubble;
    }

    /**
     * Picks the skin path to render for player form.
     * Returns blank when the caller should use base/default (Kon / owner).
     * Never returns a blank string when {@code customSkin} is set — custom wins over base.
     */
    public static String resolveRenderSkinPath(
            boolean playerForm,
            Context active,
            String sleepingSkin,
            String bathingSkin,
            String adventuringSkin,
            String customSkin
    ) {
        if (playerForm && active != null) {
            String contextPath = switch (active) {
                case SLEEPING -> sleepingSkin;
                case BATHING -> bathingSkin;
                case ADVENTURING -> adventuringSkin;
            };
            if (isSet(contextPath)) {
                return contextPath.trim();
            }
        }
        if (isSet(customSkin)) {
            return customSkin.trim();
        }
        return "";
    }

    public static boolean isSet(String path) {
        return path != null && !path.isBlank();
    }

    /**
     * Sanitize a context-skin path from the client. Allows empty (clear), {@code local:},
     * {@code url:}/{@code http(s):}, {@code player:}, and resource locations.
     */
    public static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        String path = raw.trim();
        if (path.length() > MAX_PATH_LENGTH) {
            path = path.substring(0, MAX_PATH_LENGTH);
        }
        if (path.isEmpty()) {
            return "";
        }
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("file:")) {
            return "";
        }
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return "url:" + path;
        }
        if (lower.startsWith("url:")) {
            String rest = path.substring(4).trim();
            if (!(rest.regionMatches(true, 0, "http://", 0, 7)
                    || rest.regionMatches(true, 0, "https://", 0, 8))) {
                return "";
            }
            return "url:" + rest;
        }
        if (lower.startsWith("local:")) {
            String rest = path.substring(6).trim().replace('\\', '/');
            if (rest.isEmpty() || rest.contains("..")) {
                return "";
            }
            return "local:" + rest;
        }
        if (lower.startsWith("player:")) {
            return path;
        }
        // Resource location / Mojang-style paths already used by the mod.
        return path;
    }

    public static boolean isUrlSkin(String path) {
        if (path == null) {
            return false;
        }
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.startsWith("url:") || lower.startsWith("http://") || lower.startsWith("https://");
    }

    public static boolean isLocalSkin(String path) {
        return path != null && path.toLowerCase(Locale.ROOT).startsWith("local:");
    }

    /** Extract download URL from {@code url:} / raw http(s) path. */
    public static String extractUrl(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String trimmed = path.trim();
        if (trimmed.regionMatches(true, 0, "url:", 0, 4)) {
            trimmed = trimmed.substring(4).trim();
        }
        if (trimmed.regionMatches(true, 0, "http://", 0, 7)
                || trimmed.regionMatches(true, 0, "https://", 0, 8)) {
            return trimmed;
        }
        return null;
    }

    /** Path after {@code local:} prefix (forward slashes). */
    public static String extractLocalRelative(String path) {
        if (!isLocalSkin(path)) {
            return null;
        }
        String rest = path.substring(6).trim().replace('\\', '/');
        return rest.isEmpty() ? null : rest;
    }
}
