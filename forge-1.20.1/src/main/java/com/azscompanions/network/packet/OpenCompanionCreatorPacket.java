package com.azscompanions.network.packet;

import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;

/** Server → client: open the Fallout-style companion character creator. */
public record OpenCompanionCreatorPacket(int entityId) {
    public static void encode(OpenCompanionCreatorPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId());
    }

    public static OpenCompanionCreatorPacket decode(FriendlyByteBuf buf) {
        return new OpenCompanionCreatorPacket(buf.readVarInt());
    }

}
