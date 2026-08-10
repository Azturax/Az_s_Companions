package com.azscompanions.compat;

import com.azscompanions.ai.CompanionAiRuntime;
import com.azscompanions.compat.hosted.HostedWorldMods;
import com.azscompanions.compat.hosted.IntegratedMultiplayerCompat;
import com.azscompanions.entity.FabricCompanionEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fabric soft-compat for Essential / e4mc / World Host / Open-to-LAN hosted worlds.
 */
public final class FabricHostedWorldCompat {
    private static final AtomicInteger TICK = new AtomicInteger();

    private FabricHostedWorldCompat() {
    }

    public static void bootstrap() {
        IntegratedMultiplayerCompat.installDetectedMods(
                HostedWorldMods.detectPresent(FabricLoader.getInstance()::isModLoaded));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if ((TICK.incrementAndGet() % 40) != 0) {
                return;
            }
            refresh(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            refresh(server);
            healOwnedCompanions(handler.getPlayer());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> refresh(server));
    }

    private static void refresh(MinecraftServer server) {
        IntegratedMultiplayerCompat.refreshServerState(
                server.isDedicatedServer(),
                server.isPublished(),
                server.getPlayerList().getPlayerCount());
    }

    private static void healOwnedCompanions(ServerPlayer player) {
        if (!IntegratedMultiplayerCompat.ownerNameFallbackEnabled(CompanionAiRuntime.get().settings())) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        String name = player.getGameProfile().getName();
        UUID playerId = player.getUUID();
        var box = player.getBoundingBox().inflate(256.0d);
        for (FabricCompanionEntity companion : level.getEntitiesOfClass(
                FabricCompanionEntity.class, box, FabricCompanionEntity::isAlive)) {
            IntegratedMultiplayerCompat.tryHealOwnerUuid(
                    CompanionAiRuntime.get().settings(),
                    holder(companion),
                    playerId,
                    name);
            if (playerId.equals(companion.getOwnerUuid())
                    && (companion.getOwnerName() == null || companion.getOwnerName().isBlank())) {
                companion.setOwnerName(name);
            }
        }
    }

    private static IntegratedMultiplayerCompat.UUIDHolder holder(FabricCompanionEntity companion) {
        return new IntegratedMultiplayerCompat.UUIDHolder() {
            @Override
            public UUID getOwnerUuid() {
                return companion.getOwnerUuid();
            }

            @Override
            public void setOwnerUuid(UUID uuid) {
                companion.setOwnerUuid(uuid);
            }

            @Override
            public String getOwnerName() {
                return companion.getOwnerName();
            }

            @Override
            public void setOwnerName(String name) {
                companion.setOwnerName(name);
            }
        };
    }
}
