package com.azscompanions.network;

import com.azscompanions.AzsCompanions;
import com.azscompanions.client.network.ClientNetworkHandlers;
import com.azscompanions.network.packet.CompanionAiThinkingPacket;
import com.azscompanions.network.packet.CompanionAiJoinConsentPacket;
import com.azscompanions.network.packet.CompanionAiJoinOfferPacket;
import com.azscompanions.network.packet.CompanionBehaviorPacket;
import com.azscompanions.network.packet.CompanionCommandPacket;
import com.azscompanions.network.packet.CompanionDialoguePacket;
import com.azscompanions.network.packet.CompanionGatherAssignPacket;
import com.azscompanions.network.packet.CompanionContextSkinsPacket;
import com.azscompanions.network.packet.CompanionSettingsPacket;
import com.azscompanions.network.packet.AdminActionPacket;
import com.azscompanions.network.packet.AdminAiSavePacket;
import com.azscompanions.network.packet.CompanionPersonaPacket;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(AzsCompanions.MOD_ID).versioned("1");
        registrar.playToServer(RecruitCompanionPacket.TYPE, RecruitCompanionPacket.STREAM_CODEC, RecruitCompanionPacket::handle);
        registrar.playToServer(CompanionCommandPacket.TYPE, CompanionCommandPacket.STREAM_CODEC, CompanionCommandPacket::handle);
        registrar.playToServer(CompanionGatherAssignPacket.TYPE, CompanionGatherAssignPacket.STREAM_CODEC, CompanionGatherAssignPacket::handle);
        registrar.playToServer(CompanionSettingsPacket.TYPE, CompanionSettingsPacket.STREAM_CODEC, CompanionSettingsPacket::handle);
        registrar.playToServer(CompanionContextSkinsPacket.TYPE, CompanionContextSkinsPacket.STREAM_CODEC, CompanionContextSkinsPacket::handle);
        registrar.playToServer(CompanionBehaviorPacket.TYPE, CompanionBehaviorPacket.STREAM_CODEC, CompanionBehaviorPacket::handle);
        registrar.playToServer(CompanionPersonaPacket.TYPE, CompanionPersonaPacket.STREAM_CODEC, CompanionPersonaPacket::handle);
        registrar.playToServer(AdminAiSavePacket.TYPE, AdminAiSavePacket.STREAM_CODEC, AdminAiSavePacket::handle);
        registrar.playToServer(AdminActionPacket.TYPE, AdminActionPacket.STREAM_CODEC, AdminActionPacket::handle);
        registrar.playToServer(DepositExitModePacket.TYPE, DepositExitModePacket.STREAM_CODEC, DepositExitModePacket::handle);
        registrar.playToServer(CompanionAiJoinConsentPacket.TYPE, CompanionAiJoinConsentPacket.STREAM_CODEC, CompanionAiJoinConsentPacket::handle);
        registrar.playToServer(ToggleWigglyDogPacket.TYPE, ToggleWigglyDogPacket.STREAM_CODEC, ToggleWigglyDogPacket::handle);

        // Codecs must register on both sides. Client GUI/voice handlers must never be classloaded
        // on the dedicated server (OpenCompanionCreatorPacket used to import CompanionCreatorScreen).
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientNetworkHandlers.register(registrar);
        } else {
            registrar.playToClient(CompanionDialoguePacket.TYPE, CompanionDialoguePacket.STREAM_CODEC, (packet, context) -> {});
            registrar.playToClient(OpenCompanionCreatorPacket.TYPE, OpenCompanionCreatorPacket.STREAM_CODEC, (packet, context) -> {});
            registrar.playToClient(OpenCompanionMenuPacket.TYPE, OpenCompanionMenuPacket.STREAM_CODEC, (packet, context) -> {});
            registrar.playToClient(OpenCompanionPersonaPacket.TYPE, OpenCompanionPersonaPacket.STREAM_CODEC, (packet, context) -> {});
            registrar.playToClient(OpenCompanionStatsPacket.TYPE, OpenCompanionStatsPacket.STREAM_CODEC, (packet, context) -> {});
            registrar.playToClient(OpenAzAdminPacket.TYPE, OpenAzAdminPacket.STREAM_CODEC, (packet, context) -> {});
            registrar.playToClient(TeamFightHudPacket.TYPE, TeamFightHudPacket.STREAM_CODEC, (packet, context) -> {});
            registrar.playToClient(CompanionAiThinkingPacket.TYPE, CompanionAiThinkingPacket.STREAM_CODEC, (packet, context) -> {});
            registrar.playToClient(DepositSelectionSyncPacket.TYPE, DepositSelectionSyncPacket.STREAM_CODEC, (packet, context) -> {});
            registrar.playToClient(CompanionAiJoinOfferPacket.TYPE, CompanionAiJoinOfferPacket.STREAM_CODEC, (packet, context) -> {});
        }
    }
}
