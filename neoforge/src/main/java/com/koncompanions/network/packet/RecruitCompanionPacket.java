package com.koncompanions.network.packet;

import com.koncompanions.KonCompanions;
import com.koncompanions.entity.CompanionRecruitment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RecruitCompanionPacket(String definitionId) implements CustomPacketPayload {
    public static final Type<RecruitCompanionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(KonCompanions.MOD_ID, "recruit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RecruitCompanionPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, RecruitCompanionPacket::definitionId, RecruitCompanionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RecruitCompanionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CompanionRecruitment.recruit(player, packet.definitionId());
            }
        });
    }
}
