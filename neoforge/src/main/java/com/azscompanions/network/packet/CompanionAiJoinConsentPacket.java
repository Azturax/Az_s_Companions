package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.ai.NeoAiJoinOfferEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: player accepted/declined the join-time LLM consent prompt. */
public record CompanionAiJoinConsentPacket(
        boolean accepted,
        String suggestProfile,
        boolean applyProfile
) implements CustomPacketPayload {
    public static final Type<CompanionAiJoinConsentPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "ai_join_consent"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionAiJoinConsentPacket> STREAM_CODEC =
            StreamCodec.of(CompanionAiJoinConsentPacket::write, CompanionAiJoinConsentPacket::read);

    private static void write(RegistryFriendlyByteBuf buf, CompanionAiJoinConsentPacket p) {
        buf.writeBoolean(p.accepted);
        buf.writeUtf(p.suggestProfile == null ? "" : p.suggestProfile, 32);
        buf.writeBoolean(p.applyProfile);
    }

    private static CompanionAiJoinConsentPacket read(RegistryFriendlyByteBuf buf) {
        return new CompanionAiJoinConsentPacket(buf.readBoolean(), buf.readUtf(32), buf.readBoolean());
    }

    public static void handle(CompanionAiJoinConsentPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                NeoAiJoinOfferEvents.handleConsent(
                        player, packet.accepted(), packet.suggestProfile(), packet.applyProfile());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
