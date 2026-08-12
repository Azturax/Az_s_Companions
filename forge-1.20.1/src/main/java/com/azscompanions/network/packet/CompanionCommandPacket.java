package com.azscompanions.network.packet;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.menu.CompanionCommandActions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

public record CompanionCommandPacket(int entityId, String commandName) {
    public static void encode(CompanionCommandPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId());
        buf.writeUtf(packet.commandName() == null ? "" : packet.commandName(), 8192);
    }

    public static CompanionCommandPacket decode(FriendlyByteBuf buf) {
        return new CompanionCommandPacket(buf.readVarInt(), buf.readUtf(8192));
    }


    public static void handle(CompanionCommandPacket packet, java.util.function.Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
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
        context.setPacketHandled(true);

    }
}
