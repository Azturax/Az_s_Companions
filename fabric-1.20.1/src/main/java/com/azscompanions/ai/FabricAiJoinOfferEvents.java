package com.azscompanions.ai;

import com.azscompanions.admin.FabricAzAdminAccess;
import com.azscompanions.config.FabricServerConfig;
import com.azscompanions.network.FabricNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric: push AI join offer on login; apply consent when the player accepts.
 */
public final class FabricAiJoinOfferEvents {
    private FabricAiJoinOfferEvents() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            AiJoinOffer offer = AiJoinOffer.fromServerRuntime(
                    CompanionAiRuntime.get(), server.isDedicatedServer());
            FabricNetworking.sendAiJoinOffer(player, offer);
        });
    }

    public static void handleConsent(ServerPlayer player, boolean accepted, String suggestProfile, boolean applyProfile) {
        if (player == null || !accepted) {
            return;
        }
        player.displayClientMessage(Component.literal(AiJoinConsentApply.TIP), false);
        if (!FabricAzAdminAccess.mayUse(player)) {
            return;
        }
        boolean wantApply = applyProfile
                && suggestProfile != null
                && !suggestProfile.isBlank()
                && !CompanionAiRuntime.get().isEnabled();
        CompanionAiSettings merged = AiJoinConsentApply.apply(
                FabricServerConfig.aiSettings(), suggestProfile, wantApply);
        try {
            CompanionAiConfigIO.save(FabricServerConfig.aiConfigPath(), merged);
            FabricServerConfig.replaceAiSettings(merged);
            CompanionAiRuntime.get().applySettings(merged);
            if (wantApply) {
                player.displayClientMessage(Component.literal(
                        "Companion AI enabled with your local LLM (Use server LLM stays OFF — turn it ON in AI Config to share with others)."), false);
            } else if (merged.serverLlmOnly()) {
                player.displayClientMessage(Component.literal(
                        "Use server LLM is ON for this host (shared endpoint)."), false);
            }
        } catch (Exception e) {
            player.displayClientMessage(Component.literal(
                    "Could not save companion AI settings after consent."), false);
        }
    }
}
