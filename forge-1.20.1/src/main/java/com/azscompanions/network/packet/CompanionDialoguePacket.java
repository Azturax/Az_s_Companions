package com.azscompanions.network.packet;

import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;

public record CompanionDialoguePacket(int entityId, String category, String line, String voiceProfile)
        {
    public static void encode(CompanionDialoguePacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId());
        buf.writeUtf(packet.category() == null ? "" : packet.category(), 8192);
        buf.writeUtf(packet.line() == null ? "" : packet.line(), 8192);
        buf.writeUtf(packet.voiceProfile() == null ? "" : packet.voiceProfile(), 8192);
    }

    public static CompanionDialoguePacket decode(FriendlyByteBuf buf) {
        return new CompanionDialoguePacket(buf.readVarInt(), buf.readUtf(8192), buf.readUtf(8192), buf.readUtf(8192));
    }

}
