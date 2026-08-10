package com.azscompanions.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts spoken text + structured actions from an LLM reply.
 * Supports:
 * <ul>
 *   <li>OpenAI-style {@code tool_calls} in the raw completion JSON (when present in content wrapper)</li>
 *   <li>Fenced {@code ```json} blocks with {@code {"actions":[...]}}</li>
 *   <li>Inline {@code {"actions":[...]}} objects</li>
 * </ul>
 */
public final class CompanionAiActionParser {
    private static final Pattern FENCED_JSON = Pattern.compile(
            "```(?:json)?\\s*(\\{[\\s\\S]*?})\\s*```", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACTIONS_OBJECT = Pattern.compile(
            "\\{\\s*\"actions\"\\s*:\\s*\\[[\\s\\S]*?]\\s*}", Pattern.CASE_INSENSITIVE);

    private CompanionAiActionParser() {
    }

    public record ParsedReply(String speakText, List<CompanionAiAction> actions) {
        public ParsedReply {
            speakText = speakText == null ? "" : speakText.trim();
            actions = actions == null ? List.of() : List.copyOf(actions);
        }

        public boolean hasActions() {
            return !actions.isEmpty();
        }
    }

    public static ParsedReply parse(String rawReply) {
        if (rawReply == null || rawReply.isBlank()) {
            return new ParsedReply("", List.of());
        }
        String working = rawReply.trim();
        List<CompanionAiAction> actions = new ArrayList<>();

        // Prefer fenced JSON
        Matcher fence = FENCED_JSON.matcher(working);
        StringBuffer cleaned = new StringBuffer();
        while (fence.find()) {
            String json = fence.group(1);
            actions.addAll(parseActionsJson(json));
            fence.appendReplacement(cleaned, "");
        }
        fence.appendTail(cleaned);
        working = cleaned.toString().trim();

        if (actions.isEmpty()) {
            Matcher inline = ACTIONS_OBJECT.matcher(working);
            if (inline.find()) {
                actions.addAll(parseActionsJson(inline.group()));
                working = (working.substring(0, inline.start()) + working.substring(inline.end())).trim();
            }
        }

        // Strip leftover bare tool-call noise lines
        working = working.replaceAll("(?m)^\\s*\\{\"name\".*$", "").trim();
        return new ParsedReply(working, actions);
    }

    /** Parse OpenAI chat.completion message object that may include tool_calls. */
    public static ParsedReply parseMessageObject(JsonObject message) {
        if (message == null) {
            return new ParsedReply("", List.of());
        }
        String content = "";
        if (message.has("content") && message.get("content").isJsonPrimitive()) {
            content = message.get("content").getAsString();
        }
        List<CompanionAiAction> actions = new ArrayList<>();
        if (message.has("tool_calls") && message.get("tool_calls").isJsonArray()) {
            for (JsonElement el : message.getAsJsonArray("tool_calls")) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject call = el.getAsJsonObject();
                JsonObject fn = call.has("function") && call.get("function").isJsonObject()
                        ? call.getAsJsonObject("function") : call;
                String name = fn.has("name") ? fn.get("name").getAsString() : "";
                Map<String, String> args = new LinkedHashMap<>();
                if (fn.has("arguments")) {
                    JsonElement argsEl = fn.get("arguments");
                    if (argsEl.isJsonPrimitive()) {
                        try {
                            JsonObject argObj = JsonParser.parseString(argsEl.getAsString()).getAsJsonObject();
                            flattenArgs(argObj, args);
                        } catch (Exception ignored) {
                            args.put("raw", argsEl.getAsString());
                        }
                    } else if (argsEl.isJsonObject()) {
                        flattenArgs(argsEl.getAsJsonObject(), args);
                    }
                }
                if (!name.isBlank()) {
                    actions.add(new CompanionAiAction(name, args));
                }
            }
        }
        ParsedReply fromText = parse(content);
        List<CompanionAiAction> merged = new ArrayList<>(actions);
        merged.addAll(fromText.actions());
        return new ParsedReply(fromText.speakText(), merged);
    }

    static List<CompanionAiAction> parseActionsJson(String json) {
        List<CompanionAiAction> out = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("actions") || !root.get("actions").isJsonArray()) {
                // single action object
                if (root.has("name")) {
                    Map<String, String> args = new LinkedHashMap<>();
                    flattenArgs(root, args);
                    args.remove("name");
                    out.add(new CompanionAiAction(root.get("name").getAsString(), args));
                }
                return out;
            }
            JsonArray arr = root.getAsJsonArray("actions");
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject obj = el.getAsJsonObject();
                if (!obj.has("name")) {
                    continue;
                }
                Map<String, String> args = new LinkedHashMap<>();
                flattenArgs(obj, args);
                args.remove("name");
                out.add(new CompanionAiAction(obj.get("name").getAsString(), args));
            }
        } catch (Exception ignored) {
            // malformed — ignore
        }
        return out;
    }

    private static void flattenArgs(JsonObject obj, Map<String, String> args) {
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            String key = e.getKey().toLowerCase(Locale.ROOT);
            JsonElement v = e.getValue();
            if (v == null || v.isJsonNull()) {
                continue;
            }
            if (v.isJsonPrimitive()) {
                args.put(key, v.getAsString());
            } else {
                args.put(key, v.toString());
            }
        }
    }
}
