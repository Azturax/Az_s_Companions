package com.azscompanions.network.packet;

import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;

/** S2C deposit chest selection mode + selected positions for client highlights. */
public record DepositSelectionSyncPacket(String payload) {
    public static void encode(DepositSelectionSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.payload() == null ? "" : packet.payload(), 8192);
    }

    public static DepositSelectionSyncPacket decode(FriendlyByteBuf buf) {
        return new DepositSelectionSyncPacket(buf.readUtf(8192));
    }

}
