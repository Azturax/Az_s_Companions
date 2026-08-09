package com.koncompanions.client;

import com.koncompanions.KonCompanions;
import com.koncompanions.entity.CompanionEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves classic Minecraft 64×64 (or legacy 64×32) player skins for companions.
 * Supports resource locations, {@code local:filename.png}, and {@code player:<uuid>}.
 */
@OnlyIn(Dist.CLIENT)
public final class CompanionSkinTextures {
    public static final ResourceLocation DEFAULT_KON =
            ResourceLocation.fromNamespaceAndPath(KonCompanions.MOD_ID, "textures/entity/companion/kon.png");

    private static final Map<String, ResourceLocation> LOCAL_CACHE = new ConcurrentHashMap<>();

    private CompanionSkinTextures() {
    }

    public static ResourceLocation resolve(CompanionEntity entity) {
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
            return resolveLocal(skinPath.substring("local:".length()));
        }
        ResourceLocation parsed = ResourceLocation.tryParse(skinPath);
        return parsed != null ? parsed : DEFAULT_KON;
    }

    private static ResourceLocation resolvePlayer(UUID uuid) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            var info = mc.getConnection().getPlayerInfo(uuid);
            if (info != null) {
                PlayerSkin skin = info.getSkin();
                if (skin != null && skin.texture() != null) {
                    return skin.texture();
                }
            }
        }
        if (mc.player != null && uuid.equals(mc.player.getUUID())) {
            PlayerSkin skin = mc.player.getSkin();
            if (skin != null && skin.texture() != null) {
                return skin.texture();
            }
        }
        GameProfile profile = new GameProfile(uuid, "companion");
        PlayerSkin skin = mc.getSkinManager().getInsecureSkin(profile);
        return skin != null && skin.texture() != null ? skin.texture() : DEFAULT_KON;
    }

    private static ResourceLocation resolveLocal(String fileName) {
        String key = fileName.toLowerCase().replaceAll("[^a-z0-9._\\-]", "_");
        return LOCAL_CACHE.computeIfAbsent(key, CompanionSkinTextures::loadLocal);
    }

    private static ResourceLocation loadLocal(String key) {
        Path file = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve(KonCompanions.MOD_ID).resolve("skins").resolve(key);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                KonCompanions.MOD_ID, "dynamic_skin/" + key.replace('.', '_'));
        if (!Files.isRegularFile(file)) {
            KonCompanions.LOGGER.warn("Missing local companion skin: {}", file);
            return DEFAULT_KON;
        }
        try (InputStream in = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(in);
            if (image.getWidth() != 64 || (image.getHeight() != 64 && image.getHeight() != 32)) {
                KonCompanions.LOGGER.warn("Companion skin {} is not classic MC skin size (got {}x{})",
                        file, image.getWidth(), image.getHeight());
            }
            DynamicTexture texture = new DynamicTexture(image);
            Minecraft.getInstance().getTextureManager().register(id, texture);
            return id;
        } catch (IOException e) {
            KonCompanions.LOGGER.error("Failed loading local companion skin {}", file, e);
            return DEFAULT_KON;
        }
    }

    public static void invalidate(String localFileName) {
        String key = localFileName.toLowerCase().replaceAll("[^a-z0-9._\\-]", "_");
        ResourceLocation id = LOCAL_CACHE.remove(key);
        if (id != null && !id.equals(DEFAULT_KON)) {
            Minecraft.getInstance().getTextureManager().release(id);
        }
    }
}
