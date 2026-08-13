package com.azscompanions.compat.cci;

import com.azscompanions.AzsCompanions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CciActionPacket(String actionName, String message) implements CustomPacketPayload {
    public static final Type<CciActionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "cci_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CciActionPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, CciActionPacket::actionName,
                    ByteBufCodecs.STRING_UTF8, CciActionPacket::message,
                    CciActionPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CciActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            CciCompanionAction action;
            try {
                action = CciCompanionAction.valueOf(packet.actionName());
            } catch (IllegalArgumentException ex) {
                return;
            }
            CciCompanionActions.applyOnServer(player, action, packet.message());
        });
    }
}
