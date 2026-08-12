package com.azscompanions.network;

import com.azscompanions.AzsCompanions;
import com.azscompanions.client.network.ClientNetworkHandlers;
import com.azscompanions.network.packet.AdminActionPacket;
import com.azscompanions.network.packet.AdminAiSavePacket;
import com.azscompanions.network.packet.CompanionAiJoinConsentPacket;
import com.azscompanions.network.packet.CompanionAiJoinOfferPacket;
import com.azscompanions.network.packet.CompanionAiThinkingPacket;
import com.azscompanions.network.packet.CompanionBehaviorPacket;
import com.azscompanions.network.packet.CompanionCommandPacket;
import com.azscompanions.network.packet.CompanionContextSkinsPacket;
import com.azscompanions.network.packet.CompanionDialoguePacket;
import com.azscompanions.network.packet.CompanionGatherAssignPacket;
import com.azscompanions.network.packet.CompanionPersonaPacket;
import com.azscompanions.network.packet.CompanionSettingsPacket;
import com.azscompanions.network.packet.DepositExitModePacket;
import com.azscompanions.network.packet.DepositSelectionSyncPacket;
import com.azscompanions.network.packet.OpenAzAdminPacket;
import com.azscompanions.network.packet.OpenCompanionCreatorPacket;
import com.azscompanions.network.packet.OpenCompanionMenuPacket;
import com.azscompanions.network.packet.OpenCompanionPersonaPacket;
import com.azscompanions.network.packet.OpenCompanionStatsPacket;
import com.azscompanions.network.packet.RecruitCompanionPacket;
import com.azscompanions.network.packet.TeamFightHudPacket;
import com.azscompanions.network.packet.ToggleWigglyDogPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class ModNetworking {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AzsCompanions.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private ModNetworking() {
    }

    private static <MSG> BiConsumer<MSG, Supplier<NetworkEvent.Context>> noop() {
        return (msg, ctx) -> ctx.get().setPacketHandled(true);
    }

    public static void register() {
        int id = 0;
        // C2S
        CHANNEL.messageBuilder(RecruitCompanionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RecruitCompanionPacket::encode).decoder(RecruitCompanionPacket::decode)
                .consumerMainThread(RecruitCompanionPacket::handle).add();
        CHANNEL.messageBuilder(CompanionCommandPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CompanionCommandPacket::encode).decoder(CompanionCommandPacket::decode)
                .consumerMainThread(CompanionCommandPacket::handle).add();
        CHANNEL.messageBuilder(CompanionGatherAssignPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CompanionGatherAssignPacket::encode).decoder(CompanionGatherAssignPacket::decode)
                .consumerMainThread(CompanionGatherAssignPacket::handle).add();
        CHANNEL.messageBuilder(CompanionSettingsPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CompanionSettingsPacket::encode).decoder(CompanionSettingsPacket::decode)
                .consumerMainThread(CompanionSettingsPacket::handle).add();
        CHANNEL.messageBuilder(CompanionContextSkinsPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CompanionContextSkinsPacket::encode).decoder(CompanionContextSkinsPacket::decode)
                .consumerMainThread(CompanionContextSkinsPacket::handle).add();
        CHANNEL.messageBuilder(CompanionBehaviorPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CompanionBehaviorPacket::encode).decoder(CompanionBehaviorPacket::decode)
                .consumerMainThread(CompanionBehaviorPacket::handle).add();
        CHANNEL.messageBuilder(CompanionPersonaPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CompanionPersonaPacket::encode).decoder(CompanionPersonaPacket::decode)
                .consumerMainThread(CompanionPersonaPacket::handle).add();
        CHANNEL.messageBuilder(AdminAiSavePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AdminAiSavePacket::encode).decoder(AdminAiSavePacket::decode)
                .consumerMainThread(AdminAiSavePacket::handle).add();
        CHANNEL.messageBuilder(AdminActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AdminActionPacket::encode).decoder(AdminActionPacket::decode)
                .consumerMainThread(AdminActionPacket::handle).add();
        CHANNEL.messageBuilder(DepositExitModePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DepositExitModePacket::encode).decoder(DepositExitModePacket::decode)
                .consumerMainThread(DepositExitModePacket::handle).add();
        CHANNEL.messageBuilder(CompanionAiJoinConsentPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CompanionAiJoinConsentPacket::encode).decoder(CompanionAiJoinConsentPacket::decode)
                .consumerMainThread(CompanionAiJoinConsentPacket::handle).add();
        CHANNEL.messageBuilder(ToggleWigglyDogPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ToggleWigglyDogPacket::encode).decoder(ToggleWigglyDogPacket::decode)
                .consumerMainThread(ToggleWigglyDogPacket::handle).add();

        // S2C — same message ids on both sides; client handlers only referenced on CLIENT
        boolean client = FMLEnvironment.dist == Dist.CLIENT;
        CHANNEL.messageBuilder(CompanionDialoguePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CompanionDialoguePacket::encode).decoder(CompanionDialoguePacket::decode)
                .consumerMainThread(client
                        ? (msg, ctx) -> { ctx.get().enqueueWork(() -> ClientNetworkHandlers.handleDialogue(msg)); ctx.get().setPacketHandled(true); }
                        : noop())
                .add();
        CHANNEL.messageBuilder(OpenCompanionCreatorPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenCompanionCreatorPacket::encode).decoder(OpenCompanionCreatorPacket::decode)
                .consumerMainThread(client
                        ? (msg, ctx) -> { ctx.get().enqueueWork(() -> ClientNetworkHandlers.handleOpenCreator(msg)); ctx.get().setPacketHandled(true); }
                        : noop())
                .add();
        CHANNEL.messageBuilder(OpenCompanionMenuPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenCompanionMenuPacket::encode).decoder(OpenCompanionMenuPacket::decode)
                .consumerMainThread(client
                        ? (msg, ctx) -> { ctx.get().enqueueWork(() -> ClientNetworkHandlers.handleOpenMenu(msg)); ctx.get().setPacketHandled(true); }
                        : noop())
                .add();
        CHANNEL.messageBuilder(OpenCompanionPersonaPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenCompanionPersonaPacket::encode).decoder(OpenCompanionPersonaPacket::decode)
                .consumerMainThread(client
                        ? (msg, ctx) -> { ctx.get().enqueueWork(() -> ClientNetworkHandlers.handleOpenPersona(msg)); ctx.get().setPacketHandled(true); }
                        : noop())
                .add();
        CHANNEL.messageBuilder(OpenCompanionStatsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenCompanionStatsPacket::encode).decoder(OpenCompanionStatsPacket::decode)
                .consumerMainThread(client
                        ? (msg, ctx) -> { ctx.get().enqueueWork(() -> ClientNetworkHandlers.handleOpenStats(msg)); ctx.get().setPacketHandled(true); }
                        : noop())
                .add();
        CHANNEL.messageBuilder(OpenAzAdminPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenAzAdminPacket::encode).decoder(OpenAzAdminPacket::decode)
                .consumerMainThread(client
                        ? (msg, ctx) -> { ctx.get().enqueueWork(() -> ClientNetworkHandlers.handleOpenAdmin(msg)); ctx.get().setPacketHandled(true); }
                        : noop())
                .add();
        CHANNEL.messageBuilder(TeamFightHudPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TeamFightHudPacket::encode).decoder(TeamFightHudPacket::decode)
                .consumerMainThread(client
                        ? (msg, ctx) -> { ctx.get().enqueueWork(() -> ClientNetworkHandlers.handleTeamFightHud(msg)); ctx.get().setPacketHandled(true); }
                        : noop())
                .add();
        CHANNEL.messageBuilder(CompanionAiThinkingPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CompanionAiThinkingPacket::encode).decoder(CompanionAiThinkingPacket::decode)
                .consumerMainThread(client
                        ? (msg, ctx) -> { ctx.get().enqueueWork(() -> ClientNetworkHandlers.handleAiThinking(msg)); ctx.get().setPacketHandled(true); }
                        : noop())
                .add();
        CHANNEL.messageBuilder(DepositSelectionSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DepositSelectionSyncPacket::encode).decoder(DepositSelectionSyncPacket::decode)
                .consumerMainThread(client
                        ? (msg, ctx) -> { ctx.get().enqueueWork(() -> ClientNetworkHandlers.handleDepositSelection(msg)); ctx.get().setPacketHandled(true); }
                        : noop())
                .add();
        CHANNEL.messageBuilder(CompanionAiJoinOfferPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CompanionAiJoinOfferPacket::encode).decoder(CompanionAiJoinOfferPacket::decode)
                .consumerMainThread(client
                        ? (msg, ctx) -> { ctx.get().enqueueWork(() -> ClientNetworkHandlers.handleAiJoinOffer(msg)); ctx.get().setPacketHandled(true); }
                        : noop())
                .add();
        AzsCompanions.LOGGER.info("Registered {} network messages (Forge SimpleChannel)", id);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
