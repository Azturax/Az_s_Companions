package com.koncompanions.network;

import com.koncompanions.KonCompanions;
import com.koncompanions.network.packet.CompanionDialoguePacket;
import com.koncompanions.network.packet.CompanionSettingsPacket;
import com.koncompanions.network.packet.OpenCompanionCreatorPacket;
import com.koncompanions.network.packet.RadialCommandPacket;
import com.koncompanions.network.packet.RecruitCompanionPacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(KonCompanions.MOD_ID).versioned("1");
        registrar.playToServer(RecruitCompanionPacket.TYPE, RecruitCompanionPacket.STREAM_CODEC, RecruitCompanionPacket::handle);
        registrar.playToServer(RadialCommandPacket.TYPE, RadialCommandPacket.STREAM_CODEC, RadialCommandPacket::handle);
        registrar.playToServer(CompanionSettingsPacket.TYPE, CompanionSettingsPacket.STREAM_CODEC, CompanionSettingsPacket::handle);
        registrar.playToClient(CompanionDialoguePacket.TYPE, CompanionDialoguePacket.STREAM_CODEC, CompanionDialoguePacket::handle);
        registrar.playToClient(OpenCompanionCreatorPacket.TYPE, OpenCompanionCreatorPacket.STREAM_CODEC, OpenCompanionCreatorPacket::handle);
    }
}
