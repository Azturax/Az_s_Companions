package com.azscompanions.config;

import com.azscompanions.loot.CompanionLootSupport;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Fabric common/gameplay settings → {@code config/azscompanions-common.json}.
 * Mirrors NeoForge {@code azscompanions-common.toml} keys used here (e.g. {@code world.enableLoot}).
 */
public final class FabricCommonConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static volatile boolean enableLoot = CompanionLootSupport.DEFAULT_ENABLE_LOOT;

    private FabricCommonConfig() {
    }

    public static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("azscompanions-common.json");
    }

    public static void loadOrCreate() {
        Path path = configPath();
        try {
            if (!Files.exists(path)) {
                saveDefaults(path);
                apply();
                return;
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject world = root.has("world") && root.get("world").isJsonObject()
                        ? root.getAsJsonObject("world") : root;
                if (world.has("enableLoot")) {
                    enableLoot = world.get("enableLoot").getAsBoolean();
                } else {
                    enableLoot = CompanionLootSupport.DEFAULT_ENABLE_LOOT;
                }
            }
            apply();
        } catch (Exception e) {
            enableLoot = CompanionLootSupport.DEFAULT_ENABLE_LOOT;
            apply();
            throw new RuntimeException("Failed to load " + path, e);
        }
    }

    private static void saveDefaults(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        JsonObject root = new JsonObject();
        root.addProperty("_comment",
                "Az's Companions common/gameplay. Mirrors NeoForge azscompanions-common.toml [world].");
        JsonObject world = new JsonObject();
        world.addProperty("enableLoot", CompanionLootSupport.DEFAULT_ENABLE_LOOT);
        root.add("world", world);
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
        enableLoot = CompanionLootSupport.DEFAULT_ENABLE_LOOT;
    }

    private static void apply() {
        CompanionLootSupport.setLootInjectionEnabled(enableLoot);
    }

    public static boolean enableLoot() {
        return enableLoot;
    }
}
