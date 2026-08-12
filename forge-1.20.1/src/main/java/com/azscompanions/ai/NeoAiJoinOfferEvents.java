package com.azscompanions.ai;

import com.azscompanions.admin.NeoAzAdminAccess;
import com.azscompanions.config.AiConfig;
import com.azscompanions.network.packet.CompanionAiJoinOfferPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.PacketDistributor;

/** NeoForge: push AI join offer on login; apply consent when accepted. */
public final class NeoAiJoinOfferEvents {
    private NeoAiJoinOfferEvents() {
    }

    public static void bootstrap() {
        MinecraftForge.EVENT_BUS.register(NeoAiJoinOfferEvents.class);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var server = player.getServer();
        if (server == null) {
            return;
        }
        AiJoinOffer offer = AiJoinOffer.fromServerRuntime(
                CompanionAiRuntime.get(), server.isDedicatedServer());
        com.azscompanions.network.ModNetworking.sendToPlayer(player, CompanionAiJoinOfferPacket.fromOffer(offer));
    }

    public static void handleConsent(ServerPlayer player, boolean accepted, String suggestProfile, boolean applyProfile) {
        if (player == null || !accepted) {
            return;
        }
        player.displayClientMessage(Component.literal(AiJoinConsentApply.TIP), false);
        if (!NeoAzAdminAccess.mayUse(player)) {
            return;
        }
        boolean wantApply = applyProfile
                && suggestProfile != null
                && !suggestProfile.isBlank()
                && !CompanionAiRuntime.get().isEnabled();
        CompanionAiSettings merged = AiJoinConsentApply.apply(
                CompanionAiRuntime.get().settings(), suggestProfile, wantApply);
        try {
            AiConfig.saveSettingsToDiskWithoutReload(merged);
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
