package com.azscompanions.network.packet;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.task.CollectMaterialAssign;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

/** C2S: assign collect_material gather from the companion Gather UI. */
public record CompanionGatherAssignPacket(int entityId, String itemId, int count, String depositMode)
        {
    public static void encode(CompanionGatherAssignPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId());
        buf.writeUtf(packet.itemId() == null ? "" : packet.itemId(), 8192);
        buf.writeVarInt(packet.count());
        buf.writeUtf(packet.depositMode() == null ? "" : packet.depositMode(), 8192);
    }

    public static CompanionGatherAssignPacket decode(FriendlyByteBuf buf) {
        return new CompanionGatherAssignPacket(buf.readVarInt(), buf.readUtf(8192), buf.readVarInt(), buf.readUtf(8192));
    }


    public static void handle(CompanionGatherAssignPacket packet, java.util.function.Supplier<NetworkEvent.Context> ctx) {
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
            if (!companion.isOwnedBy(player) && !companion.isTrusted(player)) {
                return;
            }
            if (companion.distanceTo(player) > 64.0d) {
                return;
            }
            String mode = packet.depositMode() == null || packet.depositMode().isBlank()
                    ? "chest" : packet.depositMode();
            CollectMaterialAssign.assign(player, companion, packet.itemId(), packet.count(), mode);
        });
        context.setPacketHandled(true);

    }
}
