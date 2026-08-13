package com.azscompanions.ai;

import com.google.gson.JsonObject;

/**
 * Host-defined extra ambient / reactive chat event (MVP).
 * Loaded from {@code customChatEvents} in {@code azscompanions-ai.json} / {@code .toml}.
 */
public final class CompanionCustomChatEvent {
    public static final int DEFAULT_COOLDOWN_SECONDS = 60;
    public static final int DEFAULT_PRIORITY = 50;
    public static final int MAX_ID = 64;
    public static final int MAX_PROMPT = 512;
    public static final int MAX_FALLBACK = 256;
    public static final int MAX_ITEM = 128;

    private String id = "";
    private boolean enabled = true;
    /** Builtin kind name ({@code item_find}, {@code explosion}, …) or {@code idle}. */
    private String trigger = "item_find";
    /** Optional registry id filter for item_* triggers; empty = any item. */
    private String itemId = "";
    private String prompt = "";
    private String fallback = "";
    private int cooldownSeconds = DEFAULT_COOLDOWN_SECONDS;
    private int priority = DEFAULT_PRIORITY;

    public String id() {
        return id;
    }

    public CompanionCustomChatEvent setId(String id) {
        this.id = sanitizeId(id);
        return this;
    }

    public boolean enabled() {
        return enabled;
    }

    public CompanionCustomChatEvent setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String trigger() {
        return trigger;
    }

    public CompanionCustomChatEvent setTrigger(String trigger) {
        this.trigger = normalizeTrigger(trigger);
        return this;
    }

    public String itemId() {
        return itemId;
    }

    public CompanionCustomChatEvent setItemId(String itemId) {
        this.itemId = itemId == null ? "" : CompanionNotableItemSupport.normalizeId(itemId);
        if (this.itemId.length() > MAX_ITEM) {
            this.itemId = this.itemId.substring(0, MAX_ITEM);
        }
        return this;
    }

    public String prompt() {
        return prompt;
    }

    public CompanionCustomChatEvent setPrompt(String prompt) {
        this.prompt = clamp(prompt, MAX_PROMPT);
        return this;
    }

    public String fallback() {
        return fallback;
    }

    public CompanionCustomChatEvent setFallback(String fallback) {
        this.fallback = clamp(fallback, MAX_FALLBACK);
        return this;
    }

    public int cooldownSeconds() {
        return cooldownSeconds;
    }

    public CompanionCustomChatEvent setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = Math.max(5, Math.min(3600, cooldownSeconds));
        return this;
    }

    public int priority() {
        return priority;
    }

    public CompanionCustomChatEvent setPriority(int priority) {
        this.priority = Math.max(1, Math.min(200, priority));
        return this;
    }

    public boolean isValid() {
        return !id.isBlank() && !trigger.isBlank();
    }

    /** True when this event listens for a builtin kind (not plain idle timer). */
    public boolean isReactiveTrigger() {
        return !"idle".equals(trigger);
    }

    public boolean matchesTrigger(CompanionRecentActionKind kind) {
        if (kind == null || !enabled || !isReactiveTrigger()) {
            return false;
        }
        return triggerEqualsKind(trigger, kind);
    }

    public boolean matchesItem(String gainedItemId) {
        if (itemId == null || itemId.isBlank()) {
            return true;
        }
        if (gainedItemId == null || gainedItemId.isBlank()) {
            return false;
        }
        return itemId.equals(CompanionNotableItemSupport.normalizeId(gainedItemId));
    }

    public CompanionCustomChatEvent copy() {
        return new CompanionCustomChatEvent()
                .setId(id)
                .setEnabled(enabled)
                .setTrigger(trigger)
                .setItemId(itemId)
                .setPrompt(prompt)
                .setFallback(fallback)
                .setCooldownSeconds(cooldownSeconds)
                .setPriority(priority);
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("enabled", enabled);
        o.addProperty("trigger", trigger);
        o.addProperty("itemId", itemId);
        o.addProperty("prompt", prompt);
        o.addProperty("fallback", fallback);
        o.addProperty("cooldownSeconds", cooldownSeconds);
        o.addProperty("priority", priority);
        return o;
    }

    public static CompanionCustomChatEvent fromJson(JsonObject o) {
        CompanionCustomChatEvent e = new CompanionCustomChatEvent();
        if (o == null) {
            return e;
        }
        if (o.has("id")) {
            e.setId(o.get("id").getAsString());
        }
        if (o.has("enabled")) {
            e.setEnabled(o.get("enabled").getAsBoolean());
        }
        if (o.has("trigger")) {
            e.setTrigger(o.get("trigger").getAsString());
        }
        if (o.has("itemId")) {
            e.setItemId(o.get("itemId").getAsString());
        }
        if (o.has("prompt")) {
            e.setPrompt(o.get("prompt").getAsString());
        }
        if (o.has("fallback")) {
            e.setFallback(o.get("fallback").getAsString());
        }
        if (o.has("cooldownSeconds")) {
            e.setCooldownSeconds(o.get("cooldownSeconds").getAsInt());
        }
        if (o.has("priority")) {
            e.setPriority(o.get("priority").getAsInt());
        }
        return e;
    }

    /** Parse one compact JSON object string (NeoForge list entries). */
    public static CompanionCustomChatEvent fromJsonString(String json) {
        if (json == null || json.isBlank()) {
            return new CompanionCustomChatEvent();
        }
        try {
            return fromJson(com.google.gson.JsonParser.parseString(json).getAsJsonObject());
        } catch (Exception ignored) {
            return new CompanionCustomChatEvent();
        }
    }

    public static String normalizeTrigger(String raw) {
        if (raw == null || raw.isBlank()) {
            return "item_find";
        }
        String t = raw.trim().toLowerCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (t) {
            case "find", "itemfind", "found", "found_something" -> "item_find";
            case "craft", "itemcraft", "crafting" -> "item_craft";
            case "craftready", "craft_complete" -> "craft_ready";
            case "boom", "tnt", "blast" -> "explosion";
            case "dark", "torch", "light" -> "darkness";
            case "hurt", "hurt_player" -> "damage";
            case "ambient", "chatter" -> "idle";
            default -> t;
        };
    }

    public static boolean triggerEqualsKind(String trigger, CompanionRecentActionKind kind) {
        if (trigger == null || kind == null || kind == CompanionRecentActionKind.CUSTOM) {
            return false;
        }
        String expected = kind.name().toLowerCase(java.util.Locale.ROOT);
        return normalizeTrigger(trigger).equals(expected);
    }

    private static String sanitizeId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String cleaned = raw.trim().toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9_.:-]", "_");
        if (cleaned.length() > MAX_ID) {
            cleaned = cleaned.substring(0, MAX_ID);
        }
        return cleaned;
    }

    private static String clamp(String value, int max) {
        if (value == null) {
            return "";
        }
        String t = value.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
