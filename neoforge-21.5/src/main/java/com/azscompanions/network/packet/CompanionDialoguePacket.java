package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CompanionDialoguePacket(int entityId, String category, String line, String voiceProfile)
        implements CustomPacketPayload {

    public static final Type<CompanionDialoguePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "dialogue"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionDialoguePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CompanionDialoguePacket::entityId,
                    ByteBufCodecs.STRING_UTF8, CompanionDialoguePacket::category,
                    ByteBufCodecs.STRING_UTF8, CompanionDialoguePacket::line,
                    ByteBufCodecs.STRING_UTF8, CompanionDialoguePacket::voiceProfile,
                    CompanionDialoguePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
