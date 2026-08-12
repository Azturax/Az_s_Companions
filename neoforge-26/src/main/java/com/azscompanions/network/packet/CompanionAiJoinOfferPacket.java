package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.ai.AiJoinOffer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S2C: AI availability / probe permission for the join-time consent prompt. */
public record CompanionAiJoinOfferPacket(
        boolean available,
        String source,
        String providerLabel,
        String endpointHint,
        String suggestProfile,
        boolean allowApply,
        boolean allowLocalProbe
) implements CustomPacketPayload {
    public static final Type<CompanionAiJoinOfferPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AzsCompanions.MOD_ID, "ai_join_offer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionAiJoinOfferPacket> STREAM_CODEC =
            StreamCodec.of(CompanionAiJoinOfferPacket::write, CompanionAiJoinOfferPacket::read);

    private static void write(RegistryFriendlyByteBuf buf, CompanionAiJoinOfferPacket p) {
        buf.writeBoolean(p.available);
        buf.writeUtf(p.source == null ? "" : p.source, 16);
        buf.writeUtf(p.providerLabel == null ? "" : p.providerLabel, 64);
        buf.writeUtf(p.endpointHint == null ? "" : p.endpointHint, 128);
        buf.writeUtf(p.suggestProfile == null ? "" : p.suggestProfile, 32);
        buf.writeBoolean(p.allowApply);
        buf.writeBoolean(p.allowLocalProbe);
    }

    private static CompanionAiJoinOfferPacket read(RegistryFriendlyByteBuf buf) {
        return new CompanionAiJoinOfferPacket(
                buf.readBoolean(),
                buf.readUtf(16),
                buf.readUtf(64),
                buf.readUtf(128),
                buf.readUtf(32),
                buf.readBoolean(),
                buf.readBoolean());
    }

    public static CompanionAiJoinOfferPacket fromOffer(AiJoinOffer offer) {
        AiJoinOffer o = offer == null ? AiJoinOffer.none() : offer;
        return new CompanionAiJoinOfferPacket(
                o.available(), o.source(), o.providerLabel(), o.endpointHint(),
                o.suggestProfile(), o.allowApply(), o.allowLocalProbe());
    }

    public AiJoinOffer toOffer() {
        return new AiJoinOffer(available, source, providerLabel, endpointHint, suggestProfile, allowApply, allowLocalProbe);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
