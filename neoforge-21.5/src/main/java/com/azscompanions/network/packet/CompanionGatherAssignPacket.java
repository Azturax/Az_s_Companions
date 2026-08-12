package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.task.CollectMaterialAssign;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: assign collect_material gather from the companion Gather UI. */
public record CompanionGatherAssignPacket(int entityId, String itemId, int count, String depositMode)
        implements CustomPacketPayload {
    public static final Type<CompanionGatherAssignPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "gather_assign"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionGatherAssignPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CompanionGatherAssignPacket::entityId,
                    ByteBufCodecs.STRING_UTF8, CompanionGatherAssignPacket::itemId,
                    ByteBufCodecs.VAR_INT, CompanionGatherAssignPacket::count,
                    ByteBufCodecs.STRING_UTF8, CompanionGatherAssignPacket::depositMode,
                    CompanionGatherAssignPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CompanionGatherAssignPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
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
    }
}
