package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server → client: open the Fallout-style companion character creator. */
public record OpenCompanionCreatorPacket(int entityId) implements CustomPacketPayload {
    public static final Type<OpenCompanionCreatorPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AzsCompanions.MOD_ID, "open_creator"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCompanionCreatorPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenCompanionCreatorPacket::entityId, OpenCompanionCreatorPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
