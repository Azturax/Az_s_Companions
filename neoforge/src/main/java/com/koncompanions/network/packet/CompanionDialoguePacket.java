package com.koncompanions.network.packet;

import com.koncompanions.KonCompanions;
import com.koncompanions.client.voice.ClientVoiceController;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CompanionDialoguePacket(int entityId, String category, String line, String voiceProfile)
        implements CustomPacketPayload {

    public static final Type<CompanionDialoguePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(KonCompanions.MOD_ID, "dialogue"));

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

    public static void handle(CompanionDialoguePacket packet, IPayloadContext context) {
        context.enqueueWork(() ->
                ClientVoiceController.handleDialogue(packet.entityId(), packet.category(), packet.line(), packet.voiceProfile()));
    }
}
