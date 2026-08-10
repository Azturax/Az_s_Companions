package com.azscompanions.ai;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loader-agnostic helpers for multiplayer-safe companion ask targeting.
 * <p>
 * Deliberately does <strong>not</strong> register global {@code /&lt;name&gt;} Brigadier
 * roots — those clash across players and mods. Prefer {@code /ask}, {@code /az ask},
 * and optional chat {@code Name ask …} resolved against the <em>commanding player's</em>
 * owned companions only.
 */
public final class CompanionAskResolve {
    private static final Pattern NAMED_ASK_CHAT = Pattern.compile(
            "^(\\S+)\\s+[Aa][Ss][Kk]\\s+(.+)$", Pattern.DOTALL);

    private CompanionAskResolve() {
    }

    /**
     * Command-safe token: lowercase ASCII letters, digits, underscore.
     * Whitespace becomes {@code _}; other punctuation is stripped.
     */
    public static String sanitizeToken(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                c = Character.toLowerCase(c);
            }
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_') {
                sb.append(c);
            } else if (Character.isWhitespace(c)) {
                if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != '_') {
                    sb.append('_');
                }
            }
        }
        while (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '_') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    /** True when sanitized display name equals sanitized query (non-empty). */
    public static boolean namesMatch(String displayName, String query) {
        String want = sanitizeToken(query);
        if (want.isEmpty()) {
            return false;
        }
        return want.equals(sanitizeToken(displayName));
    }

    /**
     * Chat form {@code Kon ask hello} (no leading slash). Empty optional if not a named ask.
     */
    public static Optional<NamedAsk> parseNamedAskChat(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String text = raw.trim();
        if (text.isEmpty() || text.startsWith("/")) {
            return Optional.empty();
        }
        Matcher m = NAMED_ASK_CHAT.matcher(text);
        if (!m.matches()) {
            return Optional.empty();
        }
        String name = m.group(1).trim();
        String message = m.group(2).trim();
        if (name.isEmpty() || message.isEmpty() || sanitizeToken(name).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new NamedAsk(name, message));
    }

    /**
     * Leading name address: {@code Bit, come here} / {@code Kon follow me}.
     * Empty when slash command, named-ask form, or no trailing message after the name.
     */
    public static Optional<NamedAsk> parseLeadingNameAddress(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String text = raw.trim();
        if (text.isEmpty() || text.startsWith("/")) {
            return Optional.empty();
        }
        if (parseNamedAskChat(text).isPresent()) {
            return Optional.empty();
        }
        int end = 0;
        while (end < text.length()) {
            char c = text.charAt(end);
            if (Character.isWhitespace(c) || c == ',' || c == ':' || c == '!' || c == ';' || c == '.') {
                break;
            }
            end++;
        }
        if (end == 0) {
            return Optional.empty();
        }
        String name = text.substring(0, end).trim();
        if (sanitizeToken(name).isEmpty()) {
            return Optional.empty();
        }
        int restStart = end;
        while (restStart < text.length()) {
            char c = text.charAt(restStart);
            if (Character.isWhitespace(c) || c == ',' || c == ':' || c == '!' || c == ';' || c == '.') {
                restStart++;
                continue;
            }
            break;
        }
        String message = text.substring(restStart).trim();
        if (message.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new NamedAsk(name, message));
    }

    /**
     * Resolve {@code /ask} / {@code /az ask} greedy text:
     * if the first token matches an owned companion name (via {@code nameMatcher}),
     * the remainder is the message; otherwise the whole string goes to nearest.
     */
    public static AskTarget resolveGreedyAsk(String greedy, NameMatcher nameMatcher) {
        if (greedy == null) {
            return AskTarget.invalid();
        }
        String trimmed = greedy.trim();
        if (trimmed.isEmpty()) {
            return AskTarget.invalid();
        }
        int space = indexOfWhitespace(trimmed);
        if (space > 0) {
            String maybeName = trimmed.substring(0, space).trim();
            String rest = trimmed.substring(space + 1).trim();
            if (!rest.isEmpty() && nameMatcher.matchesOwnedName(maybeName)) {
                return AskTarget.named(maybeName, rest);
            }
        }
        return AskTarget.nearest(trimmed);
    }

    private static int indexOfWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    @FunctionalInterface
    public interface NameMatcher {
        boolean matchesOwnedName(String nameQuery);
    }

    public record NamedAsk(String companionName, String message) {
    }

    public record AskTarget(Kind kind, String companionName, String message) {
        public enum Kind { INVALID, NEAREST, NAMED }

        public static AskTarget invalid() {
            return new AskTarget(Kind.INVALID, "", "");
        }

        public static AskTarget nearest(String message) {
            return new AskTarget(Kind.NEAREST, "", message);
        }

        public static AskTarget named(String companionName, String message) {
            return new AskTarget(Kind.NAMED, companionName, message);
        }

        public boolean isValid() {
            return kind != Kind.INVALID && message != null && !message.isBlank();
        }
    }

    /** Locale used only for docs/tests; sanitizer is ASCII-oriented. */
    public static String lowerAscii(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
