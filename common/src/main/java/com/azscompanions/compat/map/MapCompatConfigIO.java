package com.azscompanions.compat.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Fabric (and any non-TOML) loader for {@link MapCompatSettings}.
 */
public final class MapCompatConfigIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private MapCompatConfigIO() {
    }

    public static MapCompatSettings loadOrCreate(Path path) throws IOException {
        if (!Files.exists(path)) {
            MapCompatSettings defaults = new MapCompatSettings();
            save(path, defaults);
            return defaults;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            return fromJson(root);
        }
    }

    public static void save(Path path, MapCompatSettings settings) throws IOException {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(toJson(settings), writer);
        }
    }

    public static MapCompatSettings fromJson(JsonObject root) {
        MapCompatSettings s = new MapCompatSettings();
        if (root.has("showOnMinimap")) {
            s.setShowOnMinimap(root.get("showOnMinimap").getAsBoolean());
        }
        if (root.has("showChildrenOnMap")) {
            s.setShowChildrenOnMap(root.get("showChildrenOnMap").getAsBoolean());
        }
        if (root.has("showNameOnMap")) {
            s.setShowNameOnMap(root.get("showNameOnMap").getAsBoolean());
        }
        if (root.has("showOwnerOnMap")) {
            s.setShowOwnerOnMap(root.get("showOwnerOnMap").getAsBoolean());
        }
        if (root.has("iconColorArgb")) {
            s.setIconColorArgb(parseArgb(root.get("iconColorArgb").getAsString(), s.iconColorArgb()));
        }
        return s;
    }

    public static JsonObject toJson(MapCompatSettings s) {
        JsonObject root = new JsonObject();
        root.addProperty("showOnMinimap", s.showOnMinimap());
        root.addProperty("showChildrenOnMap", s.showChildrenOnMap());
        root.addProperty("showNameOnMap", s.showNameOnMap());
        root.addProperty("showOwnerOnMap", s.showOwnerOnMap());
        root.addProperty("iconColorArgb", String.format("0x%08X", s.iconColorArgb()));
        return root;
    }

    static int parseArgb(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String t = raw.trim();
        try {
            if (t.startsWith("0x") || t.startsWith("0X")) {
                return (int) Long.parseLong(t.substring(2), 16);
            }
            if (t.startsWith("#")) {
                String hex = t.substring(1);
                if (hex.length() == 6) {
                    return (int) Long.parseLong("FF" + hex, 16);
                }
                return (int) Long.parseLong(hex, 16);
            }
            return (int) Long.parseLong(t);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
