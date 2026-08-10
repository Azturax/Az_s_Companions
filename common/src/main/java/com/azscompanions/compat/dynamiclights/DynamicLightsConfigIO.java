package com.azscompanions.compat.dynamiclights;

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

/** Fabric (and any non-TOML) loader for {@link DynamicLightsSettings}. */
public final class DynamicLightsConfigIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private DynamicLightsConfigIO() {
    }

    public static DynamicLightsSettings loadOrCreate(Path path) throws IOException {
        if (!Files.exists(path)) {
            DynamicLightsSettings defaults = new DynamicLightsSettings();
            save(path, defaults);
            return defaults;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            return fromJson(root);
        }
    }

    public static void save(Path path, DynamicLightsSettings settings) throws IOException {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(toJson(settings), writer);
        }
    }

    public static DynamicLightsSettings fromJson(JsonObject root) {
        DynamicLightsSettings s = new DynamicLightsSettings();
        if (root.has("dynamicLightsCompat")) {
            s.setDynamicLightsCompat(root.get("dynamicLightsCompat").getAsBoolean());
        }
        return s;
    }

    public static JsonObject toJson(DynamicLightsSettings s) {
        JsonObject root = new JsonObject();
        root.addProperty("dynamicLightsCompat", s.dynamicLightsCompat());
        return root;
    }
}
