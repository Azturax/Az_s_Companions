package com.azscompanions.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.azscompanions.AzsCompanions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Client-side Minecraft username → UUID (+ slim arm hint) lookup via Mojang APIs.
 */
@OnlyIn(Dist.CLIENT)
public final class PlayerSkinLookup {
    public record ResolvedPlayer(UUID uuid, boolean slim, @Nullable String skinUrl) {
    }

    private static final Pattern VALID_USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private PlayerSkinLookup() {
    }

    public static boolean isValidUsername(String name) {
        return name != null && VALID_USERNAME.matcher(name).matches();
    }

    /** Resolves UUID asynchronously; callback always runs on the MC client thread. */
    public static void lookupUuidAsync(String username, Consumer<Optional<UUID>> onClientThread) {
        lookupPlayerAsync(username, opt -> onClientThread.accept(opt.map(ResolvedPlayer::uuid)));
    }

    /**
     * Resolves UUID, slim-arm flag, and skin URL from Mojang profile + session APIs.
     * Callback always runs on the MC client thread.
     */
    public static void lookupPlayerAsync(String username, Consumer<Optional<ResolvedPlayer>> onClientThread) {
        Minecraft mc = Minecraft.getInstance();
        String trimmed = username == null ? "" : username.trim();
        if (!isValidUsername(trimmed)) {
            mc.execute(() -> onClientThread.accept(Optional.empty()));
            return;
        }

        Optional<UUID> online = findOnlineUuid(mc, trimmed);
        if (online.isPresent()) {
            UUID uuid = online.get();
            boolean slim = false;
            if (mc.getConnection() != null) {
                PlayerInfo info = mc.getConnection().getPlayerInfo(uuid);
                if (info != null && info.getModelName() != null) {
                    slim = info.getModelName().contains("slim");
                }
            }
            final boolean slimFinal = slim;
            mc.execute(() -> onClientThread.accept(Optional.of(new ResolvedPlayer(uuid, slimFinal, null))));
            // Still warm the texture cache from Mojang in the background.
            CompanionSkinTextures.ensurePlayerSkinLoaded(uuid);
            return;
        }

        CompletableFuture.supplyAsync(() -> fetchMojangPlayer(trimmed))
                .whenComplete((result, err) -> mc.execute(() -> {
                    if (err != null) {
                        AzsCompanions.LOGGER.warn("Username skin lookup failed for {}: {}", trimmed, err.toString());
                        onClientThread.accept(Optional.empty());
                    } else if (result == null || result.isEmpty()) {
                        AzsCompanions.LOGGER.info("No Mojang profile for username '{}'", trimmed);
                        onClientThread.accept(Optional.empty());
                    } else {
                        ResolvedPlayer player = result.get();
                        CompanionSkinTextures.ensurePlayerSkinLoaded(player.uuid());
                        onClientThread.accept(result);
                    }
                }));
    }

    private static Optional<UUID> findOnlineUuid(Minecraft mc, String username) {
        if (mc.getConnection() == null) {
            return Optional.empty();
        }
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            if (info.getProfile() != null
                    && info.getProfile().getName() != null
                    && info.getProfile().getName().equalsIgnoreCase(username)) {
                return Optional.of(info.getProfile().getId());
            }
        }
        if (mc.player != null
                && mc.player.getGameProfile().getName() != null
                && mc.player.getGameProfile().getName().equalsIgnoreCase(username)) {
            return Optional.of(mc.player.getUUID());
        }
        return Optional.empty();
    }

    private static Optional<ResolvedPlayer> fetchMojangPlayer(String username) {
        try {
            String encoded = URLEncoder.encode(username, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + encoded))
                    .timeout(Duration.ofSeconds(6))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 204 || response.statusCode() == 404) {
                return Optional.empty();
            }
            if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
                AzsCompanions.LOGGER.warn("Mojang username API HTTP {} for {}", response.statusCode(), username);
                return Optional.empty();
            }
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!json.has("id")) {
                return Optional.empty();
            }
            UUID uuid = uuidFromUndashed(json.get("id").getAsString());
            SessionTextures textures = fetchSessionTextures(uuid);
            return Optional.of(new ResolvedPlayer(uuid, textures.slim, textures.skinUrl));
        } catch (Exception e) {
            AzsCompanions.LOGGER.warn("Mojang profile lookup failed for {}", username.toLowerCase(Locale.ROOT), e);
            return Optional.empty();
        }
    }

    static SessionTextures fetchSessionTextures(UUID uuid) {
        try {
            String undashed = uuid.toString().replace("-", "");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + undashed))
                    .timeout(Duration.ofSeconds(8))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
                AzsCompanions.LOGGER.warn("Session profile HTTP {} for {}", response.statusCode(), uuid);
                return SessionTextures.EMPTY;
            }
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!json.has("properties") || !json.get("properties").isJsonArray()) {
                return SessionTextures.EMPTY;
            }
            for (var el : json.getAsJsonArray("properties")) {
                JsonObject prop = el.getAsJsonObject();
                if (!"textures".equals(prop.get("name").getAsString()) || !prop.has("value")) {
                    continue;
                }
                String decoded = new String(Base64.getDecoder().decode(prop.get("value").getAsString()), StandardCharsets.UTF_8);
                JsonObject texturesRoot = JsonParser.parseString(decoded).getAsJsonObject();
                if (!texturesRoot.has("textures")) {
                    return SessionTextures.EMPTY;
                }
                JsonObject textures = texturesRoot.getAsJsonObject("textures");
                if (!textures.has("SKIN")) {
                    return SessionTextures.EMPTY;
                }
                JsonObject skin = textures.getAsJsonObject("SKIN");
                String url = skin.has("url") ? skin.get("url").getAsString() : null;
                boolean slim = false;
                if (skin.has("metadata") && skin.getAsJsonObject("metadata").has("model")) {
                    slim = "slim".equalsIgnoreCase(skin.getAsJsonObject("metadata").get("model").getAsString());
                }
                String capeUrl = null;
                if (textures.has("CAPE")) {
                    JsonObject cape = textures.getAsJsonObject("CAPE");
                    if (cape.has("url")) {
                        capeUrl = cape.get("url").getAsString();
                    }
                }
                return new SessionTextures(url, capeUrl, slim);
            }
        } catch (Exception e) {
            AzsCompanions.LOGGER.warn("Failed reading session textures for {}", uuid, e);
        }
        return SessionTextures.EMPTY;
    }

    private static UUID uuidFromUndashed(String hex) {
        String id = hex == null ? "" : hex.trim().replace("-", "");
        if (id.length() != 32) {
            throw new IllegalArgumentException("Invalid Mojang UUID: " + hex);
        }
        String dashed = id.substring(0, 8) + "-"
                + id.substring(8, 12) + "-"
                + id.substring(12, 16) + "-"
                + id.substring(16, 20) + "-"
                + id.substring(20);
        return UUID.fromString(dashed);
    }

    /** Package-visible session texture payload used by {@link CompanionSkinTextures}. */
    record SessionTextures(@Nullable String skinUrl, @Nullable String capeUrl, boolean slim) {
        static final SessionTextures EMPTY = new SessionTextures(null, null, false);
    }
}
