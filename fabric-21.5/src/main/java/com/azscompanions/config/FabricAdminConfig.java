package com.azscompanions.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Fabric server admin gate → {@code config/azscompanions-server.json}.
 */
public final class FabricAdminConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static volatile boolean enableAzAdminCommand = true;
    private static volatile List<String> adminWhitelist = List.of();
    private static volatile List<String> azAdminUsers = List.of();

    private FabricAdminConfig() {
    }

    public static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("azscompanions-server.json");
    }

    public static void loadOrCreate() {
        Path path = configPath();
        try {
            if (!Files.exists(path)) {
                saveDefaults(path);
                return;
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject admin = root.has("admin") && root.get("admin").isJsonObject()
                        ? root.getAsJsonObject("admin") : root;
                if (admin.has("enableAzAdminCommand")) {
                    enableAzAdminCommand = admin.get("enableAzAdminCommand").getAsBoolean();
                }
                adminWhitelist = readStringList(admin, "adminWhitelist");
                azAdminUsers = readStringList(admin, "azAdminUsers");
            }
        } catch (Exception e) {
            enableAzAdminCommand = true;
            adminWhitelist = List.of();
            azAdminUsers = List.of();
            throw new RuntimeException("Failed to load " + path, e);
        }
    }

    private static void saveDefaults(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        JsonObject root = new JsonObject();
        root.addProperty("_comment",
                "Az's Companions server admin. Whitelist UUID or player name for /az admin.");
        JsonObject admin = new JsonObject();
        admin.addProperty("enableAzAdminCommand", true);
        admin.add("adminWhitelist", new JsonArray());
        admin.add("azAdminUsers", new JsonArray());
        root.add("admin", admin);
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
        enableAzAdminCommand = true;
        adminWhitelist = List.of();
        azAdminUsers = List.of();
    }

    private static List<String> readStringList(JsonObject obj, String key) {
        List<String> out = new ArrayList<>();
        if (!obj.has(key)) {
            return List.of();
        }
        if (obj.get(key).isJsonArray()) {
            obj.getAsJsonArray(key).forEach(e -> {
                if (e != null && e.isJsonPrimitive()) {
                    out.add(e.getAsString());
                }
            });
        } else {
            for (String part : obj.get(key).getAsString().split(",")) {
                if (!part.isBlank()) {
                    out.add(part.trim());
                }
            }
        }
        return List.copyOf(out);
    }

    public static boolean enableAzAdminCommand() {
        return enableAzAdminCommand;
    }

    public static List<String> adminWhitelist() {
        return adminWhitelist;
    }

    public static List<String> azAdminUsers() {
        return azAdminUsers;
    }
}
