package com.azscompanions.network;

import com.azscompanions.AzsCompanions;
import com.azscompanions.network.packet.CompanionDialoguePacket;
import com.azscompanions.network.packet.CompanionSettingsPacket;
import com.azscompanions.network.packet.OpenCompanionCreatorPacket;
import com.azscompanions.network.packet.RadialCommandPacket;
import com.azscompanions.network.packet.RecruitCompanionPacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(AzsCompanions.MOD_ID).versioned("1");
        registrar.playToServer(RecruitCompanionPacket.TYPE, RecruitCompanionPacket.STREAM_CODEC, RecruitCompanionPacket::handle);
        registrar.playToServer(RadialCommandPacket.TYPE, RadialCommandPacket.STREAM_CODEC, RadialCommandPacket::handle);
        registrar.playToServer(CompanionSettingsPacket.TYPE, CompanionSettingsPacket.STREAM_CODEC, CompanionSettingsPacket::handle);
        registrar.playToClient(CompanionDialoguePacket.TYPE, CompanionDialoguePacket.STREAM_CODEC, CompanionDialoguePacket::handle);
        registrar.playToClient(OpenCompanionCreatorPacket.TYPE, OpenCompanionCreatorPacket.STREAM_CODEC, OpenCompanionCreatorPacket::handle);
    }
}
