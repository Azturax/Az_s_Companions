package com.azscompanions.network.packet;

import com.azscompanions.admin.AdminAiConfigSnapshot;
import com.azscompanions.admin.NeoAzAdminActions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** C2S: save AI config snapshot to disk and apply to server runtime. */
public record AdminAiSavePacket(String json) {

    public static void encode(AdminAiSavePacket p, FriendlyByteBuf buf) {
        buf.writeUtf(p.json == null ? "{}" : p.json, AdminAiConfigSnapshot.MAX_WIRE_JSON);
    }

    public static AdminAiSavePacket decode(FriendlyByteBuf buf) {
        return new AdminAiSavePacket(buf.readUtf(AdminAiConfigSnapshot.MAX_WIRE_JSON));
    }

    public static void handle(AdminAiSavePacket packet, java.util.function.Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                NeoAzAdminActions.saveAiConfig(player, AdminAiConfigSnapshot.fromWireJson(packet.json()));
            }
        });
        context.setPacketHandled(true);

    }
}
