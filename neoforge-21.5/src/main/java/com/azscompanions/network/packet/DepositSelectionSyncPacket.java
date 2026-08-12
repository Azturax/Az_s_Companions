package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** S2C deposit chest selection mode + selected positions for client highlights. */
public record DepositSelectionSyncPacket(String payload) implements CustomPacketPayload {
    public static final Type<DepositSelectionSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "deposit_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DepositSelectionSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, DepositSelectionSyncPacket::payload,
                    DepositSelectionSyncPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
