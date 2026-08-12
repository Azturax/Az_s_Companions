package com.azscompanions.network.packet;

import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;

/** Server → client: open the shared Shift+RMB companion menu (Customize | Inventory). */
public record OpenCompanionMenuPacket(int entityId) {
    public static void encode(OpenCompanionMenuPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId());
    }

    public static OpenCompanionMenuPacket decode(FriendlyByteBuf buf) {
        return new OpenCompanionMenuPacket(buf.readVarInt());
    }

}
