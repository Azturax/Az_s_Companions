package com.azscompanions.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.azscompanions.AzsCompanionsFabric;
import com.azscompanions.entity.CompanionContextSkinSupport;
import com.azscompanions.entity.FabricCompanionEntity;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Companion skins for Fabric: Mojang {@code player:}, resource paths, {@code local:}, and {@code url:}.
 * Player-form priority: context outfit → custom SkinPath → base default.
 */
@Environment(EnvType.CLIENT)
public final class FabricCompanionSkinTextures {
    public static final ResourceLocation DEFAULT_KON =
            ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "textures/entity/companion/kon.png");

    public record ReadySkin(UUID uuid, ResourceLocation texture, boolean slim) {
    }

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Map<UUID, ResourceLocation> CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, ResourceLocation> CAPE_CACHE = new ConcurrentHashMap<>();
    private static final Set<UUID> CAPE_ABSENT = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Boolean> SLIM_CACHE = new ConcurrentHashMap<>();
    private static final Set<UUID> LOADING = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, List<Consumer<Optional<ReadySkin>>>> WAITERS = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> EXTERNAL_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> EXTERNAL_LOADING = ConcurrentHashMap.newKeySet();
    private static final Set<String> EXTERNAL_FAILED = ConcurrentHashMap.newKeySet();
    private static final int MAX_EXTERNAL_BYTES = 2 * 1024 * 1024;

    private FabricCompanionSkinTextures() {
    }

    public static ResourceLocation resolve(FabricCompanionEntity entity) {
        if (FabricClientAppearanceDraft.matches(entity)) {
            String draftSkin = resolveDraftSkinPath(entity);
            if (draftSkin != null && !draftSkin.isBlank()) {
                ResourceLocation ready = resolveReadyOrNull(draftSkin);
                if (ready != null) {
                    return ready;
                }
                String custom = FabricClientAppearanceDraft.ACTIVE.skinPath;
                if (custom != null && !custom.isBlank() && !custom.equals(draftSkin)) {
                    return resolve(custom);
                }
            }
            String draftName = FabricClientAppearanceDraft.ACTIVE.name;
            if (draftName != null && draftName.trim().equalsIgnoreCase("Kon")) {
                return DEFAULT_KON;
            }
        }
        String skinPath = entity.getRenderSkinPath();
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
        ResourceLocation ready = resolveReadyOrNull(skinPath);
        if (ready != null) {
            return ready;
        }
        String custom = entity.getSkinPath();
        if (custom != null && !custom.isBlank() && !custom.equals(skinPath)) {
            return resolve(custom);
        }
        if (entity.isKonNamed()) {
            return DEFAULT_KON;
        }
        UUID owner = entity.getOwnerUuid();
        if (owner != null) {
            return resolvePlayer(owner);
        }
        return DEFAULT_KON;
    }

    private static String resolveDraftSkinPath(FabricCompanionEntity entity) {
        FabricClientAppearanceDraft draft = FabricClientAppearanceDraft.ACTIVE;
        if (draft == null) {
            return entity.getRenderSkinPath();
        }
        CompanionContextSkinSupport.Context active =
                CompanionContextSkinSupport.Context.byId(entity.getActiveContextSkinId());
        return CompanionContextSkinSupport.resolveRenderSkinPath(
                draft.form != null && draft.form.isPlayer(),
                active,
                draft.sleepingSkinPath,
                draft.bathingSkinPath,
                draft.adventuringSkinPath,
                draft.skinPath);
    }

    @org.jetbrains.annotations.Nullable
    public static ResourceLocation resolveCape(FabricCompanionEntity entity) {
        String skinPath = entity.getSkinPath();
        if (FabricClientAppearanceDraft.matches(entity)
                && FabricClientAppearanceDraft.ACTIVE.skinPath != null
                && !FabricClientAppearanceDraft.ACTIVE.skinPath.isBlank()) {
            skinPath = FabricClientAppearanceDraft.ACTIVE.skinPath;
        }
        UUID uuid = null;
        if (skinPath != null && skinPath.startsWith("player:")) {
            try {
                uuid = UUID.fromString(skinPath.substring("player:".length()).trim());
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        } else if (skinPath == null || skinPath.isBlank()) {
            if (entity.isKonNamed()) {
                return null;
            }
            uuid = entity.getOwnerUuid();
        }
        if (uuid == null || CAPE_ABSENT.contains(uuid)) {
            return null;
        }
        ResourceLocation cached = CAPE_CACHE.get(uuid);
        if (cached != null) {
            return cached;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            var info = mc.getConnection().getPlayerInfo(uuid);
            if (info != null && info.getSkin() != null && info.getSkin().capeTexture() != null) {
                CAPE_CACHE.put(uuid, info.getSkin().capeTexture());
                return info.getSkin().capeTexture();
            }
        }
        if (mc.player != null && uuid.equals(mc.player.getUUID()) && mc.player.getSkin() != null
                && mc.player.getSkin().capeTexture() != null) {
            CAPE_CACHE.put(uuid, mc.player.getSkin().capeTexture());
            return mc.player.getSkin().capeTexture();
        }
        // Kick async skin+cape download; do not mark absent until fetch finishes.
        ensureLoaded(uuid);
        return null;
    }

    public static void loadPlayerSkin(UUID uuid, Consumer<Optional<ReadySkin>> onReady) {
        Minecraft mc = Minecraft.getInstance();
        if (uuid == null) {
            mc.execute(() -> onReady.accept(Optional.empty()));
            return;
        }
        ResourceLocation cached = CACHE.get(uuid);
        if (cached != null) {
            boolean slim = SLIM_CACHE.getOrDefault(uuid, false);
            mc.execute(() -> onReady.accept(Optional.of(new ReadySkin(uuid, cached, slim))));
            return;
        }
        WAITERS.computeIfAbsent(uuid, ignored -> new CopyOnWriteArrayList<>()).add(onReady);
        ensurePlayerSkinLoaded(uuid);
    }

    public static void ensurePlayerSkinLoaded(UUID uuid) {
        ensureLoaded(uuid);
    }

    private static void notifyWaiters(UUID uuid, Optional<ReadySkin> ready) {
        List<Consumer<Optional<ReadySkin>>> waiters = WAITERS.remove(uuid);
        if (waiters == null) {
            return;
        }
        for (Consumer<Optional<ReadySkin>> waiter : waiters) {
            try {
                waiter.accept(ready);
            } catch (Exception e) {
                AzsCompanionsFabric.LOGGER.warn("Skin ready callback failed for {}", uuid, e);
            }
        }
    }

    public static ResourceLocation resolve(String skinPath) {
        ResourceLocation ready = resolveReadyOrNull(skinPath);
        return ready != null ? ready : DEFAULT_KON;
    }

    public static ResourceLocation resolveReadyOrNull(String skinPath) {
        if (skinPath == null || skinPath.isBlank()) {
            return null;
        }
        if (skinPath.startsWith("player:")) {
            try {
                return resolvePlayer(UUID.fromString(skinPath.substring("player:".length()).trim()));
            } catch (IllegalArgumentException ex) {
                return DEFAULT_KON;
            }
        }
        if (CompanionContextSkinSupport.isLocalSkin(skinPath) || CompanionContextSkinSupport.isUrlSkin(skinPath)) {
            String key = skinPath.trim();
            ResourceLocation cached = EXTERNAL_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
            if (!EXTERNAL_FAILED.contains(key)) {
                ensureExternalLoaded(key);
            }
            return null;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(skinPath);
        return parsed != null ? parsed : DEFAULT_KON;
    }

    private static void ensureExternalLoaded(String skinPath) {
        if (EXTERNAL_CACHE.containsKey(skinPath) || !EXTERNAL_LOADING.add(skinPath)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        CompletableFuture.supplyAsync(() -> loadExternalBytes(skinPath))
                .whenComplete((bytes, err) -> mc.execute(() -> {
                    EXTERNAL_LOADING.remove(skinPath);
                    if (err != null || bytes == null || bytes.length == 0) {
                        EXTERNAL_FAILED.add(skinPath);
                        AzsCompanionsFabric.LOGGER.warn("External skin failed for {}: {}", skinPath,
                                err != null ? err.toString() : "empty");
                        return;
                    }
                    try {
                        NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
                        image = processSkinImage(image);
                        String hash = shortHash(skinPath);
                        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                                AzsCompanionsFabric.MOD_ID, "dynamic_external_skin/" + hash);
                        mc.getTextureManager().register(id, new DynamicTexture(() -> "azscompanions:dynamic", image));
                        EXTERNAL_CACHE.put(skinPath, id);
                        EXTERNAL_FAILED.remove(skinPath);
                    } catch (Exception e) {
                        EXTERNAL_FAILED.add(skinPath);
                        AzsCompanionsFabric.LOGGER.warn("External skin register failed for {}", skinPath, e);
                    }
                }));
    }

    private static byte[] loadExternalBytes(String skinPath) {
        try {
            if (CompanionContextSkinSupport.isLocalSkin(skinPath)) {
                String relative = CompanionContextSkinSupport.extractLocalRelative(skinPath);
                if (relative == null) {
                    return null;
                }
                Path base = Minecraft.getInstance().gameDirectory.toPath()
                        .resolve("config").resolve("azscompanions").resolve("skins").normalize();
                Path file = base.resolve(relative).normalize();
                if (!file.startsWith(base) || !Files.isRegularFile(file)) {
                    return null;
                }
                long size = Files.size(file);
                if (size <= 0 || size > MAX_EXTERNAL_BYTES) {
                    return null;
                }
                return Files.readAllBytes(file);
            }
            String url = CompanionContextSkinSupport.extractUrl(skinPath);
            if (url == null) {
                return null;
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(12))
                    .GET()
                    .build();
            HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200 || response.body() == null) {
                return null;
            }
            try (InputStream in = response.body()) {
                byte[] data = in.readNBytes(MAX_EXTERNAL_BYTES + 1);
                if (data.length == 0 || data.length > MAX_EXTERNAL_BYTES) {
                    return null;
                }
                return data;
            }
        } catch (IOException | InterruptedException | IllegalArgumentException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    private static String shortHash(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig, 0, 8);
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
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
                        notifyWaiters(uuid, Optional.empty());
                        return;
                    }
                    try {
                        NativeImage image = NativeImage.read(new ByteArrayInputStream(payload.bytes()));
                        image = processSkinImage(image);
                        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                                AzsCompanionsFabric.MOD_ID, "dynamic_player_skin/" + uuid.toString().replace("-", ""));
                        ResourceLocation previous = CACHE.put(uuid, id);
                        SLIM_CACHE.put(uuid, payload.slim());
                        mc.getTextureManager().register(id, new DynamicTexture(() -> "azscompanions:dynamic", image));
                        if (previous != null
                                && !previous.equals(id)
                                && previous.getNamespace().equals(AzsCompanionsFabric.MOD_ID)
                                && previous.getPath().startsWith("dynamic_player_skin/")) {
                            mc.getTextureManager().release(previous);
                        }
                        registerCape(mc, uuid, payload.capeBytes());
                        notifyWaiters(uuid, Optional.of(new ReadySkin(uuid, id, payload.slim())));
                    } catch (Exception e) {
                        AzsCompanionsFabric.LOGGER.warn("Failed registering Fabric skin for {}", uuid, e);
                        notifyWaiters(uuid, Optional.empty());
                    }
                }));
    }

    private static void registerCape(Minecraft mc, UUID uuid, byte[] capeBytes) {
        if (capeBytes == null || capeBytes.length == 0) {
            CAPE_ABSENT.add(uuid);
            return;
        }
        try {
            NativeImage capeImage = NativeImage.read(new ByteArrayInputStream(capeBytes));
            ResourceLocation capeId = ResourceLocation.fromNamespaceAndPath(
                    AzsCompanionsFabric.MOD_ID, "dynamic_player_cape/" + uuid.toString().replace("-", ""));
            ResourceLocation previous = CAPE_CACHE.put(uuid, capeId);
            CAPE_ABSENT.remove(uuid);
            mc.getTextureManager().register(capeId, new DynamicTexture(() -> "azscompanions:dynamic", capeImage));
            if (previous != null
                    && !previous.equals(capeId)
                    && previous.getNamespace().equals(AzsCompanionsFabric.MOD_ID)
                    && previous.getPath().startsWith("dynamic_player_cape/")) {
                mc.getTextureManager().release(previous);
            }
        } catch (Exception e) {
            CAPE_ABSENT.add(uuid);
            AzsCompanionsFabric.LOGGER.debug("No usable Fabric cape for {}: {}", uuid, e.toString());
        }
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
            String capeUrl = null;
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
                if (textures != null && textures.has("CAPE")) {
                    JsonObject cape = textures.getAsJsonObject("CAPE");
                    if (cape.has("url")) {
                        capeUrl = cape.get("url").getAsString();
                    }
                }
            }
            if (url == null || !(url.startsWith("https://textures.minecraft.net/")
                    || url.startsWith("http://textures.minecraft.net/"))) {
                return null;
            }
            HttpRequest skinReq = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<byte[]> skinRes = HTTP.send(skinReq, HttpResponse.BodyHandlers.ofByteArray());
            if (skinRes.statusCode() != 200 || skinRes.body() == null) {
                return null;
            }
            byte[] capeBytes = null;
            if (capeUrl != null && (capeUrl.startsWith("https://textures.minecraft.net/")
                    || capeUrl.startsWith("http://textures.minecraft.net/"))) {
                HttpRequest capeReq = HttpRequest.newBuilder().uri(URI.create(capeUrl)).timeout(Duration.ofSeconds(10)).GET().build();
                HttpResponse<byte[]> capeRes = HTTP.send(capeReq, HttpResponse.BodyHandlers.ofByteArray());
                if (capeRes.statusCode() == 200 && capeRes.body() != null && capeRes.body().length > 0) {
                    capeBytes = capeRes.body();
                }
            }
            return new SkinPayload(skinRes.body(), capeBytes, slim);
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
                    modern.setPixel(x, y, image.getPixel(x, y));
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
                image.setPixel(dstX, dstY, image.getPixel(srcX, srcY));
            }
        }
    }

    private static void doNotchTransparencyHack(NativeImage image, int x1, int y1, int x2, int y2) {
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                if ((image.getPixel(x, y) >>> 24) < 128) {
                    return;
                }
            }
        }
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                image.setPixel(x, y, image.getPixel(x, y) & 0x00FFFFFF);
            }
        }
    }

    private static void setNoAlpha(NativeImage image, int x1, int y1, int x2, int y2) {
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                image.setPixel(x, y, image.getPixel(x, y) | 0xFF000000);
            }
        }
    }

    private record SkinPayload(byte[] bytes, byte[] capeBytes, boolean slim) {
    }
}
