package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.menu.RadialCommandMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RadialCommandPacket(int entityId, String commandName) implements CustomPacketPayload {
    public static final Type<RadialCommandPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "radial_command"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RadialCommandPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, RadialCommandPacket::entityId,
                    ByteBufCodecs.STRING_UTF8, RadialCommandPacket::commandName,
                    RadialCommandPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RadialCommandPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Entity entity = player.level().getEntity(packet.entityId());
            if (!(entity instanceof CompanionEntity companion)) {
                return;
            }
            RadialCommandMenu.Command command;
            try {
                command = RadialCommandMenu.Command.valueOf(packet.commandName());
            } catch (IllegalArgumentException ex) {
                return;
            }
            if (player.containerMenu instanceof RadialCommandMenu menu) {
                menu.runCommand(player, command);
            } else {
                // Allow command even if radial closed immediately.
                new RadialCommandMenu(0, player.getInventory(), companion).runCommand(player, command);
            }
        });
    }
}
