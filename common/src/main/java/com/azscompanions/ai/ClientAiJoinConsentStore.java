package com.azscompanions.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client-side disk store for join-time LLM consent decisions
 * ({@code config/azscompanions-ai-join-consent.json}).
 */
public final class ClientAiJoinConsentStore {
    public static final String FILE_NAME = "azscompanions-ai-join-consent.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private ClientAiJoinConsentStore() {
    }

    public static Map<String, ClientAiJoinConsent.Decision> load(Path path) throws IOException {
        Map<String, ClientAiJoinConsent.Decision> out = new LinkedHashMap<>();
        if (path == null || !Files.exists(path)) {
            return out;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject decisions = root.has("decisions") && root.get("decisions").isJsonObject()
                    ? root.getAsJsonObject("decisions")
                    : root;
            for (Map.Entry<String, JsonElement> e : decisions.entrySet()) {
                if (e.getValue() == null || !e.getValue().isJsonPrimitive()) {
                    continue;
                }
                ClientAiJoinConsent.Decision d = parseDecision(e.getValue().getAsString());
                if (d != ClientAiJoinConsent.Decision.UNDECIDED) {
                    out.put(ClientAiJoinConsent.normalizeKey(e.getKey()), d);
                }
            }
        }
        return out;
    }

    public static void save(Path path, Map<String, ClientAiJoinConsent.Decision> decisions) throws IOException {
        if (path == null) {
            return;
        }
        Files.createDirectories(path.getParent());
        JsonObject root = new JsonObject();
        JsonObject map = new JsonObject();
        if (decisions != null) {
            for (Map.Entry<String, ClientAiJoinConsent.Decision> e : decisions.entrySet()) {
                if (e.getValue() == null || e.getValue() == ClientAiJoinConsent.Decision.UNDECIDED) {
                    continue;
                }
                String key = ClientAiJoinConsent.normalizeKey(e.getKey());
                if ("unknown".equals(key)) {
                    continue;
                }
                map.addProperty(key, e.getValue().name().toLowerCase());
            }
        }
        root.add("decisions", map);
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    static ClientAiJoinConsent.Decision parseDecision(String raw) {
        if (raw == null || raw.isBlank()) {
            return ClientAiJoinConsent.Decision.UNDECIDED;
        }
        return switch (raw.trim().toLowerCase()) {
            case "accepted", "accept", "yes" -> ClientAiJoinConsent.Decision.ACCEPTED;
            case "dismissed", "dismiss", "no" -> ClientAiJoinConsent.Decision.DISMISSED;
            default -> ClientAiJoinConsent.Decision.UNDECIDED;
        };
    }
}
