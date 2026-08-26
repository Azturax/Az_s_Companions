package com.azscompanions.event;

import com.azscompanions.config.FabricServerConfig;
import com.azscompanions.entity.FabricCompanionDeathPersistenceSupport;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.network.FabricNetworking;
import com.azscompanions.teamfight.TeamFightSession;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Fabric team-fight scoreboard sync on join + auto kill scoring between rival teams. */
public final class FabricTeamFightEvents {
    private FabricTeamFightEvents() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            TeamFightSession session = TeamFightSession.of(
                    player.getUUID(), FabricServerConfig.TEAMFIGHT_ENABLED_BY_DEFAULT);
            FabricNetworking.sendTeamFightHud(player, session.snapshot().encode());
        });

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof FabricCompanionEntity companion && companion.isFullyInvincible()) {
                companion.setHealth(companion.getMaxHealth());
                return false;
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof FabricCompanionEntity companion) {
                FabricCompanionDeathPersistenceSupport.persistOnDeath(companion);
            }
            if (!(entity instanceof FabricCompanionEntity victim)) {
                return;
            }
            Entity source = damageSource.getEntity();
            if (!(source instanceof FabricCompanionEntity killer)) {
                return;
            }
            if (victim.getOwnerUuid() == null || !victim.getOwnerUuid().equals(killer.getOwnerUuid())) {
                return;
            }
            if (victim.level().getServer() == null) {
                return;
            }
            ServerPlayer owner = victim.level().getServer().getPlayerList().getPlayer(victim.getOwnerUuid());
            if (owner == null) {
                return;
            }
            TeamFightSession session = TeamFightSession.of(owner.getUUID());
            if (!session.tryRecordTeamKill(
                    killer.getChatDisplayName(), killer.getTeamId(),
                    victim.getChatDisplayName(), victim.getTeamId())) {
                return;
            }
            FabricNetworking.sendTeamFightHud(owner, session.snapshot().encode());
        });
    }
}
