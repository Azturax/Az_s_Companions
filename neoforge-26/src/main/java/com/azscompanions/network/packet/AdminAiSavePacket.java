package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.admin.AdminAiConfigSnapshot;
import com.azscompanions.admin.NeoAzAdminActions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: save AI config snapshot to disk and apply to server runtime. */
public record AdminAiSavePacket(String json) implements CustomPacketPayload {
    public static final Type<AdminAiSavePacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AzsCompanions.MOD_ID, "admin_ai_save"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdminAiSavePacket> STREAM_CODEC =
            StreamCodec.of(AdminAiSavePacket::write, AdminAiSavePacket::read);

    private static void write(RegistryFriendlyByteBuf buf, AdminAiSavePacket p) {
        buf.writeUtf(p.json == null ? "{}" : p.json, AdminAiConfigSnapshot.MAX_WIRE_JSON);
    }

    private static AdminAiSavePacket read(RegistryFriendlyByteBuf buf) {
        return new AdminAiSavePacket(buf.readUtf(AdminAiConfigSnapshot.MAX_WIRE_JSON));
    }

    public static void handle(AdminAiSavePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                NeoAzAdminActions.saveAiConfig(player, AdminAiConfigSnapshot.fromWireJson(packet.json()));
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
