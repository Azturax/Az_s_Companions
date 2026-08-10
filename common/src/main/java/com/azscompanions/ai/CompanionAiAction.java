package com.azscompanions.ai;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * One structured companion world action returned by the LLM (tool call or JSON block).
 */
public final class CompanionAiAction {
    private final String name;
    private final Map<String, String> args;

    public CompanionAiAction(String name, Map<String, String> args) {
        this.name = name == null ? "" : name.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        this.args = args == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(args));
    }

    public String name() {
        return name;
    }

    public Map<String, String> args() {
        return args;
    }

    public String arg(String key) {
        return args.get(key == null ? null : key.toLowerCase(Locale.ROOT));
    }

    public String argOr(String key, String fallback) {
        String v = arg(key);
        return v == null || v.isBlank() ? fallback : v;
    }

    public int argInt(String key, int fallback) {
        String v = arg(key);
        if (v == null || v.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public double argDouble(String key, double fallback) {
        String v = arg(key);
        if (v == null || v.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean argBool(String key, boolean fallback) {
        String v = arg(key);
        if (v == null || v.isBlank()) {
            return fallback;
        }
        String s = v.trim().toLowerCase(Locale.ROOT);
        if (s.equals("true") || s.equals("1") || s.equals("yes") || s.equals("on")) {
            return true;
        }
        if (s.equals("false") || s.equals("0") || s.equals("no") || s.equals("off")) {
            return false;
        }
        return fallback;
    }

    @Override
    public String toString() {
        return name + args;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CompanionAiAction that)) {
            return false;
        }
        return Objects.equals(name, that.name) && Objects.equals(args, that.args);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, args);
    }
}
