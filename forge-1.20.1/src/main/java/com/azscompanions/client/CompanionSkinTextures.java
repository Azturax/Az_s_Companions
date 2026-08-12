package com.azscompanions.client;

import com.azscompanions.AzsCompanions;
import com.azscompanions.entity.CompanionContextSkinSupport;
import com.azscompanions.entity.CompanionEntity;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
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
 * Resolves companion skins from resource locations, Mojang {@code player:<uuid>} skins,
 * {@code local:} files under {@code config/azscompanions/skins/}, and {@code url:}/{@code http(s):} downloads.
 *
 * <p>Player-form render priority: active context outfit → custom {@code SkinPath} → base default.
 *
 * <p>Important: never apply {@code player:<uuid>} until the dynamic texture is registered.
 * Showing the default Steve/Alex skin while loading causes
 * Alex/Steve UV flicker against our slim/wide model.
 */
@OnlyIn(Dist.CLIENT)
public final class CompanionSkinTextures {
    public static final ResourceLocation DEFAULT_KON =
            new ResourceLocation(AzsCompanions.MOD_ID, "textures/entity/companion/kon.png");

    public record ReadySkin(UUID uuid, ResourceLocation texture, boolean slim) {
    }

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final Map<UUID, ResourceLocation> PLAYER_TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, ResourceLocation> PLAYER_CAPE_CACHE = new ConcurrentHashMap<>();
    private static final Set<UUID> CAPE_ABSENT = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Boolean> PLAYER_SLIM_CACHE = new ConcurrentHashMap<>();
    private static final Set<UUID> LOADING = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, List<Consumer<Optional<ReadySkin>>>> WAITERS = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> EXTERNAL_TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> EXTERNAL_LOADING = ConcurrentHashMap.newKeySet();
    private static final Set<String> EXTERNAL_FAILED = ConcurrentHashMap.newKeySet();
    private static final int MAX_EXTERNAL_BYTES = 2 * 1024 * 1024;

    private CompanionSkinTextures() {
    }

    public static ResourceLocation resolve(CompanionEntity entity) {
        if (ClientAppearanceDraft.matches(entity)) {
            String draftSkin = resolveDraftSkinPath(entity);
            if (draftSkin != null && !draftSkin.isBlank()) {
                ResourceLocation ready = resolveReadyOrNull(draftSkin);
                if (ready != null) {
                    return ready;
                }
                // Prefer custom draft skin over base while context URL/local loads.
                String custom = ClientAppearanceDraft.ACTIVE.skinPath;
                if (custom != null && !custom.isBlank() && !custom.equals(draftSkin)) {
                    return resolve(custom);
                }
            }
            String draftName = ClientAppearanceDraft.ACTIVE.name;
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
        // Context local/url still downloading — keep custom applied skin over base.
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

    private static String resolveDraftSkinPath(CompanionEntity entity) {
        ClientAppearanceDraft draft = ClientAppearanceDraft.ACTIVE;
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

    /**
     * Cape for Mojang {@code player:<uuid>} skins only. Kon default / resource / local → none.
     * Texture id is cached client-side with the skin; nothing persisted on the entity.
     */
    @Nullable
    public static ResourceLocation resolveCape(CompanionEntity entity) {
        UUID uuid = resolvePlayerUuid(entity);
        if (uuid == null) {
            return null;
        }
        return resolveCape(uuid);
    }

    @Nullable
    public static ResourceLocation resolveCape(UUID uuid) {
        if (uuid == null || CAPE_ABSENT.contains(uuid)) {
            return null;
        }
        ResourceLocation cached = PLAYER_CAPE_CACHE.get(uuid);
        if (cached != null) {
            return cached;
        }
        Minecraft mc = Minecraft.getInstance();
        if (tryCacheCapeFromConnection(mc, uuid)) {
            return PLAYER_CAPE_CACHE.get(uuid);
        }
        if (mc.player != null && uuid.equals(mc.player.getUUID()) && mc.player.getCloakTextureLocation() != null) {
            PLAYER_CAPE_CACHE.put(uuid, mc.player.getCloakTextureLocation());
            return mc.player.getCloakTextureLocation();
        }
        // Cape downloads with skin fetch; kick load if needed.
        ensurePlayerSkinLoaded(uuid);
        return null;
    }

    @Nullable
    private static UUID resolvePlayerUuid(CompanionEntity entity) {
        if (ClientAppearanceDraft.matches(entity)) {
            String draftSkin = ClientAppearanceDraft.ACTIVE.skinPath;
            UUID fromDraft = parsePlayerUuid(draftSkin);
            if (fromDraft != null) {
                return fromDraft;
            }
            String draftName = ClientAppearanceDraft.ACTIVE.name;
            if (draftName != null && draftName.trim().equalsIgnoreCase("Kon")) {
                return null;
            }
        }
        String skinPath = entity.getSkinPath();
        UUID fromPath = parsePlayerUuid(skinPath);
        if (fromPath != null) {
            return fromPath;
        }
        if (skinPath == null || skinPath.isBlank()) {
            if (entity.isKonNamed()) {
                return null;
            }
            return entity.getOwnerUuid();
        }
        return null;
    }

    @Nullable
    private static UUID parsePlayerUuid(@Nullable String skinPath) {
        if (skinPath == null || !skinPath.startsWith("player:")) {
            return null;
        }
        try {
            return UUID.fromString(skinPath.substring("player:".length()).trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static ResourceLocation resolve(String skinPath) {
        ResourceLocation ready = resolveReadyOrNull(skinPath);
        return ready != null ? ready : DEFAULT_KON;
    }

    /**
     * Resolve immediately when cached; kick async load for url/local and return null until ready
     * so callers can fall back to custom skin instead of base.
     */
    @Nullable
    public static ResourceLocation resolveReadyOrNull(String skinPath) {
        if (skinPath == null || skinPath.isBlank()) {
            return null;
        }
        if (skinPath.startsWith("player:")) {
            try {
                return resolvePlayer(UUID.fromString(skinPath.substring("player:".length()).trim()));
            } catch (IllegalArgumentException ex) {
                AzsCompanions.LOGGER.warn("Invalid player skin path: {}", skinPath);
                return DEFAULT_KON;
            }
        }
        if (CompanionContextSkinSupport.isLocalSkin(skinPath) || CompanionContextSkinSupport.isUrlSkin(skinPath)) {
            String key = skinPath.trim();
            ResourceLocation cached = EXTERNAL_TEXTURE_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
            if (!EXTERNAL_FAILED.contains(key)) {
                ensureExternalSkinLoaded(key);
            }
            return null;
        }
        ResourceLocation parsed = new ResourceLocation(skinPath);
        return parsed != null ? parsed : DEFAULT_KON;
    }

    private static void ensureExternalSkinLoaded(String skinPath) {
        if (EXTERNAL_TEXTURE_CACHE.containsKey(skinPath) || !EXTERNAL_LOADING.add(skinPath)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        CompletableFuture.supplyAsync(() -> loadExternalBytes(skinPath))
                .whenComplete((bytes, err) -> mc.execute(() -> {
                    EXTERNAL_LOADING.remove(skinPath);
                    if (err != null || bytes == null || bytes.length == 0) {
                        EXTERNAL_FAILED.add(skinPath);
                        AzsCompanions.LOGGER.warn("External skin failed for {}: {}", skinPath,
                                err != null ? err.toString() : "empty");
                        return;
                    }
                    try {
                        NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
                        image = processSkinImage(image);
                        String hash = shortHash(skinPath);
                        ResourceLocation id = new ResourceLocation(
                                AzsCompanions.MOD_ID, "dynamic_external_skin/" + hash);
                        DynamicTexture texture = new DynamicTexture(image);
                        mc.getTextureManager().register(id, texture);
                        EXTERNAL_TEXTURE_CACHE.put(skinPath, id);
                        EXTERNAL_FAILED.remove(skinPath);
                    } catch (Exception e) {
                        EXTERNAL_FAILED.add(skinPath);
                        AzsCompanions.LOGGER.warn("External skin register failed for {}", skinPath, e);
                    }
                }));
    }

    @Nullable
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
            byte[] dig = md.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig, 0, 8);
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    public static boolean isPlayerSkinReady(UUID uuid) {
        return uuid != null && PLAYER_TEXTURE_CACHE.containsKey(uuid);
    }

    @Nullable
    public static Boolean getCachedSlim(UUID uuid) {
        return PLAYER_SLIM_CACHE.get(uuid);
    }

    public static void invalidatePlayer(UUID uuid) {
        if (uuid == null) {
            return;
        }
        ResourceLocation id = PLAYER_TEXTURE_CACHE.remove(uuid);
        ResourceLocation capeId = PLAYER_CAPE_CACHE.remove(uuid);
        CAPE_ABSENT.remove(uuid);
        PLAYER_SLIM_CACHE.remove(uuid);
        LOADING.remove(uuid);
        if (id != null && id.getNamespace().equals(AzsCompanions.MOD_ID)
                && id.getPath().startsWith("dynamic_player_skin/")) {
            Minecraft.getInstance().getTextureManager().release(id);
        }
        if (capeId != null && capeId.getNamespace().equals(AzsCompanions.MOD_ID)
                && capeId.getPath().startsWith("dynamic_player_cape/")) {
            Minecraft.getInstance().getTextureManager().release(capeId);
        }
    }

    /**
     * Ensures the Mojang skin is downloaded and registered, then invokes {@code onReady}
     * on the client thread with the stable texture + slim flag.
     */
    public static void loadPlayerSkin(UUID uuid, Consumer<Optional<ReadySkin>> onReady) {
        Minecraft mc = Minecraft.getInstance();
        if (uuid == null) {
            mc.execute(() -> onReady.accept(Optional.empty()));
            return;
        }
        ResourceLocation cached = PLAYER_TEXTURE_CACHE.get(uuid);
        if (cached != null) {
            boolean slim = PLAYER_SLIM_CACHE.getOrDefault(uuid, false);
            mc.execute(() -> onReady.accept(Optional.of(new ReadySkin(uuid, cached, slim))));
            return;
        }
        WAITERS.computeIfAbsent(uuid, ignored -> new CopyOnWriteArrayList<>()).add(onReady);
        ensurePlayerSkinLoaded(uuid);
    }

    public static void ensurePlayerSkinLoaded(UUID uuid) {
        if (uuid == null) {
            return;
        }
        if (PLAYER_TEXTURE_CACHE.containsKey(uuid) || !LOADING.add(uuid)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (tryCacheFromConnection(mc, uuid)) {
            LOADING.remove(uuid);
            notifyWaiters(uuid, Optional.of(new ReadySkin(
                    uuid,
                    PLAYER_TEXTURE_CACHE.get(uuid),
                    PLAYER_SLIM_CACHE.getOrDefault(uuid, false))));
            return;
        }
        if (mc.player != null && uuid.equals(mc.player.getUUID()) && mc.player.getSkinTextureLocation() != null) {
            ResourceLocation skinLoc = mc.player.getSkinTextureLocation();
            PLAYER_TEXTURE_CACHE.put(uuid, skinLoc);
            PLAYER_SLIM_CACHE.put(uuid, mc.player.getModelName().equals("slim"));
            if (mc.player.getCloakTextureLocation() != null) {
                PLAYER_CAPE_CACHE.put(uuid, mc.player.getCloakTextureLocation());
                CAPE_ABSENT.remove(uuid);
            } else {
                CAPE_ABSENT.add(uuid);
            }
            LOADING.remove(uuid);
            notifyWaiters(uuid, Optional.of(new ReadySkin(
                    uuid, skinLoc, mc.player.getModelName().equals("slim"))));
            return;
        }
        CompletableFuture.supplyAsync(() -> fetchSkinBytes(uuid))
                .whenComplete((payload, err) -> mc.execute(() -> {
                    LOADING.remove(uuid);
                    if (err != null) {
                        AzsCompanions.LOGGER.warn("Player skin download failed for {}: {}", uuid, err.toString());
                        notifyWaiters(uuid, Optional.empty());
                        return;
                    }
                    if (payload == null) {
                        notifyWaiters(uuid, Optional.empty());
                        return;
                    }
                    try {
                        NativeImage image = NativeImage.read(new ByteArrayInputStream(payload.bytes()));
                        image = processSkinImage(image);
                        ResourceLocation id = new ResourceLocation(
                                AzsCompanions.MOD_ID, "dynamic_player_skin/" + uuid.toString().replace("-", ""));
                        ResourceLocation previous = PLAYER_TEXTURE_CACHE.put(uuid, id);
                        PLAYER_SLIM_CACHE.put(uuid, payload.slim());
                        mc.getTextureManager().register(id, new DynamicTexture(image));
                        if (previous != null
                                && !previous.equals(id)
                                && previous.getNamespace().equals(AzsCompanions.MOD_ID)
                                && previous.getPath().startsWith("dynamic_player_skin/")) {
                            mc.getTextureManager().release(previous);
                        }
                        registerCapeTexture(mc, uuid, payload.capeBytes());
                        AzsCompanions.LOGGER.debug("Cached Mojang skin for {} (slim={})", uuid, payload.slim());
                        notifyWaiters(uuid, Optional.of(new ReadySkin(uuid, id, payload.slim())));
                    } catch (Exception e) {
                        AzsCompanions.LOGGER.warn("Failed registering player skin for {}", uuid, e);
                        notifyWaiters(uuid, Optional.empty());
                    }
                }));
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
                AzsCompanions.LOGGER.warn("Skin ready callback failed for {}", uuid, e);
            }
        }
    }

    /**
     * While a skin is downloading, keep a stable fallback (Kon) — never Steve/Alex defaults,
     * which glitch against slim/wide companion models.
     */
    private static ResourceLocation resolvePlayer(UUID uuid) {
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation cached = PLAYER_TEXTURE_CACHE.get(uuid);
        if (cached != null) {
            return cached;
        }
        if (tryCacheFromConnection(mc, uuid)) {
            return PLAYER_TEXTURE_CACHE.getOrDefault(uuid, DEFAULT_KON);
        }
        if (mc.player != null && uuid.equals(mc.player.getUUID()) && mc.player.getSkinTextureLocation() != null) {
            ResourceLocation skinLoc = mc.player.getSkinTextureLocation();
            PLAYER_TEXTURE_CACHE.put(uuid, skinLoc);
            PLAYER_SLIM_CACHE.put(uuid, mc.player.getModelName().equals("slim"));
            return skinLoc;
        }
        ensurePlayerSkinLoaded(uuid);
        return DEFAULT_KON;
    }

    private static boolean tryCacheFromConnection(Minecraft mc, UUID uuid) {
        if (mc.getConnection() == null) {
            return false;
        }
        var info = mc.getConnection().getPlayerInfo(uuid);
        if (info == null) {
            return false;
        }
        if (info.getSkinLocation() == null) {
            return false;
        }
        PLAYER_TEXTURE_CACHE.put(uuid, info.getSkinLocation());
        PLAYER_SLIM_CACHE.put(uuid, "slim".equals(info.getModelName()));
        if (info.getCapeLocation() != null) {
            PLAYER_CAPE_CACHE.put(uuid, info.getCapeLocation());
            CAPE_ABSENT.remove(uuid);
        } else {
            CAPE_ABSENT.add(uuid);
        }
        return true;
    }

    private static boolean tryCacheCapeFromConnection(Minecraft mc, UUID uuid) {
        if (mc.getConnection() == null) {
            return false;
        }
        var info = mc.getConnection().getPlayerInfo(uuid);
        if (info == null || info.getCapeLocation() == null) {
            CAPE_ABSENT.add(uuid);
            return false;
        }
        PLAYER_CAPE_CACHE.put(uuid, info.getCapeLocation());
        CAPE_ABSENT.remove(uuid);
        return true;
    }

    @Nullable
    private static SkinPayload fetchSkinBytes(UUID uuid) {
        PlayerSkinLookup.SessionTextures session = PlayerSkinLookup.fetchSessionTextures(uuid);
        if (session.skinUrl() == null || session.skinUrl().isBlank()) {
            AzsCompanions.LOGGER.warn("No skin URL in Mojang session profile for {}", uuid);
            return null;
        }
        String url = session.skinUrl();
        if (!(url.startsWith("http://textures.minecraft.net/")
                || url.startsWith("https://textures.minecraft.net/"))) {
            AzsCompanions.LOGGER.warn("Rejected non-Mojang skin URL for {}: {}", uuid, url);
            return null;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200 || response.body() == null || response.body().length == 0) {
                AzsCompanions.LOGGER.warn("Skin texture HTTP {} for {}", response.statusCode(), uuid);
                return null;
            }
            byte[] capeBytes = null;
            String capeUrl = session.capeUrl();
            if (capeUrl != null && (capeUrl.startsWith("https://textures.minecraft.net/")
                    || capeUrl.startsWith("http://textures.minecraft.net/"))) {
                HttpRequest capeReq = HttpRequest.newBuilder()
                        .uri(URI.create(capeUrl))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                HttpResponse<byte[]> capeRes = HTTP.send(capeReq, HttpResponse.BodyHandlers.ofByteArray());
                if (capeRes.statusCode() == 200 && capeRes.body() != null && capeRes.body().length > 0) {
                    capeBytes = capeRes.body();
                }
            }
            return new SkinPayload(response.body(), capeBytes, session.slim());
        } catch (Exception e) {
            AzsCompanions.LOGGER.warn("Failed downloading skin for {}", uuid, e);
            return null;
        }
    }

    /**
     * Mirror vanilla {@code SkinTextureDownloader}: expand legacy 64×32 skins to 64×64 and
     * apply the Notch outer-head transparency / opaque body fixes so UVs match PlayerModel.
     */
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
            // Mirror right limbs into left-limb slots (vanilla legacy conversion).
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

    private static void registerCapeTexture(Minecraft mc, UUID uuid, @Nullable byte[] capeBytes) {
        if (capeBytes == null || capeBytes.length == 0) {
            CAPE_ABSENT.add(uuid);
            return;
        }
        try {
            NativeImage capeImage = NativeImage.read(new ByteArrayInputStream(capeBytes));
            ResourceLocation capeId = new ResourceLocation(
                    AzsCompanions.MOD_ID, "dynamic_player_cape/" + uuid.toString().replace("-", ""));
            ResourceLocation previous = PLAYER_CAPE_CACHE.put(uuid, capeId);
            CAPE_ABSENT.remove(uuid);
            mc.getTextureManager().register(capeId, new DynamicTexture(capeImage));
            if (previous != null
                    && !previous.equals(capeId)
                    && previous.getNamespace().equals(AzsCompanions.MOD_ID)
                    && previous.getPath().startsWith("dynamic_player_cape/")) {
                mc.getTextureManager().release(previous);
            }
        } catch (Exception e) {
            CAPE_ABSENT.add(uuid);
            AzsCompanions.LOGGER.debug("No usable cape for {}: {}", uuid, e.toString());
        }
    }

    private record SkinPayload(byte[] bytes, @Nullable byte[] capeBytes, boolean slim) {
    }
}
