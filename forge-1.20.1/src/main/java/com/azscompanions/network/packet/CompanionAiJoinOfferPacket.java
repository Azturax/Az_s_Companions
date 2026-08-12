package com.azscompanions.network.packet;

import net.minecraftforge.network.NetworkEvent;

import com.azscompanions.ai.AiJoinOffer;
import net.minecraft.network.FriendlyByteBuf;

/** S2C: AI availability / probe permission for the join-time consent prompt. */
public record CompanionAiJoinOfferPacket(
        boolean available,
        String source,
        String providerLabel,
        String endpointHint,
        String suggestProfile,
        boolean allowApply,
        boolean allowLocalProbe
) {

    public static void encode(CompanionAiJoinOfferPacket p, FriendlyByteBuf buf) {
        buf.writeBoolean(p.available);
        buf.writeUtf(p.source == null ? "" : p.source, 16);
        buf.writeUtf(p.providerLabel == null ? "" : p.providerLabel, 64);
        buf.writeUtf(p.endpointHint == null ? "" : p.endpointHint, 128);
        buf.writeUtf(p.suggestProfile == null ? "" : p.suggestProfile, 32);
        buf.writeBoolean(p.allowApply);
        buf.writeBoolean(p.allowLocalProbe);
    }

    public static CompanionAiJoinOfferPacket decode(FriendlyByteBuf buf) {
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
}
