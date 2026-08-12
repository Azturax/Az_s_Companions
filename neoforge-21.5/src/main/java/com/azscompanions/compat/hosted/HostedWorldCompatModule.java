package com.azscompanions.compat.hosted;

import com.azscompanions.util.OwnableUuids;

import com.azscompanions.ai.CompanionAiRuntime;
import com.azscompanions.entity.CompanionEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.UUID;

/**
 * NeoForge soft-compat for Essential / e4mc / World Host / Open-to-LAN hosted worlds.
 * No compile dependency on those mods.
 */
public final class HostedWorldCompatModule {
    private static int tickCounter;

    private HostedWorldCompatModule() {
    }

    public static void bootstrap() {
        IntegratedMultiplayerCompat.installDetectedMods(HostedWorldMods.detectPresent(ModList.get()::isLoaded));
        NeoForge.EVENT_BUS.register(HostedWorldCompatModule.class);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if ((++tickCounter % 40) != 0) {
            return;
        }
        refresh(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        refresh(server);
        healOwnedCompanions(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server != null) {
            refresh(server);
        }
    }

    static void refresh(MinecraftServer server) {
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
        for (CompanionEntity companion : level.getEntitiesOfClass(CompanionEntity.class, box, CompanionEntity::isAlive)) {
            IntegratedMultiplayerCompat.tryHealOwnerUuid(
                    CompanionAiRuntime.get().settings(),
                    holder(companion),
                    playerId,
                    name);
            if (playerId.equals(OwnableUuids.get(companion))
                    && (companion.getOwnerName() == null || companion.getOwnerName().isBlank())) {
                companion.setOwnerName(name);
            }
        }
    }

    private static IntegratedMultiplayerCompat.UUIDHolder holder(CompanionEntity companion) {
        return new IntegratedMultiplayerCompat.UUIDHolder() {
            @Override
            public UUID getOwnerUuid() {
                return OwnableUuids.get(companion);
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
