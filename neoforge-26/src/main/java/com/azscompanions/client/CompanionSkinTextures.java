package com.azscompanions.client;

import com.azscompanions.AzsCompanions;
import com.azscompanions.entity.CompanionEntity;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
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
 * Resolves companion skins from resource locations and Mojang {@code player:<uuid>} skins.
 * Local file skins are not supported.
 *
 * <p>Important: never apply {@code player:<uuid>} until the dynamic texture is registered.
 * Showing {@link net.minecraft.client.resources.DefaultPlayerSkin} while loading causes
 * Alex/Steve UV flicker against our slim/wide model.
 */
@OnlyIn(Dist.CLIENT)
public final class CompanionSkinTextures {
    public static final Identifier DEFAULT_KON =
            Identifier.fromNamespaceAndPath(AzsCompanions.MOD_ID, "textures/entity/companion/kon.png");

    public record ReadySkin(UUID uuid, Identifier texture, boolean slim) {
    }

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final Map<UUID, Identifier> PLAYER_TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Identifier> PLAYER_CAPE_CACHE = new ConcurrentHashMap<>();
    private static final Set<UUID> CAPE_ABSENT = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Boolean> PLAYER_SLIM_CACHE = new ConcurrentHashMap<>();
    private static final Set<UUID> LOADING = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, List<Consumer<Optional<ReadySkin>>>> WAITERS = new ConcurrentHashMap<>();

    private CompanionSkinTextures() {
    }

    public static Identifier resolve(CompanionEntity entity) {
        if (ClientAppearanceDraft.matches(entity)) {
            String draftSkin = ClientAppearanceDraft.ACTIVE.skinPath;
            if (draftSkin != null && !draftSkin.isBlank()) {
                return resolve(draftSkin);
            }
            String draftName = ClientAppearanceDraft.ACTIVE.name;
            if (draftName != null && draftName.trim().equalsIgnoreCase("Kon")) {
                return DEFAULT_KON;
            }
        }
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

    /**
     * Cape for Mojang {@code player:<uuid>} skins only. Kon default / resource / local → none.
     * Texture id is cached client-side with the skin; nothing persisted on the entity.
     */
    @Nullable
    public static Identifier resolveCape(CompanionEntity entity) {
        UUID uuid = resolvePlayerUuid(entity);
        if (uuid == null) {
            return null;
        }
        return resolveCape(uuid);
    }

    @Nullable
    public static Identifier resolveCape(UUID uuid) {
        if (uuid == null || CAPE_ABSENT.contains(uuid)) {
            return null;
        }
        Identifier cached = PLAYER_CAPE_CACHE.get(uuid);
        if (cached != null) {
            return cached;
        }
        Minecraft mc = Minecraft.getInstance();
        if (tryCacheCapeFromConnection(mc, uuid)) {
            return PLAYER_CAPE_CACHE.get(uuid);
        }
        if (mc.player != null && uuid.equals(mc.player.getUUID())) {
            PlayerSkin skin = mc.player.getSkin();
            if (skin != null && skin.capeTexture() != null) {
                PLAYER_CAPE_CACHE.put(uuid, skin.capeTexture());
                return skin.capeTexture();
            }
            CAPE_ABSENT.add(uuid);
            return null;
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

    public static Identifier resolve(String skinPath) {
        if (skinPath == null || skinPath.isBlank()) {
            return DEFAULT_KON;
        }
        if (skinPath.startsWith("player:")) {
            try {
                return resolvePlayer(UUID.fromString(skinPath.substring("player:".length()).trim()));
            } catch (IllegalArgumentException ex) {
                AzsCompanions.LOGGER.warn("Invalid player skin path: {}", skinPath);
                return DEFAULT_KON;
            }
        }
        if (skinPath.startsWith("local:")) {
            return DEFAULT_KON;
        }
        Identifier parsed = Identifier.tryParse(skinPath);
        return parsed != null ? parsed : DEFAULT_KON;
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
        Identifier id = PLAYER_TEXTURE_CACHE.remove(uuid);
        Identifier capeId = PLAYER_CAPE_CACHE.remove(uuid);
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
        Identifier cached = PLAYER_TEXTURE_CACHE.get(uuid);
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
        if (mc.player != null && uuid.equals(mc.player.getUUID())) {
            PlayerSkin skin = mc.player.getSkin();
            if (skin != null && skin.texture() != null) {
                PLAYER_TEXTURE_CACHE.put(uuid, skin.texture());
                PLAYER_SLIM_CACHE.put(uuid, skin.model() == PlayerSkin.Model.SLIM);
                if (skin.capeTexture() != null) {
                    PLAYER_CAPE_CACHE.put(uuid, skin.capeTexture());
                    CAPE_ABSENT.remove(uuid);
                } else {
                    CAPE_ABSENT.add(uuid);
                }
                LOADING.remove(uuid);
                notifyWaiters(uuid, Optional.of(new ReadySkin(
                        uuid, skin.texture(), skin.model() == PlayerSkin.Model.SLIM)));
                return;
            }
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
                        Identifier id = Identifier.fromNamespaceAndPath(
                                AzsCompanions.MOD_ID, "dynamic_player_skin/" + uuid.toString().replace("-", ""));
                        Identifier previous = PLAYER_TEXTURE_CACHE.put(uuid, id);
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
    private static Identifier resolvePlayer(UUID uuid) {
        Minecraft mc = Minecraft.getInstance();
        Identifier cached = PLAYER_TEXTURE_CACHE.get(uuid);
        if (cached != null) {
            return cached;
        }
        if (tryCacheFromConnection(mc, uuid)) {
            return PLAYER_TEXTURE_CACHE.getOrDefault(uuid, DEFAULT_KON);
        }
        if (mc.player != null && uuid.equals(mc.player.getUUID())) {
            PlayerSkin skin = mc.player.getSkin();
            if (skin != null && skin.texture() != null) {
                PLAYER_TEXTURE_CACHE.put(uuid, skin.texture());
                PLAYER_SLIM_CACHE.put(uuid, skin.model() == PlayerSkin.Model.SLIM);
                return skin.texture();
            }
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
        PlayerSkin skin = info.getSkin();
        if (skin == null || skin.texture() == null) {
            return false;
        }
        PLAYER_TEXTURE_CACHE.put(uuid, skin.texture());
        PLAYER_SLIM_CACHE.put(uuid, skin.model() == PlayerSkin.Model.SLIM);
        if (skin.capeTexture() != null) {
            PLAYER_CAPE_CACHE.put(uuid, skin.capeTexture());
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
        if (info == null || info.getSkin() == null) {
            return false;
        }
        Identifier cape = info.getSkin().capeTexture();
        if (cape == null) {
            CAPE_ABSENT.add(uuid);
            return false;
        }
        PLAYER_CAPE_CACHE.put(uuid, cape);
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
            Identifier capeId = Identifier.fromNamespaceAndPath(
                    AzsCompanions.MOD_ID, "dynamic_player_cape/" + uuid.toString().replace("-", ""));
            Identifier previous = PLAYER_CAPE_CACHE.put(uuid, capeId);
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
