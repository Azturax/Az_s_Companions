package com.azscompanions.event;

import com.azscompanions.config.ServerConfig;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.network.packet.TeamFightHudPacket;
import com.azscompanions.teamfight.TeamFightSession;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Team-fight scoreboard sync on login + auto kill scoring between rival teams. */
public final class TeamFightGameEvents {
    private TeamFightGameEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        TeamFightSession session = TeamFightSession.of(
                player.getUUID(), ServerConfig.TEAMFIGHT_ENABLED_BY_DEFAULT.get());
        PacketDistributor.sendToPlayer(player, new TeamFightHudPacket(session.snapshot().encode()));
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof CompanionEntity victim)) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (!(source instanceof CompanionEntity killer)) {
            return;
        }
        if (victim.getOwnerUuid() == null || !victim.getOwnerUuid().equals(killer.getOwnerUuid())) {
            return;
        }
        ServerPlayer owner = victim.level().getServer() == null
                ? null
                : victim.level().getServer().getPlayerList().getPlayer(victim.getOwnerUuid());
        if (owner == null) {
            return;
        }
        TeamFightSession session = TeamFightSession.of(owner.getUUID());
        if (!session.tryRecordTeamKill(
                killer.getChatDisplayName(), killer.getTeamId(),
                victim.getChatDisplayName(), victim.getTeamId())) {
            return;
        }
        PacketDistributor.sendToPlayer(owner, new TeamFightHudPacket(session.snapshot().encode()));
    }
}
