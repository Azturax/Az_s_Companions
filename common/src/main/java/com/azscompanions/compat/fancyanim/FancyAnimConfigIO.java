package com.azscompanions.compat.fancyanim;

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

/** Fabric (and any non-TOML) loader for {@link FancyAnimSettings}. */
public final class FancyAnimConfigIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private FancyAnimConfigIO() {
    }

    public static FancyAnimSettings loadOrCreate(Path path) throws IOException {
        if (!Files.exists(path)) {
            FancyAnimSettings defaults = new FancyAnimSettings();
            save(path, defaults);
            return defaults;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            return fromJson(root);
        }
    }

    public static void save(Path path, FancyAnimSettings settings) throws IOException {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(toJson(settings), writer);
        }
    }

    public static FancyAnimSettings fromJson(JsonObject root) {
        FancyAnimSettings s = new FancyAnimSettings();
        if (root.has("translucentPlayerSkins")) {
            s.setTranslucentPlayerSkins(root.get("translucentPlayerSkins").getAsBoolean());
        }
        if (root.has("syncMobFormUuid")) {
            s.setSyncMobFormUuid(root.get("syncMobFormUuid").getAsBoolean());
        }
        return s;
    }

    public static JsonObject toJson(FancyAnimSettings s) {
        JsonObject root = new JsonObject();
        root.addProperty("translucentPlayerSkins", s.translucentPlayerSkins());
        root.addProperty("syncMobFormUuid", s.syncMobFormUuid());
        return root;
    }
}
