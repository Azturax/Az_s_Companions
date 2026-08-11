package com.azscompanions.client;

import com.azscompanions.ai.AiJoinOffer;
import com.azscompanions.ai.ClientAiJoinOfferController;
import com.azscompanions.network.FabricNetworkingClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class FabricAiJoinOfferClient {
    private FabricAiJoinOfferClient() {
    }

    public static void register() {
        ClientAiJoinOfferController.configure(
                FabricAiJoinOfferClient::openConfirm,
                FabricAiJoinOfferClient::sendConsent,
                () -> Minecraft.getInstance().screen != null,
                runnable -> Minecraft.getInstance().execute(runnable));
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientAiJoinOfferController.clientTick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                ClientAiJoinOfferController.onDisconnected());
    }

    public static void onOffer(AiJoinOffer offer) {
        ClientAiJoinOfferController.handleServerOffer(offer, currentServerKey());
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
                Component.literal(AiJoinOffer.SOURCE_LOCAL.equals(offer.source())
                        ? "Yes — use this LLM"
                        : "Yes — use server LLM"),
                Component.literal("No — use my own / skip")));
    }

    private static void sendConsent(boolean accepted, AiJoinOffer offer) {
        if (!accepted || offer == null) {
            return;
        }
        FabricNetworkingClient.sendAiJoinConsent(
                true,
                offer.suggestProfile(),
                offer.allowApply() && AiJoinOffer.SOURCE_LOCAL.equals(offer.source()));
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
