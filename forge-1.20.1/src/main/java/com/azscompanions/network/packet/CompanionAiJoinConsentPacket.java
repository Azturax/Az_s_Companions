package com.azscompanions.network.packet;

import com.azscompanions.ai.NeoAiJoinOfferEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** C2S: player accepted/declined the join-time LLM consent prompt. */
public record CompanionAiJoinConsentPacket(
        boolean accepted,
        String suggestProfile,
        boolean applyProfile
) {

    public static void encode(CompanionAiJoinConsentPacket p, FriendlyByteBuf buf) {
        buf.writeBoolean(p.accepted);
        buf.writeUtf(p.suggestProfile == null ? "" : p.suggestProfile, 32);
        buf.writeBoolean(p.applyProfile);
    }

    public static CompanionAiJoinConsentPacket decode(FriendlyByteBuf buf) {
        return new CompanionAiJoinConsentPacket(buf.readBoolean(), buf.readUtf(32), buf.readBoolean());
    }

    public static void handle(CompanionAiJoinConsentPacket packet, java.util.function.Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                NeoAiJoinOfferEvents.handleConsent(
                        player, packet.accepted(), packet.suggestProfile(), packet.applyProfile());
            }
        });
        context.setPacketHandled(true);

    }
}
