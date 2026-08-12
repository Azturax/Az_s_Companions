package com.azscompanions.network.packet;

import com.azscompanions.entity.CompanionRecruitment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record RecruitCompanionPacket(String definitionId) {
    public static void encode(RecruitCompanionPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.definitionId() == null ? "" : packet.definitionId(), 8192);
    }

    public static RecruitCompanionPacket decode(FriendlyByteBuf buf) {
        return new RecruitCompanionPacket(buf.readUtf(8192));
    }


    public static void handle(RecruitCompanionPacket packet, java.util.function.Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                var created = CompanionRecruitment.recruit(player, packet.definitionId());
                if (created != null) {
                    com.azscompanions.ai.CompanionPersonaOnboarding.offerIfNeeded(player, created);
                }
            }
        });
        context.setPacketHandled(true);

    }
}
