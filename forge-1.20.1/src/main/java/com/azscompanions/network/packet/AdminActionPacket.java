package com.azscompanions.network.packet;

import com.azscompanions.admin.NeoAzAdminActions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** C2S: admin overview action. */
public record AdminActionPacket(String action) {
    public static void encode(AdminActionPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.action() == null ? "" : packet.action(), 8192);
    }

    public static AdminActionPacket decode(FriendlyByteBuf buf) {
        return new AdminActionPacket(buf.readUtf(8192));
    }


    public static void handle(AdminActionPacket packet, java.util.function.Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                NeoAzAdminActions.handleAction(player, packet.action());
            }
        });
        context.setPacketHandled(true);

    }
}
