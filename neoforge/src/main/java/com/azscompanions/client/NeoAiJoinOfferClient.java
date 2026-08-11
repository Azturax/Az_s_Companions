package com.azscompanions.client;

import com.azscompanions.ai.AiJoinOffer;
import com.azscompanions.ai.ClientAiJoinOfferController;
import com.azscompanions.network.packet.CompanionAiJoinConsentPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import com.azscompanions.AzsCompanions;

@EventBusSubscriber(modid = AzsCompanions.MOD_ID, value = Dist.CLIENT)
public final class NeoAiJoinOfferClient {
    private static boolean configured;

    private NeoAiJoinOfferClient() {
    }

    private static void ensureConfigured() {
        if (configured) {
            return;
        }
        configured = true;
        ClientAiJoinOfferController.configure(
                NeoAiJoinOfferClient::openConfirm,
                NeoAiJoinOfferClient::sendConsent,
                () -> Minecraft.getInstance().screen != null,
                runnable -> Minecraft.getInstance().execute(runnable));
    }

    public static void onOffer(AiJoinOffer offer) {
        ensureConfigured();
        ClientAiJoinOfferController.handleServerOffer(offer, currentServerKey());
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        ensureConfigured();
        ClientAiJoinOfferController.clientTick();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientAiJoinOfferController.onDisconnected();
    }

    private static void openConfirm(AiJoinOffer offer) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.setScreen(new ConfirmScreen(
                accepted -> {
                    mc.setScreen(null);
                    ClientAiJoinOfferController.onUserChoice(accepted);
                },
                Component.literal(offer.promptTitle()),
                Component.literal(offer.promptMessage()),
                Component.literal("Yes — use server LLM"),
                Component.literal("No")));
    }

    private static void sendConsent(boolean accepted, AiJoinOffer offer) {
        if (!accepted || offer == null) {
            return;
        }
        PacketDistributor.sendToServer(new CompanionAiJoinConsentPacket(
                true,
                offer.suggestProfile(),
                offer.allowApply() && AiJoinOffer.SOURCE_LOCAL.equals(offer.source())));
    }

    private static String currentServerKey() {
        Minecraft mc = Minecraft.getInstance();
        ServerData data = mc.getCurrentServer();
        if (data != null && data.ip != null && !data.ip.isBlank()) {
            return data.ip;
        }
        if (mc.hasSingleplayerServer()) {
            return "integrated";
        }
        return "unknown";
    }
}
