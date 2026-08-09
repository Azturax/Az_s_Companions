package com.koncompanions.client;

import com.koncompanions.KonCompanionsFabric;
import com.koncompanions.entity.FabricCompanionEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public final class FabricCompanionSkinTextures {
    public static final ResourceLocation DEFAULT_KON =
            ResourceLocation.fromNamespaceAndPath(KonCompanionsFabric.MOD_ID, "textures/entity/companion/kon.png");

    private static final Map<String, ResourceLocation> LOCAL_CACHE = new ConcurrentHashMap<>();

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
            String key = skinPath.substring("local:".length()).toLowerCase().replaceAll("[^a-z0-9._\\-]", "_");
            return LOCAL_CACHE.computeIfAbsent(key, FabricCompanionSkinTextures::loadLocal);
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

    private static ResourceLocation loadLocal(String key) {
        Path file = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve(KonCompanionsFabric.MOD_ID).resolve("skins").resolve(key);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                KonCompanionsFabric.MOD_ID, "dynamic_skin/" + key.replace('.', '_'));
        if (!Files.isRegularFile(file)) {
            return DEFAULT_KON;
        }
        try (InputStream in = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(in);
            DynamicTexture texture = new DynamicTexture(image);
            Minecraft.getInstance().getTextureManager().register(id, texture);
            return id;
        } catch (IOException e) {
            KonCompanionsFabric.LOGGER.error("Failed loading local companion skin {}", file, e);
            return DEFAULT_KON;
        }
    }
}
