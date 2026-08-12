package com.azscompanions.client;

import com.azscompanions.AzsCompanions;
import com.azscompanions.ai.AiJoinOffer;
import com.azscompanions.ai.ClientAiJoinConsent;
import com.azscompanions.ai.ClientAiJoinConsentStore;
import com.azscompanions.ai.ClientAiJoinOfferController;
import com.azscompanions.network.packet.CompanionAiJoinConsentPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;

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
        ClientAiJoinConsent.configureStore(
                FMLPaths.CONFIGDIR.get().resolve(ClientAiJoinConsentStore.FILE_NAME));
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
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
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
                Component.literal(AiJoinOffer.SOURCE_LOCAL.equals(offer.source())
                        ? "Yes — use this LLM"
                        : "Yes — use server LLM"),
                Component.literal("No — use my own / skip")));
    }

    private static void sendConsent(boolean accepted, AiJoinOffer offer) {
        if (!accepted || offer == null) {
            return;
        }
        com.azscompanions.network.ModNetworking.sendToServer(new CompanionAiJoinConsentPacket(
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
