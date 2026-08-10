package com.azscompanions.network;

import com.azscompanions.AzsCompanions;
import com.azscompanions.client.network.ClientNetworkHandlers;
import com.azscompanions.network.packet.CompanionCommandPacket;
import com.azscompanions.network.packet.CompanionDialoguePacket;
import com.azscompanions.network.packet.CompanionSettingsPacket;
import com.azscompanions.network.packet.OpenCompanionCreatorPacket;
import com.azscompanions.network.packet.OpenCompanionMenuPacket;
import com.azscompanions.network.packet.RecruitCompanionPacket;
import com.azscompanions.network.packet.TeamFightHudPacket;
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
        registrar.playToServer(CompanionSettingsPacket.TYPE, CompanionSettingsPacket.STREAM_CODEC, CompanionSettingsPacket::handle);

        // Codecs must register on both sides. Client GUI/voice handlers must never be classloaded
        // on the dedicated server (OpenCompanionCreatorPacket used to import CompanionCreatorScreen).
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientNetworkHandlers.register(registrar);
        } else {
            registrar.playToClient(CompanionDialoguePacket.TYPE, CompanionDialoguePacket.STREAM_CODEC, (packet, context) -> {});
            registrar.playToClient(OpenCompanionCreatorPacket.TYPE, OpenCompanionCreatorPacket.STREAM_CODEC, (packet, context) -> {});
            registrar.playToClient(OpenCompanionMenuPacket.TYPE, OpenCompanionMenuPacket.STREAM_CODEC, (packet, context) -> {});
            registrar.playToClient(TeamFightHudPacket.TYPE, TeamFightHudPacket.STREAM_CODEC, (packet, context) -> {});
        }
    }
}
