package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.admin.NeoAzAdminActions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: admin overview action. */
public record AdminActionPacket(String action) implements CustomPacketPayload {
    public static final Type<AdminActionPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AzsCompanions.MOD_ID, "admin_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdminActionPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, AdminActionPacket::action, AdminActionPacket::new);

    public static void handle(AdminActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                NeoAzAdminActions.handleAction(player, packet.action());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
