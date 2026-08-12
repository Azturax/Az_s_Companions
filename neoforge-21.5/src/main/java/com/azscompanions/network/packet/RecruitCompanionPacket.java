package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.entity.CompanionRecruitment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RecruitCompanionPacket(String definitionId) implements CustomPacketPayload {
    public static final Type<RecruitCompanionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "recruit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RecruitCompanionPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, RecruitCompanionPacket::definitionId, RecruitCompanionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RecruitCompanionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                var created = CompanionRecruitment.recruit(player, packet.definitionId());
                if (created != null) {
                    com.azscompanions.ai.CompanionPersonaOnboarding.offerIfNeeded(player, created);
                }
            }
        });
    }
}
