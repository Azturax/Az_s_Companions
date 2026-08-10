package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.menu.CompanionCommandActions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CompanionCommandPacket(int entityId, String commandName) implements CustomPacketPayload {
    public static final Type<CompanionCommandPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "companion_command"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionCommandPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CompanionCommandPacket::entityId,
                    ByteBufCodecs.STRING_UTF8, CompanionCommandPacket::commandName,
                    CompanionCommandPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CompanionCommandPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Entity entity = player.level().getEntity(packet.entityId());
            if (!(entity instanceof CompanionEntity companion)) {
                return;
            }
            CompanionCommandActions.Command command;
            try {
                command = CompanionCommandActions.Command.valueOf(packet.commandName());
            } catch (IllegalArgumentException ex) {
                return;
            }
            CompanionCommandActions.run(player, companion, command);
        });
    }
}
