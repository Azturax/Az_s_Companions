package com.azscompanions.compat.hosted;

import com.azscompanions.ai.CompanionAiRuntime;
import com.azscompanions.entity.CompanionEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;

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
        MinecraftForge.EVENT_BUS.register(HostedWorldCompatModule.class);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
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
            if (playerId.equals(companion.getOwnerUuid())
                    && (companion.getOwnerName() == null || companion.getOwnerName().isBlank())) {
                companion.setOwnerName(name);
            }
        }
    }

    private static IntegratedMultiplayerCompat.UUIDHolder holder(CompanionEntity companion) {
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
