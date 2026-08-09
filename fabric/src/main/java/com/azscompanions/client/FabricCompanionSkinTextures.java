package com.azscompanions.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.azscompanions.AzsCompanionsFabric;
import com.azscompanions.entity.FabricCompanionEntity;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mojang-only companion skins for Fabric. Avoids DefaultPlayerSkin fallback while loading
 * (that mismatches slim/wide models and causes UV flicker).
 */
@Environment(EnvType.CLIENT)
public final class FabricCompanionSkinTextures {
    public static final ResourceLocation DEFAULT_KON =
            ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "textures/entity/companion/kon.png");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Map<UUID, ResourceLocation> CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> SLIM_CACHE = new ConcurrentHashMap<>();
    private static final Set<UUID> LOADING = ConcurrentHashMap.newKeySet();

    private FabricCompanionSkinTextures() {
    }

    public static ResourceLocation resolve(FabricCompanionEntity entity) {
        String skinPath = entity.getSkinPath();
        if (skinPath == null || skinPath.isBlank()) {
            if (entity.isKonNamed()) {
                return DEFAULT_KON;
            }
            UUID owner = entity.getOwnerUuid();
            if (owner != null) {
                return resolvePlayer(owner);
            }
            return DEFAULT_KON;
        }
        return resolve(skinPath);
    }

    public static ResourceLocation resolve(String skinPath) {
        if (skinPath == null || skinPath.isBlank()) {
            return DEFAULT_KON;
        }
        if (skinPath.startsWith("player:")) {
            try {
                return resolvePlayer(UUID.fromString(skinPath.substring("player:".length()).trim()));
            } catch (IllegalArgumentException ex) {
                return DEFAULT_KON;
            }
        }
        if (skinPath.startsWith("local:")) {
            return DEFAULT_KON;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(skinPath);
        return parsed != null ? parsed : DEFAULT_KON;
    }

    private static ResourceLocation resolvePlayer(UUID uuid) {
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation cached = CACHE.get(uuid);
        if (cached != null) {
            return cached;
        }
        if (mc.getConnection() != null) {
            var info = mc.getConnection().getPlayerInfo(uuid);
            if (info != null) {
                PlayerSkin skin = info.getSkin();
                if (skin != null && skin.texture() != null) {
                    CACHE.put(uuid, skin.texture());
                    SLIM_CACHE.put(uuid, skin.model() == PlayerSkin.Model.SLIM);
                    return skin.texture();
                }
            }
        }
        if (mc.player != null && uuid.equals(mc.player.getUUID())) {
            PlayerSkin skin = mc.player.getSkin();
            if (skin != null && skin.texture() != null) {
                CACHE.put(uuid, skin.texture());
                SLIM_CACHE.put(uuid, skin.model() == PlayerSkin.Model.SLIM);
                return skin.texture();
            }
        }
        ensureLoaded(uuid);
        return DEFAULT_KON;
    }

    private static void ensureLoaded(UUID uuid) {
        if (CACHE.containsKey(uuid) || !LOADING.add(uuid)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        CompletableFuture.supplyAsync(() -> download(uuid))
                .whenComplete((payload, err) -> mc.execute(() -> {
                    LOADING.remove(uuid);
                    if (err != null || payload == null) {
                        if (err != null) {
                            AzsCompanionsFabric.LOGGER.warn("Fabric skin download failed for {}", uuid, err);
                        }
                        return;
                    }
                    try {
                        NativeImage image = NativeImage.read(new ByteArrayInputStream(payload.bytes()));
                        image = processSkinImage(image);
                        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                                AzsCompanionsFabric.MOD_ID, "dynamic_player_skin/" + uuid.toString().replace("-", ""));
                        ResourceLocation previous = CACHE.put(uuid, id);
                        SLIM_CACHE.put(uuid, payload.slim());
                        mc.getTextureManager().register(id, new DynamicTexture(image));
                        if (previous != null
                                && !previous.equals(id)
                                && previous.getNamespace().equals(AzsCompanionsFabric.MOD_ID)
                                && previous.getPath().startsWith("dynamic_player_skin/")) {
                            mc.getTextureManager().release(previous);
                        }
                    } catch (Exception e) {
                        AzsCompanionsFabric.LOGGER.warn("Failed registering Fabric skin for {}", uuid, e);
                    }
                }));
    }

    private static SkinPayload download(UUID uuid) {
        try {
            String undashed = uuid.toString().replace("-", "");
            HttpRequest profileReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + undashed))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> profileRes = HTTP.send(profileReq, HttpResponse.BodyHandlers.ofString());
            if (profileRes.statusCode() != 200 || profileRes.body() == null) {
                return null;
            }
            JsonObject json = JsonParser.parseString(profileRes.body()).getAsJsonObject();
            if (!json.has("properties")) {
                return null;
            }
            String url = null;
            boolean slim = false;
            for (var el : json.getAsJsonArray("properties")) {
                JsonObject prop = el.getAsJsonObject();
                if (!"textures".equals(prop.get("name").getAsString())) {
                    continue;
                }
                String decoded = new String(Base64.getDecoder().decode(prop.get("value").getAsString()), StandardCharsets.UTF_8);
                JsonObject textures = JsonParser.parseString(decoded).getAsJsonObject().getAsJsonObject("textures");
                if (textures != null && textures.has("SKIN")) {
                    JsonObject skin = textures.getAsJsonObject("SKIN");
                    url = skin.get("url").getAsString();
                    if (skin.has("metadata") && skin.getAsJsonObject("metadata").has("model")) {
                        slim = "slim".equalsIgnoreCase(skin.getAsJsonObject("metadata").get("model").getAsString());
                    }
                }
            }
            if (url == null || !(url.startsWith("https://textures.minecraft.net/")
                    || url.startsWith("http://textures.minecraft.net/"))) {
                return null;
            }
            HttpRequest skinReq = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<byte[]> skinRes = HTTP.send(skinReq, HttpResponse.BodyHandlers.ofByteArray());
            return skinRes.statusCode() == 200 ? new SkinPayload(skinRes.body(), slim) : null;
        } catch (Exception e) {
            AzsCompanionsFabric.LOGGER.warn("Fabric Mojang skin fetch failed for {}", uuid, e);
            return null;
        }
    }

    private static NativeImage processSkinImage(NativeImage input) {
        NativeImage image = input;
        if (image.getWidth() == 64 && image.getHeight() == 32) {
            NativeImage modern = new NativeImage(64, 64, true);
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 64; x++) {
                    modern.setPixelRGBA(x, y, image.getPixelRGBA(x, y));
                }
            }
            image.close();
            image = modern;
            copyRectMirror(image, 4, 16, 16, 32, 4, 4);
            copyRectMirror(image, 8, 16, 16, 32, 4, 4);
            copyRectMirror(image, 0, 20, 24, 32, 4, 12);
            copyRectMirror(image, 4, 20, 16, 32, 4, 12);
            copyRectMirror(image, 8, 20, 8, 32, 4, 12);
            copyRectMirror(image, 12, 20, 16, 32, 4, 12);
            copyRectMirror(image, 44, 16, -8, 32, 4, 4);
            copyRectMirror(image, 48, 16, -8, 32, 4, 4);
            copyRectMirror(image, 40, 20, 0, 32, 4, 12);
            copyRectMirror(image, 44, 20, -8, 32, 4, 12);
            copyRectMirror(image, 48, 20, -16, 32, 4, 12);
            copyRectMirror(image, 52, 20, -8, 32, 4, 12);
        }
        if (image.getWidth() == 64 && image.getHeight() == 64) {
            doNotchTransparencyHack(image, 32, 0, 64, 32);
            setNoAlpha(image, 0, 0, 32, 16);
            setNoAlpha(image, 0, 16, 64, 32);
            setNoAlpha(image, 16, 48, 48, 64);
        }
        return image;
    }

    private static void copyRectMirror(NativeImage image, int x, int y, int offX, int offY, int width, int height) {
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                int srcX = x + dx;
                int srcY = y + dy;
                int dstX = x + offX + (width - 1 - dx);
                int dstY = y + offY + dy;
                if (srcX < 0 || srcY < 0 || dstX < 0 || dstY < 0
                        || srcX >= image.getWidth() || srcY >= image.getHeight()
                        || dstX >= image.getWidth() || dstY >= image.getHeight()) {
                    continue;
                }
                image.setPixelRGBA(dstX, dstY, image.getPixelRGBA(srcX, srcY));
            }
        }
    }

    private static void doNotchTransparencyHack(NativeImage image, int x1, int y1, int x2, int y2) {
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                if ((image.getPixelRGBA(x, y) >>> 24) < 128) {
                    return;
                }
            }
        }
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                image.setPixelRGBA(x, y, image.getPixelRGBA(x, y) & 0x00FFFFFF);
            }
        }
    }

    private static void setNoAlpha(NativeImage image, int x1, int y1, int x2, int y2) {
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                image.setPixelRGBA(x, y, image.getPixelRGBA(x, y) | 0xFF000000);
            }
        }
    }

    private record SkinPayload(byte[] bytes, boolean slim) {
    }
}
