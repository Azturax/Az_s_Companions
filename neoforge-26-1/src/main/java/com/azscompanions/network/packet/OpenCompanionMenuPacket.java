package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server → client: open the shared Shift+RMB companion menu (Customize | Inventory). */
public record OpenCompanionMenuPacket(int entityId) implements CustomPacketPayload {
    public static final Type<OpenCompanionMenuPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AzsCompanions.MOD_ID, "open_menu"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCompanionMenuPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenCompanionMenuPacket::entityId, OpenCompanionMenuPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
