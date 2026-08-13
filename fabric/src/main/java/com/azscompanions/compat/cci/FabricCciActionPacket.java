package com.azscompanions.compat.cci;

import com.azscompanions.AzsCompanionsFabric;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record FabricCciActionPacket(String actionName, String message) implements CustomPacketPayload {
    public static final Type<FabricCciActionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "cci_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FabricCciActionPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, FabricCciActionPacket::actionName,
                    ByteBufCodecs.STRING_UTF8, FabricCciActionPacket::message,
                    FabricCciActionPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FabricCciActionPacket packet, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            FabricCciCompanionAction action;
            try {
                action = FabricCciCompanionAction.valueOf(packet.actionName());
            } catch (IllegalArgumentException ex) {
                return;
            }
            FabricCciCompanionActions.applyOnServer(player, action, packet.message());
        });
    }
}
